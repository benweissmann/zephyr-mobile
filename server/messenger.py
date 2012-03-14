#!/usr/bin/env python
# encoding: utf-8
import logging
logging.basicConfig(level=logging.DEBUG)
from . import return_status, exported
import time
import settings
from PyDbLite import Base
import bisect
import os
from itertools import islice


def make_message(sender, message, cls=None, instance=None, user=None):
    return {
        "sender": sender,
        "message": message,
        "timestamp": time.time(),
        "read": False,
        "cls": cls or "message",
        "instance": instance or "personal",
        "user": user
    }

def open_or_create_db():
    """
    Either open an existing db or create a new one.

    Arguments:
        path -  the path to the database. the database does not need to exist
                but the parent directory must.
    """
    db = Base(settings.ZEPHYR_DB)
    try:
        db.open()
    except IOError:
        if not os.path.isdir(settings.DATA_DIR):
            os.makedirs(settings.DATA_DIR)
        db.create("sender", "message", "timestamp", "read", "cls", "instance", "user")
        db.create_index("sender")
        db.create_index("cls")
        db.create_index("read")
        db.create_index("instance")
        db.create_index("user")
        db.open()
    return db

class Filter(object):
    """
    A filter that is an or of its clauses anded with its parent (recursivly).
    """

    def __init__(self, f, parent=None):
        """Initialize a new Filter
        Arguments:
            f - A list of ORs
            parent - the parent to and this filter with
        Filter clause:
            {
                field: field-name,
                regex: bool,
                value: search-value,
            }
        """
        self.f = frozenset(tuple(fi) for fi in f)
        self.fid = hash((parent.fid if parent is not None else hash(None), self.f)) # Creates a unique ordering but I am lazy
        self.parent = parent

    def __hash__(self):
        return self.fid
    
    def getMatchedIDs(self, messages, start=0, end=float("inf")):
        if self.parent:
            ids = self.parent.getMatchedIDs(messages, start, end)
            if len(ids) == 0:
                return ids
            new_ids = []
            for field, regex, value in self.f:
                if regex:
                    raise NotImplementedError()
                if value not in messages.indices[field]:
                    continue
            new_ids.extend(m for m in messages.indices[field][value] if m in ids)
            return new_ids
        else:
            new_ids = []
            for field, regex, value in self.f:
                if regex:
                    raise NotImplementedError()
                if value not in messages.indices[field]:
                    continue
                cur_ids = messages.indices[field][value]

                new_ids.extend(islice(
                    cur_ids,
                    bisect.bisect_left(cur_ids, start) if start else 0,
                    bisect.bisect_left(cur_ids, end) if end is not float("inf") else None
                ))

            return new_ids

