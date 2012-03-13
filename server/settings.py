import os

CONFIG_FILE = os.path.join(os.environ.get("XDG_CONFIG_HOME", os.path.expandvars("$HOME/.config")), "zephyr-server.ini")
DATA_DIR = os.path.join(os.environ.get("XDG_DATA_HOME", os.path.expandvars("$HOME/.local/share")), "zephyr-server")
ZEPHYR_DB = os.path.join(DATA_DIR, "zephyrs.db")
