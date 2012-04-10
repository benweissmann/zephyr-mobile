#!/usr/bin/env python
# encoding: utf-8
import logging
logging.basicConfig(level=logging.DEBUG)
from common import return_status, exported
from settings import ZEPHYR_DB, preferences
import sqlite3
from itertools import izip
from functools import wraps
from threading import Thread, RLock
from datetime import datetime
sqlite3.register_converter("BOOL", lambda v: v != "0")
try:
    import zephyr
except ImportError:
    import test_zephyr as zephyr

__all__ = ("Filter", "Messenger")

def transaction(func):
    @wraps(func)
    def do(self, *args, **kwargs):
        with self.lock:
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
    # Multi-threading ... should be safe
    db = sqlite3.connect(path, detect_types=sqlite3.PARSE_DECLTYPES, check_same_thread=False)
    db.row_factory = lambda cursor, row: dict(izip((c[0] for c in cursor.description), row))
    db.execute("""CREATE TABLE IF NOT EXISTS messages (
        id          INTEGER NOT NULL PRIMARY KEY,
        sender      TEXT    NOT NULL,
        auth        BOOL    NOT NULL DEFAULT 1,
        signature   TEXT    NOT NULL DEFAULT "",
        message     TEXT    NOT NULL,
        read        BOOL    NOT NULL DEFAULT 0,
        cls         TEXT    NOT NULL DEFAULT "message",
        instance    TEXT    NOT NULL DEFAULT "personal",
        user        TEXT    DEFAULT NULL,
        timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )""")
    db.commit()
    return db

def gen_params(num):
    return "(" + "?,"*(num-1) + "?)"

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
            self._objs, self._query_list = zip(*f)
            self._where = " WHERE " + " AND ".join(self._query_list)
        else:
            self._query_list = ()
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

    def delete(self, db):
        return db.execute("DELETE FROM messages" + self._where, self._objs).rowcount

    def markRead(self, db, updates):
        return db.execute("UPDATE messages SET read=1" + self._where, self._objs).rowcount

    def markUnread(self, db, updates):
        return db.execute("UPDATE messages SET read=0" + self._where, self._objs).rowcount

    def count(self, db, offset=0, perpage=-1):
        return self.applyQuery(db, "SELECT count(*) AS total", offset, perpage).fetchone()["total"]

    def counts(self, db):
        """
        Returns (unread_count, total_count)
        """
        query = """
                SELECT count(*) AS total, count(unread) AS unread
                FROM (
                    SELECT nullif(read, 1) AS unread
                    FROM messages
                    %s
                )
                """ % (self._where)
        result = db.execute(query, self._objs).fetchone()
        return (result['unread'], result['total'])

    def oldestUnreadOffset(self, db):
        """
        selects the offset of the oldest unread zephyr using a query similar to

        SELECT count(*) AS offset
        FROM  messages
        WHERE col1=val1 AND col2=val2 AND timestamp < (
            SELECT timestamp
            FROM messages
            WHERE col1=val1 AND col2=val2 AND read=0
            ORDER BY timestamp
            LIMIT 1
        )

        If there are no unread messages, uses -1

        Returns (offset, total_count)
        """

        unread, total = self.counts(db)
        if unread == 0:
            return (-1, total)

        subquery_where = " AND ".join(self._query_list + ("read=0",))
        subquery = "SELECT timestamp FROM messages WHERE %s ORDER BY timestamp LIMIT 1" % (subquery_where)
        query_where_addendum = "timestamp < (%s)" % (subquery)
        query_where = " AND ".join(self._query_list + (query_where_addendum,))
        query = "SELECT count(*) AS offset FROM messages WHERE %s" % (query_where)
        return (db.execute(query, self._objs*2).fetchone()["offset"], total)

    def getIDs(self, db, offset=0, perpage=-1):
        return [i["id"] for i in self.applyQuery(db, "SELECT id", offset, perpage)]


    def filterResponse(self, db, offset=0, perpage=-1):
        return {
            "filter": str(self.fid),
            "messages": self.get(db, offset, perpage),
            "perpage": perpage,
            "offset": offset,
        }

