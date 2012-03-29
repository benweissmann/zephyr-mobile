#!/usr/bin/env python
# encoding: utf-8
import logging
logging.basicConfig(level=logging.DEBUG)
from . import return_status, exported
import settings
import sqlite3
import os
from itertools import izip
from functools import wraps
sqlite3.register_converter("BOOL", lambda v: v != "0")

def transaction(func):
    @wraps(func)
    def do(self, *args, **kwargs):
        try:
            self.db.commit()
            r = func(self, *args, **kwargs)
            self.db.commit()
            return r
        except BaseException as e:
            self.db.rollback()
            raise e
    return do

def open_or_create_db(path):
    """
    Either open an existing db or create a new one.

    Arguments:
        path -  the path to the database. the database does not need to exist
                but the parent directory must.
    """
    if path != ":memory:":
        directory = os.path.dirname(path)
        if not os.path.isdir(directory):
            os.makedirs(directory)
    db = sqlite3.connect(path, detect_types=sqlite3.PARSE_DECLTYPES)
    db.row_factory = lambda cursor, row: dict(izip((c[0] for c in cursor.description), row))
    db.execute("""CREATE TABLE IF NOT EXISTS messages (
        id          INTEGER NOT NULL PRIMARY KEY,
        sender      TEXT    NOT NULL,
        message     TEXT    NOT NULL,
        read        BOOL    NOT NULL DEFAULT 0,
        cls         TEXT    NOT NULL DEFAULT "message",
        instance    TEXT    NOT NULL DEFAULT "personal",
        user        TEXT    DEFAULT NULL,
        timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )""")
    db.commit()
    return db

class Filter(object):
    """
    A filter that is an or of its clauses anded with its parent (recursivly).
    """

    def __init__(self, cls=None, instance=None, user=None, sender=None, read=None, message=None, after=None, before=None):
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
        f = tuple([ (i,s)  for i,s in (
            (cls, "cls=?"),
            (instance, "instance=?"),
            (user, "user=?"),
            (sender, "sender=?"),
            (int(read) if read is not None else None, "read=?"),
            (("%" + message + "%") if message is not None else None, 'message LIKE ?'),
            (after, "timestamp > ?"),
            (before, "timestamp < ?"),
        ) if i is not None])
        self.fid = hash(f)


        if f:
            self._objs, query_list = zip(*f)
            self._where = " WHERE " + " AND ".join(query_list)
        else:
            self._objs = ()
            self._where = ""

    def __hash__(self):
        return self.fid

    def applyQuery(self, db, action, offset=0, perpage=-1):
        return db.execute(
            "%s FROM messages %s ORDER BY timestamp LIMIT ? OFFSET ?" % (action, self._where),
            self._objs + (perpage,offset)
        )

    def get(self, db, offset=0, perpage=-1):
        return self.applyQuery(db, "SELECT *", offset, perpage).fetchall()

    def delete(self, db, offset=0, perpage=-1):
        return self.applyQuery(db, "DELETE", offset, perpage).rowcount

    def markRead(self, db, updates):
        return db.execute("UPDATE messages SET read=1" + self._where, self._objs).rowcount

    def markUnread(self, db, updates):
        return db.execute("UPDATE messages SET read=0" + self._where, self._objs).rowcount

    def count(self, db, offset=0, perpage=-1):
        return self.applyQuery(db, "SELECT count(*) AS total FROM messages", offset, perpage).fetchone()["total"]

    def getIDs(self, db, offset=0, perpage=-1):
        return [i["id"] for i in self.applyQuery(db, "SELECT id FROM messages", offset, perpage)]


    def filterResponse(self, db, offset=0, perpage=-1):
        return {
            "filter": str(self.fid),
            "messages": self.get(db, offset, perpage),
            "perpage": perpage,
            "offset": offset,
        }

