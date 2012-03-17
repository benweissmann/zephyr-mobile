#!/usr/bin/env python
# encoding: utf-8
import logging
logging.basicConfig(level=logging.DEBUG)
from . import return_status, exported
import time
import settings
from PyDbLite import Base
import os


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

    def __init__(self, messageFilter, messages):
        """Initialize a new Filter
        Arguments:
            f - a filter {"field": "value", ... }
        Filter clause:
            {
                field: field-name,
                regex: bool,
                value: search-value,
            }
        """
        self.messageFilter = messageFilter
        self.fid = id(self)
        self.messages = messages
        self.timestamp = time.time()

    def __hash__(self):
        return self.fid

    def get(self, offset=0, perpage=None):
        if perpage is None:
            return self.messages[offset:]
        else:
            return self.messages[offset:offset+perpage]

    def filterResponse(self, offset=0, perpage=None):
        return {
            "filter": str(self.fid),
            "messages": self.get(offset, perpage),
            "count": len(self),
            "perpage": perpage,
            "offset": offset,
            "timestamp": self.timestamp,
        }

    def __len__(self):
        return len(self.messages)

class Messenger(object):
    def __init__(self, submanager, username):
        self.submanager = submanager # XXX: Needed for now (not later).
        self.messages = open_or_create_db()
        self.username = username
        self.filters = {}

    def _store_message(self, message):
        """ Stores a message and returns its ID. """
        ret = self.messages.insert(**message)
        self.messages.commit()
        return ret

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
            self._store_message(make_message(self.username, message, cls, instance, user))

    @exported
    def filterMessages(self, messageFilter):
        """
        Filters messages.
        Arguments:
            messageFilter - a filter in the form of {"field": "value"}
            parent_fid - the parent filters filter id

        Returns:
            (fid, count) where fid is a string filterID and count is the number
            of matched messages.

        # Matches all unread messages in class help.
        >>> fid = messenger.filterMessages({"cls": "help", "read": False})
        # Get the messages that match the filter
        >>> messenger.get(fid)
        """
        f = Filter(messageFilter, sorted(self.messages(**messageFilter), key=lambda x: x["timestamp"]))
        self.filters[f.fid] = f
        return (str(f.fid), len(f))

    @exported
    def get(self, fid, offset=0, perpage=None):
        """
        Get the messages that match fid.
        Arguments:
            fid     - the filter's ID
            offset  - the first item to return from the matched messages.
                      A negitive offset will index backwards.
            perpage - The maximum number of of results to return.
        
        # Message Format
        >>> message = {
        >>>     "__id__": int,      # this is the id of the message
        >>>     "__version__": int, # this is updated every time the message is changed (marked etc.)
        >>>     "cls": str,
        >>>     "instance": str,
        >>>     "message": str,
        >>>     "user": str,        # the destination user
        >>>     "sender": str       # the sender
        >>> }

        # Returns:
        >>> return_value = {
        >>>     "filter": int,      # The filter used to generate the result.
        >>>     "offset": int,      # The offset of the first result returned.
        >>>     "perpage": int,     # The number of results requested.
        >>>     "count": int,       # The total number of results found.
        >>>     "messages": list    # A list of message dictionaries.
        >>> }


        """
        return self.filters[int(fid)].filterResponse(offset, perpage)


    @exported
    def hasNew(self, last, messageFilter=None):
        """
        Returns True if there is a message that matches messageFilter newer
        than last.
        """
        if messageFilter is None:
            return self.messages.next_id - 1 > last
        else:
            for i in self.messages(**messageFilter):
                if i > last:
                    return True
            return False

    @exported
    def delete(self, ids):
        """
        Deletes the given messages.
        Returns the number deleted.
        """
        return self._deleteMessages(self.messages[i] for i in ids)

    @exported
    def deleteFilter(self, fid=None, offset=0, perpage=None):
        """
        Delete the messages that match the given filter.
        Returns the number deleted.
        """
        return self._deleteMessages(self.filters[int(fid)].get(offset, perpage))

    def _deleteMessages(self, messages):
        count = self.messages.delete(messages)
        self.messages.commit()
        return count

    @exported
    @return_status
    def markFilter(self, status, fid=None, offset=0, perpage=None):
        """ Mark all of the messages that match a filter with the given status. """
        self._markMessages(status, self.filters[int(fid)].get(offset, perpage))


    @exported
    @return_status
    def mark(self, status, ids):
        """ Mark the given messages as read. """
        self._markMessages(self.messages[i] for i in ids)

    def _markMessages(self, status, messages):
        if status == "read":
            self.messages.update(messages, read=True)
        elif status == "unread":
            self.messages.update(messages, read=False)
        else:
            raise ValueError("Invalid status")
        self.messages.commit()

    @exported
    def getIDs(self, fid=None, offset=0, perpage=None):
        """ Get all of the message ids that match the given filter. """
        return [m["__id__"] for m in self.filters[int(fid)].get(offset, perpage)]

    @exported
    def getClasses(self):
        """ List the classes with messages. """
        return [(cls, [len(self.messages(cls=cls, read=False)), len(self.messages(cls=cls, read=False))]) for cls in self.messages["cls"]]

    @exported
    def getUnreadClasses(self):
        """ List the classes with messages. """
        #XXX: TODO
        raise NotImplementedError("TODO")

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

    @exported
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
            return len(self.filters[int(fid)])

