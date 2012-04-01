from SimpleXMLRPCServer import SimpleXMLRPCServer
from subscriptions import SubscriptionManager
from messenger import Messenger
from datetime import datetime
from . import exported

class Server(object):
    def __init__(self):
        self._start_time = datetime.now()
        self.username = "ME"
        self.subscriptions = exported(SubscriptionManager(self.username))
        self.messenger = exported(Messenger(self.username))

    def _dispatch(self, method, params):
        obj = self
        for i in method.split('.'):
            obj = getattr(obj, i)
            if not getattr(obj, "_export", False):
                raise AttributeError("Method not supported.")

        return obj(*params)

    @exported
    def uptime(self):
        """Returns the uptime in seconds."""
        return (datetime.now() - self._start_time).total_seconds()


    @exported
    def getUser(self):
        return self.username

server = SimpleXMLRPCServer(('localhost', 9000), allow_none=True)
server.register_instance(Server())
server.register_introspection_functions()

try:
    server.serve_forever()
except KeyboardInterrupt:
    print "exiting..."

