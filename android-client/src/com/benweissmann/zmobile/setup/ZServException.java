package com.benweissmann.zmobile.setup;

public class ZServException extends Exception {
    private static final long serialVersionUID = 1L;

    public ZServException() {
        super();
    }

    public ZServException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public ZServException(String detailMessage) {
        super(detailMessage);
    }

    public ZServException(Throwable throwable) {
        super(throwable);
    }
}