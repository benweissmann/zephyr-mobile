from subprocess import call, Popen, PIPE
import os, pwd
from exceptions import AuthenticationRequired
from settings import AUTH_TIMEOUT, DATA_DIR, HOME
from time import time, sleep
from uuid import uuid4 as uuid
from threading import Thread, Event
import logging
logger = logging.getLogger(__name__)

PATHS = [DATA_DIR, HOME]

TOKENS = {}
RENEW_TIMEOUT = 3600
TICKET_TIME = "7d"

nop = lambda *_:None
USER = pwd.getpwuid(os.geteuid()).pw_name # os.getlogin fails on linerva

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
        "-l"+TICKET_TIME,
        "-r"+TICKET_TIME,
        username
    ], stdin=PIPE, stdout=open(os.devnull, "w"), stderr=open(os.devnull, "w"))
    p.communicate(password)
    return p.wait() == 0

def renewTickets():
    return call(["kinit", "-R"], stdout=open(os.devnull, "w"), stderr=open(os.devnull, "w")) == 0

def refreshAFS():
    # If i am not on afs, don't fail.
    try:
        ret = call(["aklog"] + PATHS, stdout=open(os.devnull, "w"), stderr=open(os.devnull, "w"))
        if ret: logger.error("aklog failed: reason %d" % ret)
    except OSError:
        pass

def authenticate(username, password):
    import pam
    # This will fetch tickets twice but...
    if not pam.authenticate(USER, password, "sshd"):
        raise AuthenticationRequired("Invalid credentials.")

    if not getTickets(username, password):
        raise AuthenticationRequired("Unable to get tickets.")

    return makeToken()

class RenewTicketsTimer(Thread):
    def __init__(self, on_auth_expire=nop, time=RENEW_TIMEOUT):
        super(RenewTicketsTimer, self).__init__()
        self.timeout = time
        self.event = Event()
        self.on_auth_expire = on_auth_expire

    def run(self):
        while not self.event.is_set():
            if renewTickets():
                sleep(1) # Give the tickets a second.
                refreshAFS()
            else:
                self.on_auth_expire()
            self.event.wait(self.timeout)

    def stop(self):
        self.event.set()

