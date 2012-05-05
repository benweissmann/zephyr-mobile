from settings import VERSION_FILE
import logging
import os
from exceptions import UpgradeError
from . import VERSION as new_version
logger = logging.getLogger(__name__)

__all__ = ("upgrade",)

def upgrade():
    if os.path.exists(VERSION_FILE):
        try:
            with open(VERSION_FILE, "r") as vfile:
                old_version = int(vfile.readline())
        except IOError:
            raise UpgradeError("Couldn't read version file.")
        except ValueError:
            raise UpgradeError("Invalid version file format.")
    else:
        logger.info("No version file found. Assuming initial install.")
        write_version(new_version)
        return False

    if new_version < old_version:
        raise UpgradeError("Server version too old.")

    for version in xrange(old_version+1, new_version+1):
        Upgrader.upgrade(version)

    return True
        
def write_version(version):
    try:
        with open(VERSION_FILE, "w") as vfile:
            vfile.write(str(version))
    except IOError:
        raise UpgradeError("Failed to write version file.")

nop = lambda *_:None

class Upgrader(object):
    __slots__ = ()
    def __new__(*args):
        raise TypeError("Can't instantiate class.")

    @classmethod
    def upgrade(cls, version):
        ret = getattr(cls, "v%d" % version, nop)()
        write_version(version)
        return ret

    @staticmethod
    def v2():
        print "Upgraded to version 2."
