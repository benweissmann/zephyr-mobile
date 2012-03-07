#!/usr/bin/env python
# encoding: utf-8
from . import return_status, exported
import time

class Message(object):
    def __init__(self, sender, message, cls, instance, user):
        self.sender = sender
        self.message = message
        self.timestamp = time.time()
        self.read = False
        self.cls = cls or "message"
        self.instance = instance or "personal"
        self.user = user

    def mark_read(self):
        self.read = True
    
    def is_read(self):
        return self.read

class Messenger(object):
    def __init__(self, submanager, username):
        self.submanager = submanager # XXX: Needed for now (not later).
        self.filters = {}
        self.messages = []
        self.message_attributes = {
            "class": {},
            "instance": {},
            "user": {},
            "sender": {}
        }
        self.username = username

    def _store_message(self, msg):
        self.messages.append(msg)
        self.message_attributes["class"].setdefault(msg.cls, set()).add(msg)
        self.message_attributes["instance"].setdefault(msg.instance, set()).add(msg)
        self.message_attributes["sender"].setdefault(msg.sender, set()).add(msg)
        self.message_attributes["user"].setdefault(msg.user, set()).add(msg)

    @exported
    @return_status
    def send(self, message, cls=None, instance=None, user=None):
        if self.submanager.matchTripplet(cls, instance, user):
            self._store_message(Message(self.username, message, cls, instance, user))

    @exported
    def filterMessages(self, fid, messageFilter):
        # XXX: Stub
        return fid

    @exported
    def get(self, fid=0, start=0, end=None):
        return self.messages[start:end]

    @exported
    def hasNew(self, last, fid=0):
        return len(self.messages) > (last+1)

    @exported
    @return_status
    def markFilter(self, status, fid=0, start=0, end=None):
        # XXX: Not Done
        self.mark(self, status, range(start, end if end is not None else len(self.messages) - 1))

    def delete(self, ids):
        # XXX: Not implimented
        return False

    def deleteFilter(self, fid=0, start=0, end=None):
        # XXX: Not implimented
        return False

    @exported
    @return_status
    def mark(self, status, ids):
        if status == "read":
            for m in self.messages:
                m.mark_read()
        elif status == "unread":
            for m in self.messages:
                m.mark_read()
        else:
            raise ValueError("Invalid status")

    @exported
    def getIDs(self, fid):
        # XXX: Stub
        return []

    @exported
    def getClasses(self, unread=True):
        # XXX: Stub
        return []

    @exported
    def getInstances(self, cls, unread=True):
        # XXX: Stub
        return []

    @exported
    def getCount(self, fid):
        # XXX: Stub
        return (0, 0)


