package com.benweissmann.zmobile.service.callbacks;

import com.benweissmann.zmobile.service.ZephyrService.ZephyrBinder;

public interface BinderCallback {
    /**
     * Callback. Given a binder. MUST call onComplete.run() when finished
     * with the binder.
     */
    public void run(ZephyrBinder binder, Runnable onComplete);
}
