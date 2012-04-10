from collections import deque
from threading import Condition
from time import time

__all__ = ("sender", "receive", "interrupt", "ZNotice", "realm", "Subscriptions", "init")
class SimpleBlockingQueue(object):
    def __init__(self):
        self.items = deque()
        self.condition = Condition()
        self.interrupted = False

    def put(self, item):
        with self.condition:
            self.items.pushleft(item)

    def take(self, block=False):
        with self.condition:
            if block:
                while not self.items:
                    self.condition.wait()
                    if self.interrupted:
                        self.interrupted = False
                        return None
            elif not self.items:
                return None

            return self.items.popright()

    def interrupt(self):
        self.condition.notifyAll()



realm = lambda: "ATHENA.MIT.EDU"
sender = lambda: "ME"

queue = SimpleBlockingQueue()

receive = queue.take
interrupt = queue.interrupt

class ZNotice(object):
    def __init__(self, **options):
        self.kind = 2
        self.cls = 'message'
        self.instance = 'personal'

        self.uid = 0
        self.time = time()
        self.port = 0
        self.auth = True
        self.recipient = ''
        self.sender = 'ME'
        self.opcode = ''
        self.format = "Class $class, Instance $instance:\nTo: @bold($recipient) at $time $date\nFrom: @bold{$1 <$sender>}\n\n$2"
        self.other_fields = []
        self.fields = []

        for k, v in options.iteritems():
            setattr(self, k, v)

    def getmessage(self):
        return '\0'.join(self.fields)

    def setmessage(self, newmsg):
        self.fields = newmsg.split('\0')

    message = property(getmessage, setmessage)

    def send(self):
        queue.put(self)

Subscriptions = set
init = lambda:None
