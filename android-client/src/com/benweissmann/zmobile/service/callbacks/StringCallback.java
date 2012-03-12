package com.benweissmann.zmobile.service.callbacks;

public interface StringCallback {
    public void run(String s);
    
    public void onError(Throwable e);
}
