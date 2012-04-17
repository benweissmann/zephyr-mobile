package com.benweissmann.zmobile.setup;

public interface ZServCallback {
    public void run(ZServ zServ);
    public void onError(ZServException e);
}