class Messenger(object):
    def __init__(self, submanager, username):
        self.submanager = submanager # XXX: Needed for now (not later).
        self.messages = open_or_create_db()
        self.username = username
        self.filters = {}

    @exported
    @return_status
    def send(self, message, cls=None, instance=None, user=None):
        """
        Send a zephyr.

        Arguments:
            message - The message content
            cls - the message class (defaults to "message")
            instance - the message instance (defaults to "personal")
            user - the destination user (None means everyone)

        # Send a message to -c help -i linux
        >>> messenger.send("This is a really short message.", "help", "linux")

        # Send a message to bsw (the two following are equivalent)
        >>> messenger.send("This is a really short message.", None, None, "bsw")
        >>> messenger.send("This is a really short message.", "message", "personal", "bsw")
        """

        if self.submanager.matchTripplet(cls, instance, user):
            self.messages.insert(**make_message(self.username, message, cls, instance, user))
            self.messages.commit()

    @exported
    def filterMessages(self, messageFilters, parent_fid=None):
        """
        Filters messages.
        Arguments:
            messageFilters - a list of filters
            parent_fid - the parent filters filter id

        Returns:
            fid - a string filterID

        filter:
            A list of ORd queries in the form of.
            Query: (String(field), Bool(value_is_regex), value)
            Filter: [QueryA, QueryB, QueryC] == QueryA v QueryB v QueryC

        # Matches all unread messages in either the message or help class.
        >>> fid = messenger.filterMessages([[("cls", False, "message"), ("cls", False, "help")], [("read", False, False)]])
        # Get the messages that match the filter
        >>> messenger.get(fid)

        # Equivalent filter
        >>> fidA = messenger.filterMessages([[("cls", False, "message"), ("cls", False, "help")]])
        >>> fidB = messenger.filterMessages([[("read", False, False)]], fidA)
        >>> messenger.get(fid)
        """
        f = self.filters[int(parent_fid)] if parent_fid is not None else None
        for messageFilter in messageFilters:
            f = Filter(messageFilter, f)
            # Store intermediate filters.
            if f.fid in self.filters:
                f = self.filters[f.fid]
            else:
                self.filters[f.fid] = f

        return str(f.fid)

    @exported
    def get(self, fid=None, start=0, end=float("inf")):
        """
        Get the messages that match fid.
        Arguments:
            fid - the filter's ID
            start - the id of the first message (inclusive)
            end - the id of the last message (*EXCLUSIVE*)
        
        returns a time sorted list of message dictionaries
        Message format:

        >>> {
        >>>     "__id__": int,      # this is the id of the message
        >>>     "__version__": int, # this is updated every time the message is changed (marked etc.)
        >>>     "cls": str,
        >>>     "instance": str,
        >>>     "message": str,
        >>>     "user": str,        # the destination user
        >>>     "sender": str       # the sender
        >>> }

        """
        return sorted((self.messages[i] for i in self.applyFilter(fid, start, end)), key=lambda x: x["timestamp"])

    @exported
    def hasNew(self, last, fid=None):
        """ Returns true if there is a new message that matches the given filter. """
        # XXX: This should be faster.
        if fid is None:
            return self.messages.next_id - 1 > last
        else:
            for i in self.applyFilter(fid=fid, start=last):
                if i > last:
                    return True
            return False

    @exported
    def markFilter(self, status, fid=None, start=0, end=float("inf")):
        """ Mark all of the messages that match a filter with the given status. """
        return self.mark(status, self.applyFilter(fid, start, end))

    @exported
    def delete(self, ids):
        """ Delete the messages with th given ids. """
        self.messages.delete(self.messages[i] for i in ids)
        self.messages.commit()

    def applyFilter(self, fid=None, start=0, end=float("inf")):
        if fid is None:
            return (i for i in self.messages.records.iterkeys() if start <= i < end)
        else:
            return self.filters[int(fid)].getMatchedIDs(self.messages, start, end)


    @exported
    def deleteFilter(self, fid=None, start=0, end=float("inf")):
        """ Delete the messages that match the given filter. """
        return self.delete(self.applyFilter(fid, start, end))

    @exported
    #@return_status
    def mark(self, status, ids):
        """ Mark the given messages as read. """
        #raise NotImplementedError("This doesn't work for an unknown reason")
        messages = (self.messages[i] for i in ids)
        if status == "read":
            self.messages.update(messages, read=True)
        elif status == "unread":
            self.messages.update(messages, read=False)
        else:
            raise ValueError("Invalid status")
        self.messages.commit()

    @exported
    def getIDs(self, fid=None, start=0, end=float("inf")):
        """ Get all of the message ids that match the given filter. """
        return sorted(self.applyFilter(fid, start, end))
        

    @exported
    def getClasses(self, unread=True):
        """ List the classes with messages. """
        return self.messages._cls.keys()

    @exported
    def getInstances(self, cls):
        """ List the instances with messages in a given class.

        returns:
            [("instance", [unread_count, read_count]), ...]
        """
        insts = {}
        for m in self.messages(cls=cls):
            insts.setdefault(m["instance"], [])[int(m["read"])] += 1

        return insts.items()

    def getUnreadInstances(self, cls):
        """ List the instances with unread messages in a given class.

        returns:
            [("instance", message_count), ...]
        """
        insts = {}
        for m in self.messages(cls=cls, read=False):
            i = m["instance"]
            if i not in insts:
                insts[i] = 1
            else:
                insts[i] += 1
        return insts.items()

    @exported
    def getCount(self, fid=None):
        """ Get the number of messages that match a filter. """
        if fid is None:
            return len(self.messages)
        else:
            return len(list(self.applyfilter(fid)))


