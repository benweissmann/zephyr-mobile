from server import exported
import pwd, os
from . import zephyr

__all__ = ("Preferences", "setVariable", "getVariable")

class Preferences(object):
    """
    External preferences interface.
    Using settings directly is more efficient.
    """
    # Hidden classes

    @exported
    def hideClass(self, cls):
        hiddenClasses = getVariable("hidden-classes")
        hiddenClasses.add(cls)
        setVariable("hidden-classes", hiddenClasses)
        return True

    @exported
    def unhideClass(self, cls):
        hiddenClasses = getVariable("hidden-classes")
        try:
            hiddenClasses.remove(cls)
        except KeyError:
            return False
        setVariable("hidden-classes", hiddenClasses)
        return True

    @exported
    def getHiddenClasses(self):
        return getVariable("hidden-classes")

    @exported
    def setHiddenClasses(self, classes):
        setVariable("hidden-classes", classes)
        return True

    # Starred Classes

    @exported
    def starClass(self, cls):
        starredClasses = getVariable("starred-classes")
        starredClasses.add(cls)
        setVariable("starred-classes", starredClasses)
        return True

    @exported
    def unstarClass(self, cls):
        starredClasses = getVariable("starred-classes")
        try:
            starredClasses.remove(cls)
        except KeyError:
            return False
        setVariable("starred-classes", starredClasses)
        return True

    @exported
    def getStarredClasses(self):
        return getVariable("starred-classes")

    @exported
    def setStarredClasses(self, classes):
        setVariable("starred-classes", classes)
        return True

    # Signature

    @exported
    def getSignature(self):
        return getVariable("signature")

    @exported
    def setSignature(self, sig):
        setVariable("signature", sig)
        return True


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
