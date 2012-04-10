import os
import pwd
import json

__all__ = ("ZEPHYR_DB", "CONFIG_FILE", "ZSUBS", "preferences")

CONFIG_FILE = os.path.join(os.environ.get("XDG_CONFIG_HOME", os.path.expandvars("$HOME/.config")), "zephyr-server.ini")
ZEPHYR_DB = os.path.join(os.environ.get("XDG_DATA_HOME", os.path.expandvars("$HOME/.local/share")), "zephyr-server/zephyrs.db")
ZSUBS = os.path.join(os.environ.get("HOME"), ".zephyr.subs")


class Preferences(dict):
    DEFAULTS = {
        "signature": pwd.getpwuid(os.getuid()).pw_gecos.split(',', 1)[0],
        "starred-classes": [],
        "hidden-classes": ["message"]
    }
    _obj = None
    def __init__(self, config_file):
        self.config_file = config_file
        self.load()
        Preferences._obj = self

    def save(self):
        if not os.path.exists(self.config_file):
            directory = os.path.dirname(self.config_file)
            if not os.path.isdir(directory):
                os.makedirs(directory)

        with open(self.config_file, "w") as f:
            json.dump(self, f, indent=4)

    def load(self):
        self.clear()
        self.update(self.DEFAULTS)
        try:
            with open(self.config_file) as f:
                self.update(json.load(f))
        except IOError:
            self.save()

    def __setitem__(self, item, value):
        super(self, Preferences).__setitem__(self, item, value)
        self.save()

preferences = Preferences(CONFIG_FILE)
