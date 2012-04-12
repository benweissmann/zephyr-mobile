class VersionMismatchError(Exception):
    """
    Raised if the server version is less than the minimum requested by the
    client.
    """
    def __init__(self, server, client):
        super(VersionMismatchError, self).__init__(
            "Server version '%d' is less than the minimum requested server version '%d'." % (server, client)
        )

class InvalidAuthenticationToken(Exception):
    """
    Raised if the client is not properly authenticated with the server.
    """

class AuthenticationRequired(Exception):
    """
    Raised if the clients authentication token has expired.
    """