class Messenger(object):
    def __init__(self, username, db_path = settings.ZEPHYR_DB):
        self.db = open_or_create_db(db_path)
        self.username = username
        self.filters = {}

    @transaction
    def store_messages(self, *messages):
        """ Stores a message and returns its ID.

        >>> messenger = Messenger("ME", ":memory:")
        >>> messenger.store_messages(
        >>>     ("SENDER", "MESSAGE", "CLASS", "INSTANCE", "USER"),
        >>>     ("SENDER", "MESSAGE2", "CLASS", "INSTANCE", "USER")
        >>> )
        """

        return self.db.executemany(
            'INSERT INTO messages(sender, message, cls, instance, user) VALUES (?, ?, ?, ?, ?)',
            iter(messages)
        )

    @exported
    @return_status
    def send(self, message, cls="message", instance="personal", user=None):
        """
        Send a zephyr.

        Arguments:
            message - The message content
            cls - the message class (defaults to "message")
            instance - the message instance (defaults to "personal")
            user - the destination user (None means everyone)

        >>> messenger = Messenger("ME", ":memory:")
        # Send a message to -c help -i linux
        >>> messenger.send("This is a really short message.", "help", "linux")
        # Send a message to bsw (the two following are equivalent)
        >>> messenger.send("This is a really short message.", None, None, "bsw")
        >>> messenger.send("This is a really short message.", "message", "personal", "bsw")
        """

        self.store_messages((self.username, message, cls, instance, user))

    @exported
    def filterMessages(self, messageFilter):
        """
        Filters messages.
        Arguments:
            messageFilter - a filter in the form of {"field": "value"}
            Valid fields are:
                sender, class, instance, user, message, before, after, read


        Returns:
            fid - a string filterID and count.

        # Matches all unread messages in class help.
        >>> fid = messenger.filterMessages({"cls": "help", "read": False})
        # Get the messages that match the filter
        >>> messenger.get(fid)
        """
        f = Filter(**messageFilter)
        self.filters[f.fid] = f
        return str(f.fid)

    @exported
    def get(self, fid, offset=0, perpage=-1):
        """
        Get the messages that match fid.
        Arguments:
            fid     - the filter's ID
            offset  - the first item to return from the matched messages.
                      A negitive offset will index backwards.
            perpage - The maximum number of of results to return. Passing a
                      negative number returns all results.
        
        >>> from . import subscriptions
        >>> m = Messenger(subscriptions.SubscriptionManager("ME"), "ME")
        >>> m.store_message(
        >>> message = {
        >>>     "id": int,
        >>>     "timestamp": int,
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
        >>>     "messages": list    # A list of message dictionaries.
        >>> }


        """
        return self.filters[int(fid)].filterResponse(self.db, offset, perpage)


    @exported
    def hasNew(self, last, messageFilter=None):
        """
        Returns True if there is a message that matches messageFilter newer
        than last.
        """
        # XXX: SQLITE
        return True

    @exported
    @transaction
    def delete(self, ids):
        """
        Deletes the given messages.
        Returns the number deleted.
        """
        return self.db.executemany("DELETE FROM messages WHERE id=?", ((i,) for i in ids)).rowcount

    @exported
    @transaction
    def deleteFilter(self, fid, offset=0, perpage=-1):
        """
        Delete the messages that match the given filter.
        Returns the number deleted.
        """
        return self.filters[int(fid)].delete(self.db, offset, perpage)

    @exported
    def markFilter(self, status, fid, offset=0, perpage=-1):
        if status == "read":
            return self.markFilterRead(offset, perpage)
        elif status == "unread":
            return self.markFilterUnread(offset, perpage)

    @exported
    @transaction
    def markFilterRead(self, fid):
        """ Mark all of the messages that match a filter with the given status. """
        return self.filters[int(fid)].markRead(self.db)

    @exported
    @transaction
    def markFilterUnread(self, fid):
        """ Mark all of the messages that match a filter with the given status. """
        return self.filters[int(fid)].markUnread(self.db)


    @exported
    def mark(self, status, ids):
        """ Mark the given messages as read. """
        if status == "read":
            return self.markRead()
        elif status == "unread":
            return self.markUnread()

    @exported
    @transaction
    def markRead(self, ids):
        return self.db.executemany("UPDATE messages SET read=1 WHERE id=?", ((i,) for i in ids)).rowcount

    @exported
    @transaction
    def markUnread(self, ids):
        return self.db.executemany("UPDATE messages SET read=0 WHERE id=?", ((i,) for i in ids)).rowcount



    @exported
    @transaction
    def getIDs(self, fid=None, offset=0, perpage=-1):
        """ Get all of the message ids that match the given filter. """
        return self.filters[int(fid)].getIDs(self.db, offset, perpage)

    @exported
    def getInstances(self, cls, offset=0, perpage=-1):
        """ List the instances with messages in a given class.

        returns:
            [("instance", [unread_count, total_count]), ...]
        """
        return [ (r["instance"], (r["unread"], r["total"])) for r in self.db.execute(
            """
            SELECT instance, COUNT(*) AS total, COUNT(unread) AS unread
            FROM (SELECT cls, instance, nullif(read, 1) AS unread, timestamp FROM messages)
            WHERE cls=?
            GROUP BY instance
            ORDER BY MAX(timestamp)
            LIMIT ? OFFSET ?
            """, (cls, perpage, offset)) ]

    @exported
    def getUnreadInstances(self, cls, offset=0, perpage=-1):
        """ List the instances with messages in a given class.

        returns:
            [("instance", [unread_count, total_count]), ...]
        """
        return [ (r["instance"], (r["unread"], r["total"])) for r in self.db.execute(
            """
            SELECT instance, COUNT(*) AS total, COUNT(unread) AS unread
            FROM (SELECT cls, instance, read, nullif(read, 1) AS unread, timestamp FROM messages)
            WHERE cls=? AND read=0
            GROUP BY instance
            ORDER BY MAX(timestamp)
            LIMIT ? OFFSET ?
            """, (cls, perpage, offset)) ]



    @exported
    def getClasses(self, offset=0, perpage=-1):
        """
        List the classes with messages.
        Returns:
            (class, [unread_count, total_count], ...)
        """
        return [ (r["cls"], (r["unread"], r["total"])) for r in self.db.execute(
            """
            SELECT cls, COUNT(*) AS total, COUNT(unread) AS unread
            FROM (SELECT cls, nullif(read, 1) AS unread, timestamp FROM messages)
            GROUP BY cls
            ORDER BY MAX(timestamp)
            LIMIT ? OFFSET ?
            """, (perpage, offset)) ]

    @exported
    def getUnreadClasses(self, offset=0, perpage=-1):
        """
        List the classes with messages.
        Returns:
            (class, [unread_count, total_count], ...)
        """
        return [ (r["cls"], (r["unread"], r["total"])) for r in self.db.execute(
            """
            SELECT cls, COUNT(*) AS total, COUNT(unread) AS unread
            FROM (SELECT cls, read, nullif(read, 1) AS unread, timestamp FROM messages)
            WHERE read=0
            GROUP BY cls
            ORDER BY MAX(timestamp)
            LIMIT ? OFFSET ?
            """, (perpage, offset)) ]


    @exported
    def getCount(self, fid=None):
        """ Get the number of messages that match a filter. """
        if fid is not None:
            return self.filters[int(fid)].count()
        else:
            return self.db.execute("SELECT COUNT(*) AS total FROM messages").fetchone()["total"]

if __name__ == '__main__':
    from subscriptions import SubscriptionManager
    m = Messenger(SubscriptionManager("ME"), "ME")
    f, c = m.filterMessages()
