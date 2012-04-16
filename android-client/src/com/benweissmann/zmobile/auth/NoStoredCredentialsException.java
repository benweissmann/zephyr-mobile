package com.benweissmann.zmobile.auth;

public class NoStoredCredentialsException extends Exception {
    private static final long serialVersionUID = 1L;

    public NoStoredCredentialsException() {
        super();
    }

    public NoStoredCredentialsException(String detailMessage,
                                        Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NoStoredCredentialsException(String detailMessage) {
        super(detailMessage);
    }

    public NoStoredCredentialsException(Throwable throwable) {
        super(throwable);
    }
    
}
