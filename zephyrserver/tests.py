import unittest
#import test_zephyr as zephyr

class testMessenger(unittest.TestCase):

    def setUp(self):
        import messenger
        import settings

        settings.setVariable("starred-classes", ["help"])
        settings.setVariable("signature", "")
        settings.setVariable("hidden-classes", ["message"])

        self.messenger = messenger.Messenger("ME", ":memory:")

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
            'signature': '',
            "message": "MESSAGE",
            "cls": "CLASS",
            "signature": "",
            "instance": "INSTANCE",
            "user": "USER",
            "id": 1,
            "auth": True,
            "read": False
        })

        self.assertEqual(m2, {
            "sender": "SENDER",
            'signature': u'',
            "message": "MESSAGE2",
            "cls": "CLASS",
            "instance": "INSTANCE",
            "signature": "",
            "user": "USER",
            "id": 2,
            "auth": True,
            "read": False
        })

    def populateTestMessages(self):
        self.messenger.store_messages(
            ("steb", "Short basic message.", "offtopic", "offtopic.d", None),
            ("bsw", "First linux message.", "help", "linux", None),
            ("bsw", "Second linux message.", "help", "linux", None),
            ("bsw", "Linux.d message.", "help", "linux.d", None),
            ("bsw", "Other help message", "help", "other", None),
            ("bsw", "User Message", "help", "other", None),
            ("bsw", "M1", "message", "personal", "ME"),
            ("steb", "M2", "message", "personal", "ME")
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
        tst = {
            'filter': fid,
            'messages': [
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'First linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'signature': u'',
                 "auth": True,
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'Second linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'signature': u'',
                 "auth": True,
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux.d',
                 'message': u'Linux.d message.',
                 'read': False,
                 'sender': u'bsw',
                 'signature': u'',
                 "auth": True,
                 'user': None},
                {'cls': u'help',
                 'instance': u'other',
                 'message': u'Other help message',
                 'read': False,
                 'sender': u'bsw',
                 'signature': u'',
                 "auth": True,
                 'user': None},
                {'cls': u'help',
                 'instance': u'other',
                 'message': u'User Message',
                 'signature': u'',
                 'read': False,
                 'sender': u'bsw',
                 "auth": True,
                 'user': None}],
            'offset': 0,
            'perpage': -1
        }
        self.assertEquals(msgs, tst)

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
                 'signature': u'',
                 "auth": True,
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'Second linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'signature': u'',
                 "auth": True,
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux.d',
                 'message': u'Linux.d message.',
                 'read': False,
                 'signature': u'',
                 "auth": True,
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
                 'signature': u'',
                 "auth": True,
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux',
                 'message': u'Second linux message.',
                 'read': False,
                 'sender': u'bsw',
                 'signature': u'',
                 "auth": True,
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
                 'signature': u'',
                 "auth": True,
                 'user': None},
                {'cls': u'help',
                 'instance': u'linux.d',
                 'message': u'Linux.d message.',
                 'read': False,
                 'sender': u'bsw',
                 'signature': u'',
                 "auth": True,
                 'user': None},],
            'offset': 0,
            'perpage': -1
        })

    def testGetClasses(self):
        self.populateTestMessages()
        self.assertEquals(self.messenger.getClasses(),
                    [
                        {'cls': u'help', 'starred': True, 'total': 5, 'unread': 5},
                        {'cls': u'offtopic', 'starred': False, 'total': 1, 'unread': 1},
                    ]
                   )

    def testGetInstances(self):
        self.populateTestMessages()
        self.assertEquals(self.messenger.getInstances("help"),
                    [
                        {'instance': u'linux', 'total': 2, 'unread': 2},
                        {'instance': u'linux.d', 'total': 1, 'unread': 1},
                        {'instance': u'other', 'total': 2, 'unread': 2}
                    ]
                   )

    def testGetPersonals(self):
        self.populateTestMessages()
        self.assertEquals(self.messenger.getPersonals(),
                    [
                        {'sender': u'bsw', 'total': 1, 'unread': 1},
                        {'sender': u'steb', 'total': 1, 'unread': 1},
                    ]
                   )

    def testMarkReadUnread(self):
        self.populateTestMessages()
        fid = self.messenger.filterMessages({"sender": "bsw"})
        self.messenger.markFilterRead(fid, 1, 2)
        self.assertTrue(not self.messenger.get(fid, 0, 1)["messages"][0]["read"])
        self.assertTrue(all( i["read"] for i in self.messenger.get(fid, 1, 2)["messages"]))
        self.assertTrue(all( not i["read"] for i in self.messenger.get(fid, 3)["messages"]))
        self.messenger.markFilterUnread(fid, 1, 2)
        self.assertTrue(all( not i["read"] for i in self.messenger.get(fid)["messages"]))

    def testDelete(self):
        fid = self.messenger.filterMessages({"sender": "bsw"})
        self.messenger.deleteFilter(fid)
        self.assertTrue(len(self.messenger.get(fid)["messages"]) == 0)


if __name__ == '__main__':
    unittest.main()
