package com.benweissmann.zmobile.service.callbacks;

public interface ZephyrCallback<T> {
    public void run(T s);
    
    public void onError(Exception e);
}
