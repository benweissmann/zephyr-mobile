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
