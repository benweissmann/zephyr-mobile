package com.benweissmann.zmobile.service.callbacks;

public interface ZephyrStatusCallback {
    public void onSuccess();
    public void onFailure();
    public void onError(Exception e);
}
