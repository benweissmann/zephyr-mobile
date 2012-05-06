from SimpleXMLRPCServer import SimpleXMLRPCServer, SimpleXMLRPCRequestHandler
from subscriptions import SubscriptionManager
from messenger import Messenger
from time import time
from server import exported, assertCompatable, assertAuthenticated, runserver
from auth import authenticate
import preferences, logging
import os, ssl, socket
from . import VERSION, zephyr
import settings
from exceptions import ServerKilled
import inspect
from time import sleep

logger = logging.getLogger("xmlrpc")

DEFAULT_PORT = 0
DEFAULT_HOST = "0.0.0.0"

DEFAULT_SERVER_CERT = os.path.join(settings.DATA_DIR, "certs/server_public.pem")
DEFAULT_SERVER_KEY = os.path.join(settings.DATA_DIR, "certs/server_private.pem")

__all__ = ('ZephyrXMLRPCServer',)

class ZephyrXMLRPCRequestHandler(SimpleXMLRPCRequestHandler):
    def log_message(self, format, *args):
        logger.debug("%s - " + format, self.address_string(), *args)

    def log_error(self, format, args):
        logger.error("%s - " + format, self.address_string(), *args)

class ZephyrXMLRPCServer(SimpleXMLRPCServer, object):
    TYPE = "XML-RPC"
    def __init__(self,
                 host=DEFAULT_HOST,
                 port=DEFAULT_PORT,
                 ssl=False,
                 db=settings.ZEPHYR_DB,
                 keyfile=DEFAULT_SERVER_KEY,
                 certfile=DEFAULT_SERVER_CERT):
        self.use_ssl = ssl
        self.keyfile = keyfile
        self.certfile = certfile
        super(ZephyrXMLRPCServer, self).__init__((host, port), allow_none=True, requestHandler=ZephyrXMLRPCRequestHandler)

        try:
            zephyr.init()
        except IOError:
            sleep(1)
            zephyr.init()

        self.username = zephyr.sender()
        self.preferences = exported(preferences.Preferences())
        self.subscriptions = exported(SubscriptionManager(self.username))
        self.messenger = exported(Messenger(self.username, db_path=db))

    def server_bind(self):
        if self.use_ssl:
            self.socket = ssl.wrap_socket(self.socket, keyfile=self.keyfile, certfile=self.certfile, server_side=True)
        super(ZephyrXMLRPCServer, self).server_bind()

    def _dispatch(self, method, params):
        # Need to pop
        try:
            params = list(params)
            if method == "getServerVersion":
                return VERSION
            else:
                try:
                    assertCompatable(params.pop(0))
                except IndexError:
                    raise TypeError("Minimum version unspecified.")

            #TODO: Get rid of username argument
            if method == "authenticate":
                return authenticate(self.username, params[1])
            else:
                try:
                    assertAuthenticated(params.pop(0))
                except IndexError:
                    raise TypeError("No authentication token provided.")

            obj = self
            for i in method.split('.'):
                obj = getattr(obj, i)
                if not getattr(obj, "_export", False):
                    raise AttributeError("Method not supported.")

            try:
                return obj(*params)
            except Exception as e:
                logging.getLogger(getattr(inspect.getmodule(inspect.trace()[-1]), "__name__", __name__)).debug("%s: %s" % (e.__class__.__name__, e.message))
                raise e
        except KeyboardInterrupt:
            raise ServerKilled()

    def getInfo(self):
        addr, port = self.socket.getsockname()
        host = socket.getfqdn()
        # Hostname did not resolve:
        if host == "localhost.localdomain" and addr != "127.0.0.1":
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            try:
                s.connect(("mit.edu", 80))
                host = s.getsockname()[0]
            except socket.error:
                pass # Can't resolve mit.edu. meh...
            finally:
                s.close()

        return {
            "HOST": host,
            "PORT": port,
            "SSL": int(self.use_ssl),
            "CERT_PUB": self.certfile or self.keyfile or "",
        }

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
        finally:
            self.messenger.quit()

def parse_args():
    import argparse
    parser = argparse.ArgumentParser(description="A server for receiving, \
                                      storing, sending, and searching zephyrs \
                                      over XMLRPC.")

    parser.add_argument('-s', '--ssl',
                        dest='ssl',
                        default=False,
                        const=True,
                        action='store_const',
                        help="Use ssl."
                       )

    parser.add_argument('-k', '--keyfile',
                        dest='keyfile',
                        action="store",
                        default=DEFAULT_SERVER_KEY,
                        help="The server's private key."
                       )

    parser.add_argument('-c', '--cert',
                        dest='certfile',
                        action="store",
                        default=DEFAULT_SERVER_CERT,
                        help="The server's public key."
                       )

    parser.add_argument('-a', '--address',
                        dest='host',
                        action="store",
                        default=DEFAULT_HOST,
                        help="Address to listen on."
                       )

    parser.add_argument('-p', '--port',
                        dest='port',
                        action="store",
                        type=int,
                        default=DEFAULT_PORT,
                        help="Port to listen on."
                       )

    parser.add_argument('-f', '--fork',
                        dest='dofork',
                        default=False,
                        const=True,
                        action="store_const",
                        help="Daemonize the server."
                       )

    parser.add_argument('-d', '--debug',
                        dest='debug',
                        default=False,
                        const=True,
                        action="store_const",
                        help="Log debugging output."
                       )

    return vars(parser.parse_args())


if __name__ == '__main__':
    args = parse_args()
    dofork = args.pop('dofork')
    if args.pop('debug'):
        settings.LOG_LEVEL = logging.DEBUG
        logging.getLogger().setLevel(settings.LOG_LEVEL)
    runserver(ZephyrXMLRPCServer, args=args, dofork=dofork)
