import os
import pwd

CONFIG_FILE = os.path.join(os.environ.get("XDG_CONFIG_HOME", os.path.expandvars("$HOME/.config")), "zephyr-server.ini")
ZEPHYR_DB = os.path.join(os.environ.get("XDG_DATA_HOME", os.path.expandvars("$HOME/.local/share")), "zephyr-server/zephyrs.db")
ZSUBS = os.path.join(os.environ.get("HOME"), ".zephyr.subs")

signature = pwd.getpwuid(os.getuid()).pw_gecos.split(',', 1)[0]

# XXX: Let user set settings manually (config file loading).
