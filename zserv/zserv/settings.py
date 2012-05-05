import os, logging

DATA_DIR = os.path.join(os.environ.get("XDG_DATA_HOME", os.path.expandvars("$HOME/.local/share")), "zephyr-server")
HOME = os.environ.get("HOME")

ZEPHYR_DB = os.path.join(DATA_DIR, "zephyrs.db")
INFO_FILE = os.path.join(DATA_DIR, "info")
LOCK_FILE = os.path.join(DATA_DIR, "lock")
ZSUBS = os.path.join(HOME, ".zephyr.subs")
ZVARS = os.path.join(HOME, ".zephyr.vars")
AUTH_TIMEOUT = 86400 # 1 day between authentications
VERSION_FILE = os.path.join(DATA_DIR, "version")

LOG_LEVEL = logging.INFO
LOG_FILE = os.path.join(DATA_DIR, "server.log")
LOG_FORMAT = "%(asctime)s  %(levelname)s:%(name)s: %(message)s"
LOG_DATE_FORMAT = "%b %d %Y %T"
