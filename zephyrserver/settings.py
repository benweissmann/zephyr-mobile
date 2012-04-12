import os
import pwd
try:
    import zephyr
except:
    import test_zephyr as zephyr

__all__ = ("ZEPHYR_DB", "ZSUBS", "getVariable", "setVariable")

CONFIG_FILE = os.path.join(os.environ.get("XDG_CONFIG_HOME", os.path.expandvars("$HOME/.config")), "zephyr-server.ini")
ZEPHYR_DB = os.path.join(os.environ.get("XDG_DATA_HOME", os.path.expandvars("$HOME/.local/share")), "zephyr-server/zephyrs.db")
ZSUBS = os.path.join(os.environ.get("HOME"), ".zephyr.subs")


def string_to_set(value):
    return set(value.split(','))

def set_to_string(value):
    return ",".join(value)

DEFAULTS = {
    "signature": pwd.getpwuid(os.getuid()).pw_gecos.split(',', 1)[0],
    "starred-classes": set(),
    "hidden-classes": set()
}
TRANSFORMS = {
    "hidden-classes": (string_to_set, set_to_string),
    "starred-classes": (string_to_set, set_to_string)
}

def getVariable(var, default=None):
    value = zephyr.getVariable(var)
    if value is None:
        return default if default is not None else DEFAULTS.get(var, None)
    if var in TRANSFORMS:
        value = TRANSFORMS[var][0](value)
    return value

def setVariable(var, value):
    if var in TRANSFORMS:
        value = TRANSFORMS[var][1](value)
    zephyr.setVariable(var, str(value))
