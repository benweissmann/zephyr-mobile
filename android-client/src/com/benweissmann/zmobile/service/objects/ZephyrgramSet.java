package com.benweissmann.zmobile.service.objects;

public interface ZephyrgramSet {
    public String getName();
    public int getUnreadCount();
    public int getTotalCount();
    public IQuery getQuery();
}
