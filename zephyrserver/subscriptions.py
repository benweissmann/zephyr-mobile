#!/usr/bin/env python
# encoding: utf-8
from common import exported
import settings
import os

try:
    import zephyr
except ImportError:
    import test_zephyr as zephyr

__all__ = ("SubscriptionManager",)

TIMEOUT = 3

def parse_sub(s):
        s = s[:s.find("#")].strip()
        if s:
            return tuple(s.split(','))

def filterFile(fname, func):
    # Python 2.6 compatibility: can't combine with statements.
    with open(fname) as fi:
        with open(fname, "r+") as fo:
            fo.writelines(l for l in fi.xreadlines() if func(l))
            fo.truncate()

class SubscriptionManager(object):
    DEFAULT_SUBS = (
        ('%me%', '*', '*'),
        ('message', '*', '%me%')
    )
    def __init__(self, username, zsubfile=settings.ZSUBS):
        self.subscriptions = zephyr.Subscriptions()
        self.submap = {}

        self.username = username
        self.zsubfile = zsubfile

        self.load_or_create_zsubs()

    def load_or_create_zsubs(self):
        if not os.path.isfile(self.zsubfile):
            return self.set(self.DEFAULT_SUBS)

        with open(self.zsubfile) as f:
            for line in f.xreadlines():
                sub = parse_sub(line)
                if sub is not None:
                    self.add(sub, save=False)

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
        return list(self.subscriptions)

    @exported
    def set(self, subscriptions, save=True):
        """Set the subscriptions."""
        self.clear()
        self.subscriptions.update(subscriptions)
        if save:
            with open(self.zsubfile, 'w') as f:
                f.writelines(",".join(sub) + "\n" for sub in self.subscriptions)
        return True



    @exported
    def clear(self, save=True):
        """Clear the subscriptions."""
        self.subscriptions.clear()
        self.submap.clear()
        if save:
            open(self.zsubfile, 'w').close() # Truncate
        return True

    @exported
    def remove(self, subscription, save=True):
        """Remove the given subscription."""
        subscription = tuple(subscription)
        try:
            self.subscriptions.remove(subscription)
        except KeyError:
            return False
        self._rem_submap(subscription)
        if save:
            filterFile(self.zsubfile, lambda l: parse_sub(l) != subscription)
        return True

    @exported
    def add(self, subscription, save=True):
        """Add the given subscription."""
        subscription = tuple(subscription)
        if subscription in self.subscriptions:
            return False

        self.subscriptions.add(subscription)
        self._add_submap(subscription)
        if save:
            with open(self.zsubfile, 'a') as f:
                f.write(",".join(subscription) + "\n")

    @exported
    def removeAll(self, subscriptions, save=True):
        """Remove all given subscriptions."""
        subscriptions = self.intersection(tuple(s) for s in subscriptions)
        if not subscriptions:
            return
        self.subscriptions.difference_update(subscriptions)
        for sub in subscriptions:
                self._rem_submap(sub)
        if save:
            filterFile(self.zsubfile, lambda l: parse_sub(l) not in subscriptions)
        return len(subscriptions)

    @exported
    def addAll(self, subscriptions, save=True):
        """Add all given subscriptions."""
        subscriptions = set(tuple(s) for s in subscriptions) - self.subscriptions
        if not subscriptions:
            return 0
        self.subscriptions.update(subscriptions)
        for sub in subscriptions:
            self._add_submap(sub)
        if save:
            with open(self.zsubfile, 'a') as f:
                f.writelines(",".join(sub) + "\n" for sub in subscriptions)
        return len(subscriptions)

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
