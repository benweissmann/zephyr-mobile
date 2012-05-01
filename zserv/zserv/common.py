from functools import wraps
from . import VERSION as server_version
from exceptions import VersionMismatchError, AuthenticationRequired
from settings import INFO_FILE, LOCK_FILE
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
    signal.signal(signal.SIGHUP, lambda *args: exit(0))
    signal.signal(signal.SIGTERM, lambda *args: exit(0))

    info_contents = {"VERSION": server_version}
    info_contents.update(server.getInfo())

    lck_fd = None
    try:
        try:
            lck_fd = os.open(LOCK_FILE, os.O_WRONLY|os.O_CREAT|os.O_EXCL, 0000)
        except (IOError, OSError):
            lck_fd = None # Just in case.
            print "Failed to create lock."
            exit(1)

        try:
            fcntl.flock(lck_fd, fcntl.LOCK_EX|fcntl.LOCK_NB)
        except (IOError, OSError):
            print "Failed to lock lock."
            exit(1)

        try:
            os.write(lck_fd, str(os.getpid()))
        except (IOError, OSError):
            print "Failed to write PID to lock."
            exit(1)
        
        try:
            os.fchmod(lck_fd, 0600)
        except (IOError, OSError):
            print "Failed to make lock readable."
            exit(1)
        
        try:
            with open(INFO_FILE, "w+", 0600) as info_file:
                info_file.writelines("%s='%s'\n" % i for i in info_contents.iteritems())
        except IOError:
            print "Failed to write info file."
            exit(1)

        try:
            fcntl.flock(lck_fd, fcntl.LOCK_SH|fcntl.LOCK_NB)
        except (IOError, OSError):
            print "Failed to make lock shared."
            exit(1)

        ticket_timer = RenewTicketsTimer(server.shutdown)
        try:
            ticket_timer.start()
            server.serve_forever()
        finally:
            ticket_timer.stop()
    finally:
        if lck_fd is not None:
            try:
                os.close(lck_fd)
            except (OSError, IOError):
                pass
        if os.path.exists(INFO_FILE):
            os.remove(INFO_FILE)
        if os.path.exists(LOCK_FILE):
            os.remove(LOCK_FILE)

