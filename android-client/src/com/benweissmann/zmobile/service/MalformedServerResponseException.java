package com.benweissmann.zmobile.service;

public class MalformedServerResponseException extends Exception {
    private static final long serialVersionUID = 1L;
    private String msg;
    private Throwable cause;

    public MalformedServerResponseException(String msg, Throwable cause) {
        this.msg = msg;
        this.cause = cause;
    }
    
    public MalformedServerResponseException(String msg) {
        this(msg, null);
    }
    
    public MalformedServerResponseException(Throwable cause) {
        this((cause==null ? null : cause.toString()), cause);
    }

    @Override
    public String getMessage() {
        return msg;
    }
    
    @Override
    public Throwable getCause() {
        return cause;
    }
}
