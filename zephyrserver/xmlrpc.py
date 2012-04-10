from SimpleXMLRPCServer import SimpleXMLRPCServer
from subscriptions import SubscriptionManager
from messenger import Messenger
from time import time
from common import exported, assertCompatable, assertAuthenticated
import preferences
import sys
from . import VERSION
import ssl

try:
    import zephyr
except ImportError:
    print "Failed to import zephyr, using test zephyr."
    import test_zephyr as zephyr


__all__ = ('ZephyrXMLRPCServer',)

DEFAULT_PORT = 9000
DEFAULT_HOST = "localhost"

class ZephyrXMLRPCServer(SimpleXMLRPCServer, object):
    def __init__(self, host=DEFAULT_HOST, port=DEFAULT_PORT, ssl=True):
        self.use_ssl = ssl
        super(ZephyrXMLRPCServer, self).__init__((host, port), allow_none=True)
        zephyr.init()
        self.username = zephyr.sender()
        self.preferences = exported(preferences.Preferences())
        self.subscriptions = exported(SubscriptionManager(self.username))
        self.messenger = exported(Messenger(self.username))

    def authenticate(self, username, password):
        # Call kinit. if true: set token
        return True #XXX

    def server_bind(self):
        if self.use_ssl:
            self.socket = ssl.wrap_socket(self.socket)
        super(ZephyrXMLRPCServer, self).server_bind()

    def _dispatch(self, method, params):
        # Need to pop
        params = list(params)
        if method == "getServerVersion":
            return VERSION
        else:
            try:
                assertCompatable(params.pop(0))
            except IndexError:
                raise TypeError("Minimum version unspecified.")

        if method == "authenticate":
            return self.authenticate(*params)
        else:
            try:
                print params
                assertAuthenticated(params.pop(0))
            except IndexError:
                raise TypeError("No authentication token provided.")

        obj = self
        for i in method.split('.'):
            obj = getattr(obj, i)
            if not getattr(obj, "_export", False):
                raise AttributeError("Method not supported.")

        return obj(*params)

    @exported
    def uptime(self):
        """Returns the uptime in seconds."""
        return time() - self.start_time


    @exported
    def getUser(self):
        return self.username

    # Does nothing but return true. Used to check auth/version...
    @exported
    def ping(self):
        return True

    def serve_forever(self):
        self.start_time = time()
        self.messenger.start()
        try:
            super(ZephyrXMLRPCServer, self).serve_forever()
        except KeyboardInterrupt:
            pass
        finally:
            print "exiting..."
            self.messenger.quit()


if __name__ == '__main__':
    if len(sys.argv) > 1:
        host = sys.argv[1]
        if len(sys.argv) > 2:
            port = int(sys.argv[2] or DEFAULT_PORT)
        else:
            port = DEFAULT_PORT
    else:
        host = DEFAULT_HOST
    ZephyrXMLRPCServer(host, port).serve_forever()
