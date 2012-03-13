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
        "class": cls or "message",
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
        db.create("sender", "message", "timestamp", "read", "class", "instance", "user")
        db.create_index("sender")
        db.create_index("class")
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
        if self.submanager.matchTripplet(cls, instance, user):
            self.messages.insert(**make_message(self.username, message, cls, instance, user))
            self.messages.commit()

    @exported
    def filterMessages(self, messageFilters, parent_fid=None):
        """
        Takes a list of chained filters.
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
        return sorted((self.messages[i] for i in self.applyFilter(fid, start, end)), key=lambda x: x["timestamp"])

    @exported
    def hasNew(self, last, fid=None):
        self.applyFilter(fid=fid, start=last)
        return (last+1) < self.messages.next_id

    @exported
    def markFilter(self, status, fid=None, start=0, end=float("inf")):
        return self.mark(status, self.applyFilter(fid, start, end))

    @exported
    def delete(self, ids):
        self.messages.delete(self.messages[i] for i in ids)
        self.messages.commit()

    def applyFilter(self, fid=None, start=0, end=float("inf")):
        if fid is None:
            return (i for i in self.messages.records.iterkeys() if start <= i < end)
        else:
            return self.filters[int(fid)].getMatchedIDs(self.messages, start, end)


    @exported
    def deleteFilter(self, fid=None, start=0, end=float("inf")):
        return self.delete(self.applyFilter(fid, start, end))

    @exported
    #@return_status
    def mark(self, status, ids):
        raise NotImplementedError("This doesn't work for an unknown reason")
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
        return sorted(self.applyFilter(fid, start, end))
        

    @exported
    def getClasses(self, unread=True):
        return self.messages._class.keys()

    @exported
    def getInstances(self, cls, unread=True):
        return tuple(set(m["instance"] for m in self.messages._class[cls]))

    @exported
    def getCount(self, fid=None):
        if fid is None:
            return len(self.messages)
        else:
            return len(list(self.applyfilter(fid)))


