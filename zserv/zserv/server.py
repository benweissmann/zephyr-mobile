from . import VERSION as server_version
from exceptions import VersionMismatchError, AuthenticationRequired
import settings
from auth import checkToken, checkTickets, RenewTicketsTimer
from multiprocessing import Event
import sys, os, fcntl, signal
from upgrade import upgrade
import logging
logger = logging.getLogger(__name__)

__all__ = ('exported', 'assetCompatable', 'assertAuthenticated', 'runserver')

def exported(obj):
    obj._export = True
    return obj

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

    logger.info("Starting server: '%s'", server_class.__name__)

    logger.debug("Creating lock file.")
    lck_fd = None
    try:
        lck_fd = os.open(settings.LOCK_FILE, os.O_WRONLY|os.O_CREAT|os.O_EXCL, 0600)
    except (IOError, OSError):
        lck_fd = None # Just in case.
        logger.error("Failed to create lock.")
        exit(1)

    # We are now sure that no other server is running and can remove lock files
    # etc. on cleanup

    try:
        logger.debug("Locking lock file.")
        try:
            fcntl.flock(lck_fd, fcntl.LOCK_EX|fcntl.LOCK_NB)
        except:
            logger.error("Failed to lock lock.")
            exit(1)

        logger.debug("Upgrading configs.")
        try:
            upgrade()
        except:
            logger.error("Failed to upgrade server config.")
            exit(1)

        if dofork:
            logger.debug("Forking.")
            fork(server_started)

        ### Child stuff. ###

        logger.debug("Writing pid to lock file.")
        try:
            os.write(lck_fd, str(os.getpid()))
        except (IOError, OSError):
            logger.error("Failed to write PID to lock.")
            exit(1)

        # Initialize the server *after* forking.
        logger.debug("Initializing server.")
        server = server_class(**args)

        # Get info
        logger.debug("Writing info file.")
        info_contents = {"VERSION": server_version}
        info_contents.update(server.getInfo())
        
        try:
            with open(settings.INFO_FILE, "w+", 0600) as info_file:
                info_file.writelines("%s='%s'\n" % i for i in info_contents.iteritems())
        except IOError:
            logger.error("Failed to write info file.")
            exit(1)

        server_started.set()

        logger.debug("Starting renewal ticket timer.")
        ticket_timer = RenewTicketsTimer(server.shutdown)
        try:
            ticket_timer.start()
            logger.debug("Starting server loop.")
            server.serve_forever()
            logger.error("Server died for no reason.")
        except KeyboardInterrupt:
            logger.info("Interrupted. Exiting.")
        except SystemExit as e:
            logger.info("Exiting with status %d" % e.code)
            raise e # We want to make sure to preserve the exit code
        except Exception as e:
            logging.exception("Unhandled exception.")
        finally:
            ticket_timer.stop()
    finally:
        if lck_fd is not None:
            try:
                os.close(lck_fd)
            except (OSError, IOError):
                pass
        if os.path.exists(settings.INFO_FILE):
            logger.debug("Removing info file.")
            os.remove(settings.INFO_FILE)
        if os.path.exists(settings.LOCK_FILE):
            logger.debug("Removing lock file.")
            os.remove(settings.LOCK_FILE)

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
                    logger.error("Child killed. Grandchild should be dead.")
                    ret = -os.WTERMSIG(status)
                else:
                    if os.WIFEXITED(status):
                        ret = os.WEXITSTATUS(status)
                    else:
                        logger.error("WTF. The child didn't exit normally and wasn't killed.")
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
                            logger.error("Child killed. Grandchild should be dead.")
                            ret = -os.WTERMSIG(status)
                        else:
                            if os.WIFEXITED(status):
                                ret = os.WEXITSTATUS(status)
                            else:
                                logger.error("WTF. The child didn't exit normally and wasn't killed.")
                                ret = -1
                else:
                    logger.error("Timed out waiting for grandchild. Killing grandchild.")
                    os.kill(pid, 15)
                    ret = 1
            except BaseException:
                logger.error("Exception in child. Killing grandchild.")
                os.kill(pid, 15)
                ret = 1
            finally:
                os._exit(ret)
        else:
            # grandchild.
            sys.stdin.close()
            sys.stdout = open(settings.LOG_FILE, 'a')
            sys.stderr = open(settings.LOG_FILE, 'a')
