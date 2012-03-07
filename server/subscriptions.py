#!/usr/bin/env python
# encoding: utf-8
from . import return_status, exported

def parse_zsubs():
    return set((("sipb", "*", "*"), ("help", "linux", "*"), ("message", "*", "ME")))

class SubscriptionManager(object):
    def __init__(self, username):
        self.subscriptions = parse_zsubs()
        self.submap = {}
        self.username = username
        for sub in self.subscriptions:
            self._add_submap(sub)

    def _add_submap(self, sub):
        """Add a subscription from the subscription matching map."""
        self.submap.setdefault(sub[0], {}).setdefault(sub[1], set()).add(sub[2])

    def _rem_submap(self, sub):
        """Remove a subscription from the subscription matching map."""
        cls, inst, user = sub

        if len(self.submap[cls]) == 1:
            del self.submap[cls]
        elif len(self.submap[cls][inst]) == 1:
            del self.submap[cls][inst]
        else:
            del self.submap[cls][inst][user]


    @exported
    def get(self):
        """Get a list of subscriptions."""
        try:
            return list(self.subscriptions)
        except Exception as e:
            return e

    @exported
    @return_status
    def set(self, subscriptions):
        """Set the subscriptions."""
        self.subscriptions = set(tuple(s) for s in subscriptions)
        for sub in self.subscriptions:
            self._add_submap(sub)

    @exported
    @return_status
    def clear(self):
        """Clear the subscriptions."""
        self.subscriptions.clear()
        self.submap.clear()

    @exported
    @return_status
    def remove(self, subscription):
        """Remove the given subscription."""
        self.subscriptions.remove(tuple(subscription))
        self._rem_submap(subscription)

    @exported
    @return_status
    def add(self, subscription):
        """Add the given subscription."""
        self.subscriptions.add(tuple(subscription))
        self._add_submap(subscription)

    @exported
    @return_status
    def removeAll(self, subscriptions):
        """Remove all given subscriptions."""
        self.subscriptions.difference_update(set(tuple(s) for s in subscriptions))
        for sub in self.subscriptions:
            self._rem_submap(sub)
        

    @exported
    @return_status
    def addAll(self, subscriptions):
        """Add all given subscriptions."""
        self.subscriptions.update(set(tuple(s) for s in subscriptions))
        for sub in self.subscriptions:
            self._add_submap(sub)

    @exported
    def matchTripplet(self, cls=None, instance=None, user=None):
        """Determine if the given triplet would be matched by a subscription."""
        cls = cls or "personal"
        instance = instance or "message"
        user = user or "*"


        if cls in self.submap:
            mp = self.submap[cls]
        else:
            return False

        if instance in mp:
            mp = mp[instance]
        elif "*" in mp:
            mp = mp["*"]
        else:
            return False
        
        return bool("*" in mp or user in mp)

    @exported
    def isSubscribed(self, subscription):
        """Returns true if the user is subscribed to the given triplet."""
        return tuple(subscription) in self.subscriptions

