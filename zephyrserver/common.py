from functools import wraps
from . import VERSION as server_version
from exceptions import VersionMismatchError

def return_status(func):
    @wraps(func)
    def do(*args, **kwargs):
        try:
            func(*args, **kwargs)
        except:
            return False
        return True
    return do

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
    pass # XXX: This needs to check the token
