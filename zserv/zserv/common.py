from functools import wraps
from . import VERSION as server_version
from exceptions import VersionMismatchError, AuthenticationRequired
from settings import INFO_FILE, LOCK_FILE, LOGFILE
from auth import checkToken, checkTickets, RenewTicketsTimer
from multiprocessing import Event
import sys, os, fcntl, signal

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

def runserver(server_class, args, dofork=False):
    os.umask(077) # Default to private files.

    server_started = Event() # Fired when the child is ready

    # Call finally
    signal.signal(signal.SIGHUP, lambda *args: exit(0))
    signal.signal(signal.SIGTERM, lambda *args: exit(0))

    lck_fd = None
    try:
        lck_fd = os.open(LOCK_FILE, os.O_WRONLY|os.O_CREAT|os.O_EXCL, 0600)
    except (IOError, OSError):
        lck_fd = None # Just in case.
        print "Failed to create lock."
        exit(1)

    # We are now sure that no other server is running and can remove lock files
    # etc. on cleanup

    try:
        try:
            fcntl.flock(lck_fd, fcntl.LOCK_EX|fcntl.LOCK_NB)
        except:
            print "Failed to lock lock."
            exit(1)

        if dofork:
            fork(server_started)

        ### Child stuff. ###

        try:
            os.write(lck_fd, str(os.getpid()))
        except (IOError, OSError):
            print "Failed to write PID to lock."
            exit(1)

        # Initialize the server *after* forking.
        server = server_class(**args)

        # Get info
        info_contents = {"VERSION": server_version}
        info_contents.update(server.getInfo())

        
        try:
            with open(INFO_FILE, "w+", 0600) as info_file:
                info_file.writelines("%s='%s'\n" % i for i in info_contents.iteritems())
            print INFO_FILE
        except IOError:
            print "Failed to write info file."
            exit(1)

        server_started.set()

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

def fork(event):
    pid = os.fork()
    ret = 0
    if pid:
        try:
            # Wait for child.
            new_pid, status = os.waitpid(pid, 0)
            # Child has exited, return childs exit code.
            if new_pid == pid:
                if os.WIFSIGNALED(status):
                    print "Child killed. Grandchild should be dead."
                    ret = -os.WTERMSIG(status)
                else:
                    if os.WIFEXITED(status):
                        ret = os.WEXITSTATUS(status)
                    else:
                        print "WTF. The child didn't exit normally and wasn't killed."
                        ret = -1
        finally:
            os._exit(ret)
    else:
        # This is the child. Fork again.
        os.setsid()
        pid = os.fork()
        if pid:
            try:
                # Wait for grandchild
                event.wait(5)
                if event.is_set():
                    new_pid, status = os.waitpid(pid, os.WNOHANG)
                    if new_pid == pid:
                        if os.WIFSIGNALED(status):
                            print "Child killed. Grandchild should be dead."
                            ret = -os.WTERMSIG(status)
                        else:
                            if os.WIFEXITED(status):
                                ret = os.WEXITSTATUS(status)
                            else:
                                print "WTF. The child didn't exit normally and wasn't killed."
                                ret = -1
                else:
                    print "Timed out waiting for grandchild. Killing grandchild."
                    os.kill(pid, 15)
                    ret = 1
            except BaseException:
                print "Exception in child. Killing grandchild."
                os.kill(pid, 15)
                ret = 1
            finally:
                os._exit(ret)
        else:
            # grandchild.
            sys.stdin.close()
            sys.stdout = open(LOGFILE, 'a')
            sys.stderr = open(LOGFILE, 'a')
