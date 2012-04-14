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
        hiddenClasses = settings.getVariable("hidden-classes")
        hiddenClasses.add(cls)
        settings.setVariable("hidden-classes", hiddenClasses)
        return True

    @exported
    def unhideClass(self, cls):
        hiddenClasses = settings.getVariable("hidden-classes")
        try:
            hiddenClasses.remove(cls)
        except KeyError:
            return False
        settings.setVariable("hidden-classes", hiddenClasses)
        return True

    @exported
    def getHiddenClasses(self):
        return settings.getVariable("hidden-classes")

    @exported
    def setHiddenClasses(self, classes):
        settings.setVariable("hidden-classes", classes)
        return True

    # Starred Classes

    @exported
    def starClass(self, cls):
        starredClasses = settings.getVariable("starred-classes")
        starredClasses.add(cls)
        settings.setVariable("starred-classes", starredClasses)
        return True

    @exported
    def unstarClass(self, cls):
        starredClasses = settings.getVariable("starred-classes")
        try:
            starredClasses.remove(cls)
        except KeyError:
            return False
        settings.setVariable("starred-classes", starredClasses)
        return True

    @exported
    def getStarredClasses(self):
        return settings.getVariable("starred-classes")

    @exported
    def setStarredClasses(self, classes):
        settings.setVariable("starred-classes", classes)
        return True

    # Signature

    @exported
    def getSignature(self):
        return settings.getVariable("signature")

    @exported
    def setSignature(self, sig):
        settings.setVariable("signature", sig)
        return True
