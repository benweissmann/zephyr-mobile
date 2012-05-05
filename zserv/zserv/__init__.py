import logging
import settings

__all__ = ("VERSION", "zephyr")

logging.basicConfig(
    filename=settings.LOG_FILE,
    level=settings.LOG_LEVEL,
    format=settings.LOG_FORMAT,
    datefmt=settings.LOG_DATE_FORMAT
)

VERSION = 1

try:
    import zephyr
except ImportError:
    import test_zephyr as zephyr
    logging.info("Failed to import zephyr, using test zephyr.")



