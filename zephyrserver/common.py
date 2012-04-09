from functools import wraps

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
