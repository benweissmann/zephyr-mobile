import os
import logging
import settings

VERSION = 1

__all__ = ("VERSION", "zephyr")

os.umask(077) # Default to private files.

if not os.path.exists(settings.DATA_DIR):
    os.path.makedirs(settings.DATA_DIR, 0700)

if not os.path.isdir(settings.DATA_DIR):
    print "The data directory is not a directory."
    exit(1)

logging.basicConfig(
    filename=settings.LOG_FILE,
    level=settings.LOG_LEVEL,
    format=settings.LOG_FORMAT,
    datefmt=settings.LOG_DATE_FORMAT
)

try:
    import zephyr
except ImportError:
    import test_zephyr as zephyr
    logging.info("Failed to import zephyr, using test zephyr.")



