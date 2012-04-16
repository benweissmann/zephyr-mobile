from functools import wraps
from . import VERSION as server_version
from exceptions import VersionMismatchError, AuthenticationRequired
from settings import INFO_FILE
from auth import checkToken, checkTickets, RenewTicketsTimer

def exported(obj):
    obj._export = True
    return obj

def error(msg):
    print(msg)

def sync(func):
    @wraps(func)
    def do(self, *args, **kwargs):
        with self.lock:
            return func(self, *args, **kwargs)
    return do

def assertCompatable(version):
    if not isinstance(version, int):
        raise TypeError("Version must be an int")
    if version > server_version:
        raise VersionMismatchError(server_version, version)

def assertAuthenticated(token):
    if not checkToken(token) or not checkTickets():
        raise AuthenticationRequired("Invalid token.")

def runserver(server):
    import fcntl, os, signal

    # Call finally
    signal.signal(signal.SIGHUP, exit)
    signal.signal(signal.SIGTERM, exit)

    info_contents = {"PID": os.getpid(),
                     "VERSION": server_version}
    info_contents.update(server.getInfo())

    try:
        fd = os.open(INFO_FILE, os.O_WRONLY|os.O_CREAT|os.O_EXCL, 0000)
        fcntl.flock(fd, fcntl.LOCK_EX|fcntl.LOCK_NB)
        os.fchmod(fd, 0600)
    except OSError:
        print "Failed to create info file."
        exit(1)
    
    ticket_timer = RenewTicketsTimer(server.shutdown)
    try:
        fobj = os.fdopen(fd, "w")
        fobj.writelines("%s='%s'\n" % i for i in info_contents.iteritems())
        fobj.flush()
        fcntl.flock(fd, fcntl.LOCK_SH|fcntl.LOCK_NB)
        ticket_timer.start()
        server.serve_forever()
    finally:
        ticket_timer.stop()
        fobj.close()
        os.remove(INFO_FILE)

