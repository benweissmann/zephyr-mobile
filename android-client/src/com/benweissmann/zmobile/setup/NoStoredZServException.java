package com.benweissmann.zmobile.setup;

public class NoStoredZServException extends Exception {
    private static final long serialVersionUID = 1L;

    public NoStoredZServException() {
        super();
    }

    public NoStoredZServException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NoStoredZServException(String detailMessage) {
        super(detailMessage);
    }

    public NoStoredZServException(Throwable throwable) {
        super(throwable);
    }
}
