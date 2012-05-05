#!/usr/bin/env python
# encoding: utf-8
from server import exported
import settings
import os
from time import sleep
from . import zephyr

__all__ = ("SubscriptionManager",)

TIMEOUT = 3
# TODO: When we fix the nuking zsubs issue, set save=True
SAVE=False

def parse_sub(s):
    if not s or s[0] == "!": return None # FIXME: Handle unsubs.
    tripplet = s[:s.find("#")].split(",")
    return tuple(tripplet) if len(tripplet) == 3 else None

class SubscriptionManager(object):
    DEFAULT_SUBS = (
        ('%me%', '*', '*'),
        ('message', '*', '%me%')
    )
    def __init__(self, username, zsubfile=settings.ZSUBS):
        try:
            self.subscriptions = zephyr.Subscriptions()
        except IOError:
            sleep(1)
            self.subscriptions = zephyr.Subscriptions()

        self.username = username
        self.zsubfile = zsubfile

        self.load_or_create_zsubs()

    def save(self):
        with open(self.zsubfile, "w", 0600) as f:
            f.writelines(",".join(sub) + "\n" for sub in self.subscriptions)

    def load_or_create_zsubs(self):
        if not os.path.isfile(self.zsubfile):
            return self.set(self.DEFAULT_SUBS)

        with open(self.zsubfile) as f:
            for line in f.xreadlines():
                sub = parse_sub(line)
                if sub is not None:
                    self.add(sub, save=False)

    @exported
    def get(self):
        """Get a list of subscriptions."""
        return list(self.subscriptions)

    @exported
    def set(self, subscriptions, save=SAVE):
        """Set the subscriptions."""
        self.clear()

        # Give it one retry
        try:
            self.subscriptions.update(subscriptions)
        except IOError:
            sleep(1)
            self.subscriptions.update(subscriptions)

        if save:
            self.save()
        return True



    @exported
    def clear(self, save=SAVE):
        """Clear the subscriptions."""
        # Give it one retry
        try:
            self.subscriptions.clear()
        except IOError:
            sleep(1)
            self.subscriptions.clear()

        if save:
            open(self.zsubfile, 'w').close() # Truncate
        return True

    @exported
    def remove(self, subscription, save=SAVE):
        """Remove the given subscription."""
        subscription = tuple(subscription)

        # Give it one retry
        try:
            self.subscriptions.remove(subscription)
        except KeyError:
            return False
        except IOError:
            sleep(1)
            try:
                self.subscriptions.remove(subscription)
            except KeyError:
                return False

        if save:
            self.save()
        return True

    @exported
    def add(self, subscription, save=SAVE):
        """Add the given subscription."""
        subscription = tuple(subscription)
        if subscription in self.subscriptions:
            return False

        # Give it one retry
        try:
            self.subscriptions.add(subscription)
        except IOError:
            sleep(1)
            self.subscriptions.add(subscription)

        if save:
            with open(self.zsubfile, "a", 0600) as f:
                f.write(",".join(subscription) + "\n")

    @exported
    def removeAll(self, subscriptions, save=SAVE):
        """Remove all given subscriptions."""
        subscriptions = self.intersection(tuple(s) for s in subscriptions)
        if not subscriptions:
            return

        # Give it one retry
        try:
            self.subscriptions.difference_update(subscriptions)
        except IOError:
            sleep(1)
            self.subscriptions.difference_update(subscriptions)

        if save:
            self.save()
        return len(subscriptions)

    @exported
    def addAll(self, subscriptions, save=SAVE):
        """Add all given subscriptions."""
        subscriptions = set(tuple(s) for s in subscriptions) - self.subscriptions
        if not subscriptions:
            return 0

        # Give it one retry
        try:
            self.subscriptions.update(subscriptions)
        except IOError:
            sleep(1)
            self.subscriptions.update(subscriptions)

        if save:
            with open(self.zsubfile, 'a') as f:
                f.writelines(",".join(sub) + "\n" for sub in subscriptions)
        return len(subscriptions)

    @exported
    def isSubscribed(self, subscription):
        """Returns true if the user is subscribed to the given triplet."""
        return tuple(subscription) in self.subscriptions