class Messenger(Thread):
    def __init__(self, username, db_path=ZEPHYR_DB):
        super(Messenger, self).__init__()
        self.db = open_or_create_db(db_path)
        self.username = username
        self.filters = {}
        self.lock = RLock()

    def run(self):
        while True:
            z = zephyr.receive(block=True)
            if z is None:
                return
            self.store_znotice(zephyr.receive())

    def quit(self):
        zephyr.interrupt()
        self.join()

    # FOR TESTING
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

    @transaction
    def store_znotice(self, znotice):
        if znotice.opcode == 'PING':
            return # Deal with pings later XXX FIXME XXX
        elif znotice.opcode != '':
            return # Not a message

        msg = ""
        sig = ""
        if len(znotice.fields) == 1:
            msg = znotice.fields[0]
        elif len(znotice.fields) > 1:
            sig = znotice.fields[0]
            msg = znotice.fields[1]

        return self.db.execute(
            'INSERT INTO messages(sender, auth, signature, message, cls, instance, user, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
            (
                znotice.sender,
                znotice.auth,
                sig,
                msg,
                znotice.cls,
                znotice.instance,
                znotice.recipient or None,
                datetime.fromtimestamp(znotice.time)
            )
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
        # Send a message to bsw
        >>> messenger.send("This is a really short message.", "message", "personal", "bsw")
        """
        zephyr.ZNotice(
            cls=cls,
            instance=instance,
            recipient=user,
            message="%s\x00%s" % (preferences["signature"], message)).send()

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
        if not ids:
            return 0;
        return self.db.execute("DELETE FROM messages WHERE id IN " + gen_params(len(ids)), tuple(ids)).rowcount


    @exported
    @transaction
    def deleteFilter(self, fid, offset=0, perpage=-1):
        """
        Delete the messages that match the given filter.
        Returns the number deleted.
        """
        if offset != 0 or perpage >= 0:
            return self.delete(self.filters[int(fid)].getIDs(self.db, offset, perpage))
        else:
            return self.filters[int(fid)].delete(self.db)

    @exported
    def markFilter(self, status, fid, offset=0, perpage=-1):
        if status == "read":
            return self.markFilterRead(fid)
        elif status == "unread":
            return self.markFilterUnread(fid)

    @exported
    @transaction
    def markFilterRead(self, fid, offset=0, perpage=-1):
        """ Mark all of the messages that match a filter with the given status. """
        if offset != 0 or perpage >= 0:
            return self.markRead(self.filters[int(fid)].getIDs(self.db, offset, perpage))
        else:
            return self.filters[int(fid)].markRead(self.db)

    @exported
    @transaction
    def markFilterUnread(self, fid, offset=0, perpage=-1):
        """ Mark all of the messages that match a filter with the given status. """
        if offset != 0 or perpage >= 0:
            return self.markUnread(self.filters[int(fid)].getIDs(self.db, offset, perpage))
        else:
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
        if not ids:
            return 0;
        return self.db.execute("UPDATE messages SET read=1 WHERE id IN " + gen_params(len(ids)), tuple(ids)).rowcount

    @exported
    @transaction
    def markUnread(self, ids):
        if not ids:
            return 0;
        return self.db.execute("UPDATE messages SET read=0 WHERE id IN " + gen_params(len(ids)), tuple(ids)).rowcount

    @exported
    @transaction
    def getIDs(self, fid=None, offset=0, perpage=-1):
        """ Get all of the message ids that match the given filter. """
        return self.filters[int(fid)].getIDs(self.db, offset, perpage)

    @exported
    def getInstances(self, cls, offset=0, perpage=-1):
        """ List the instances with messages in a given class.

        Returns:
            [{
                instance: instance,
                unread: unread_count,
                total: total_count,
             },
            ]
        """
        return self.db.execute(
            """
            SELECT instance, COUNT(*) AS total, COUNT(unread) AS unread
            FROM (SELECT cls, instance, nullif(read, 1) AS unread, timestamp FROM messages)
            WHERE cls=?
            GROUP BY instance
            ORDER BY MAX(timestamp) DESC
            LIMIT ? OFFSET ?
            """, (cls, perpage, offset)).fetchall()

    @exported
    def getUnreadInstances(self, cls, offset=0, perpage=-1):
        """ List the instances with messages in a given class.

        Returns:
            [{
                instance: instance,
                unread: unread_count,
                total: total_count,
             },
            ]
        """
        return self.db.execute(
            """
            SELECT instance, COUNT(*) AS total, COUNT(unread) AS unread
            FROM (SELECT cls, instance, read, nullif(read, 1) AS unread, timestamp FROM messages)
            WHERE cls=? AND read=0
            GROUP BY instance
            ORDER BY MAX(timestamp) DESC
            LIMIT ? OFFSET ?
            """, (cls, perpage, offset)).fetchall()



    @exported
    def getClasses(self, offset=0, perpage=-1):
        """
        List the classes with messages.
        Returns:
            [{
                cls: class,
                unread: unread_count,
                total: total_count,
                starred: true if the class is starred,
             },
            ]
        """
        classes = self.db.execute(
            """
            SELECT cls, COUNT(*) AS total, COUNT(unread) AS unread
            FROM (SELECT cls, nullif(read, 1) AS unread, timestamp FROM messages)
            GROUP BY cls
            ORDER BY MAX(timestamp) DESC
            LIMIT ? OFFSET ?
            """, (perpage, offset)).fetchall()

        # XXX: This is wrong. I should get all of the subscribed classes.

        starred_classes = set(preferences["starred-classes"])
        for c in classes:
            c["starred"] = c["cls"] in starred_classes

        return classes


    @exported
    def getUnreadClasses(self, offset=0, perpage=-1):
        """
        List the classes with messages.
        Returns:
            [{
                cls: class,
                unread: unread_count,
                total: total_count,
                starred: true if the class is starred,
             },
            ]

        """
        classes = self.db.execute(
            """
            SELECT cls, COUNT(*) AS total, COUNT(unread) AS unread
            FROM (SELECT cls, read, nullif(read, 1) AS unread, timestamp FROM messages)
            WHERE read=0
            GROUP BY cls
            ORDER BY MAX(timestamp) DESC
            LIMIT ? OFFSET ?
            """, (perpage, offset)).fetchall()

        # I store the starred attribute separately because it is not a feature of zephyr.
        starred_classes = set(preferences["starred-classes"])
        for c in classes:
            c["starred"] = c["cls"] in starred_classes

        return classes

    @exported
    def getPersonals(self, offset=0, perpage=-1):
        """
        List the users that have sent personals.
        Returns:
            [
                {
                    sender: sender,
                    unread: unread_count,
                    total: total_count,
                }, ...
            ]
        """
        return self.db.execute(
            """
            SELECT sender, COUNT(*) AS total, COUNT(unread) AS unread
            FROM (
                SELECT sender, read, nullif(read, 1) AS unread, timestamp, cls, instance, user
                FROM messages
                WHERE user IS NOT NULL AND instance=? AND cls=?
            )
            GROUP BY sender
            ORDER BY MAX(timestamp) DESC
            LIMIT ? OFFSET ?
            """, ("personal", "message", perpage, offset)).fetchall();

    @exported
    def getCount(self, fid=None):
        """ Get the number of messages that match a filter. """
        if fid is not None:
            return self.filters[int(fid)].count(self.db)
        else:
            return self.db.execute("SELECT COUNT(*) AS total FROM messages").fetchone()["total"]

    @exported
    def getOldestUnreadOffset(self, fid):
        """
        Gets the offset of the oldest unread message that matches a filter, or
        -1 if there are no unread messages. Produces undefined behavior
        if the filter contains a condition on read. Returns
        (offset, total_count)
        """
        return self.filters[int(fid)].oldestUnreadOffset(self.db)
