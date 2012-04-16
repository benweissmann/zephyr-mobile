from functools import wraps
from . import VERSION as server_version
from exceptions import VersionMismatchError, AuthenticationRequired
from subprocess import call, Popen, PIPE
from settings import AUTH_TIMEOUT
from time import time
from uuid import uuid4 as uuid
from settings import INFO_FILE
import os

TOKENS = {}

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

def checkTickets():
    return call(["klist","-s"], stdout=open(os.devnull, "w"), stderr=open(os.devnull, "w")) == 0

def checkToken(token):
    try:
        return (time() - TOKENS[token]) <= AUTH_TIMEOUT
    except KeyError:
        return False

def makeToken():
    now = time()
    # Clean up old tokens
    for token, timestamp in TOKENS.iteritems():
        if now-timestamp > AUTH_TIMEOUT:
            del TOKENS[token]

    # Create the new one
    token = str(uuid())
    TOKENS[token] = now
    return token

def getTickets(username, password):
    p = Popen([
        "kinit",
        "-F",
        "-l7d",
        username
    ], stdin=PIPE, stdout=open(os.devnull, "w"), stderr=open(os.devnull, "w"))
    p.communicate(password)
    return p.wait() == 0

def authenticate(username, password):
    if getTickets(username, password):
        return makeToken()
    else:
        raise AuthenticationRequired("Invalid credentials")

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
    
    try:
        fobj = os.fdopen(fd, "w")
        fobj.writelines("%s='%s'\n" % i for i in info_contents.iteritems())
        fobj.flush()
        fcntl.flock(fd, fcntl.LOCK_SH|fcntl.LOCK_NB)
        server.serve_forever()
    finally:
        fobj.close()
        os.remove(INFO_FILE)

