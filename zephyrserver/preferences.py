from common import exported
import settings


class Preferences(object):
    """
    External preferences interface.
    Using settings directly is more efficient.
    """
    # Hidden classes

    @exported
    def hideClass(self, cls):
        hiddenClasses = settings.get("hidden-classes")
        hiddenClasses.add(cls)
        settings.set("hidden-classes", hiddenClasses)
        return True

    @exported
    def unhideClass(self, cls):
        hiddenClasses = settings.get("hidden-classes")
        try:
            hiddenClasses.remove(cls)
        except KeyError:
            return False
        settings.set("hidden-classes", hiddenClasses)
        return True

    @exported
    def getHiddenClasses(self):
        return settings.get("hidden-classes")

    @exported
    def setHiddenClasses(self, classes):
        settings.set("hidden-classes", classes)
        return True

    # Starred Classes

    @exported
    def starClass(self, cls):
        starredClasses = settings.get("starred-classes")
        starredClasses.add(cls)
        settings.set("starred-classes", starredClasses)
        return True

    @exported
    def unstarClass(self, cls):
        starredClasses = settings.get("starred-classes")
        try:
            starredClasses.remove(cls)
        except KeyError:
            return False
        settings.set("starred-classes", starredClasses)
        return True

    @exported
    def getStarredClasses(self):
        return settings.get("starred-classes")

    @exported
    def setStarredClasses(self, classes):
        settings.set("starred-classes", classes)
        return True

    # Signature

    @exported
    def getSignature(self):
        return settings.get("signature")

    @exported
    def setSignature(self, sig):
        settings.set("signature", sig)
        return True
