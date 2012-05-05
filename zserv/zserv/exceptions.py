from xmlrpclib import Fault

class BaseZServError(Fault):
    ERRNO = 1
    def __init__(self, message=""):
        super(BaseZServError, self).__init__(self.ERRNO, message)


class VersionMismatchError(BaseZServError):
    """
    Raised if the server version is less than the minimum requested by the
    client.
    """
    ERRNO = 2
    def __init__(self, server, client):
        super(VersionMismatchError, self).__init__(
            "Server version '%d' is less than the minimum requested server version '%d'." % (server, client),
        )

class AuthenticationRequired(BaseZServError):
    """
    Raised if the clients authentication token has expired.
    """
    ERRNO = 3

class ServerKilled(BaseZServError):
    ERRNO = 4

class UpgradeError(BaseZServError):
    ERRNO = 5
