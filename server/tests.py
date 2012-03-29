import unittest

class testMessenger(unittest.TestCase):

    def setUp(self):
        from messenger import Messenger
        self.messenger = Messenger("ME", ":memory:")

    def testStoreMessages(self):
        from datetime import datetime
        self.messenger.store_messages(
            ("SENDER", "MESSAGE", "CLASS", "INSTANCE", "USER"),
            ("SENDER", "MESSAGE2", "CLASS", "INSTANCE", "USER")
        ) 
        m1, m2 = self.messenger.db.execute("SELECT * FROM messages ORDER BY id").fetchall()
        self.assertLessEqual((datetime.now() - m1["timestamp"]).total_seconds(), 5)
        del m1["timestamp"]
        self.assertLessEqual((datetime.now() - m2["timestamp"]).total_seconds(), 5)
        del m2["timestamp"]

        self.assertEqual(m1, {
            "sender": "SENDER",
            "message": "MESSAGE",
            "cls": "CLASS",
            "instance": "INSTANCE",
            "user": "USER",
            "id": 1,
            "read": False
        })

        self.assertEqual(m2, {
            "sender": "SENDER",
            "message": "MESSAGE2",
            "cls": "CLASS",
            "instance": "INSTANCE",
            "user": "USER",
            "id": 2,
            "read": False
        })

    def populateTestMessages(self):
        self.messenger.store_messages(
            ("steb", "Short basic message.", "offtopic", "offtopic.d", None),
            ("bsw", "First linux message.", "help", "linux", None),
            ("bsw", "Second linux message.", "help", "linux", None),
            ("bsw", "Linux.d message.", "help", "linux.d", None),
            ("bsw", "Other help message", "help", "other", None),
            ("bsw", "User Message", "help", "other", None)
        )

    def timedPopulateTestMessages(self):
        from datetime import datetime
        from time import sleep
        self.messenger.store_messages(
            ("steb", "Short basic message.", "offtopic", "offtopic.d", None),
            ("bsw", "First linux message.", "help", "linux", None)
        )
        sleep(1)
        t1 = datetime.utcnow()
        sleep(1)
        self.messenger.store_messages(
            ("bsw", "Second linux message.", "help", "linux", None),
            ("bsw", "Linux.d message.", "help", "linux.d", None)
        )
        sleep(1)
        t2 = datetime.utcnow()
        sleep(1)
        self.messenger.store_messages(
            ("bsw", "Other help message", "help", "other", None),
            ("bsw", "User Message", "help", "other", None)
        ) 
        return (t1, t2)

    def cleanResponse(self, response):
        for m in response["messages"]:
            del m["timestamp"]
            del m["id"]
        

    def testFilterMessagesClass(self):
        self.populateTestMessages()
        fid = self.messenger.filterMessages({"cls": "help"})

        self.assertIsInstance(fid, str)
        msgs = self.messenger.get(fid)
        self.cleanResponse(msgs)
        self.assertEquals(msgs, {
            'filter': fid,
            'messages': [
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'First linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'Second linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux.d',
                 'message': u'Linux.d message.',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},
                {'cls': u'help',
                 'instance': u'other',
                 'message': u'Other help message',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},
                {'cls': u'help',
                 'instance': u'other',
                 'message': u'User Message',
                 'read': False,
                 'sender': u'bsw',
                 'user': None}],
            'offset': 0,
            'perpage': -1
        })

    def testFilterMessagesMessage(self):
        self.populateTestMessages()
        fid = self.messenger.filterMessages({"message": "linux"})

        self.assertIsInstance(fid, str)
        msgs = self.messenger.get(fid)
        self.cleanResponse(msgs)
        self.assertEquals(msgs, {
            'filter': fid,
            'messages': [
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'First linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'Second linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux.d',
                 'message': u'Linux.d message.',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},],
            'offset': 0,
            'perpage': -1
        })

    def testFilterMessagesInstance(self):
        self.populateTestMessages()
        fid = self.messenger.filterMessages({"instance": "linux"})

        self.assertIsInstance(fid, str)
        msgs = self.messenger.get(fid)
        self.cleanResponse(msgs)
        self.assertEquals(msgs, {
            'filter': fid,
            'messages': [
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'First linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'Second linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},],
            'offset': 0,
            'perpage': -1
        })

    def testFilterMessagesTime(self):
        t1, t2 = self.timedPopulateTestMessages()
        fid = self.messenger.filterMessages({"after": t1, "before": t2})

        self.assertIsInstance(fid, str)
        msgs = self.messenger.get(fid)
        self.cleanResponse(msgs)
        self.assertEquals(msgs, {
            'filter': fid,
            'messages': [
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'Second linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux.d',
                 'message': u'Linux.d message.',
                 'read': False,
                 'sender': u'bsw',
                 'user': None},],
            'offset': 0,
            'perpage': -1
        })
        


if __name__ == '__main__':
    unittest.main()
