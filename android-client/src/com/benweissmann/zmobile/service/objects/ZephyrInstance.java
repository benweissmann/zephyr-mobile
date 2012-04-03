package com.benweissmann.zmobile.service.objects;

import java.io.Serializable;

/**
 * Immutable class that represent a Zephyr instance
 * @author Ben Weissmann <bsw@mit.edu>
 */
public final class ZephyrInstance implements Serializable, ZephyrgramSet {
    private static final long serialVersionUID = 1L;
    private final String cls;
    private final String name;
    private final int unreadCount;
    private final int totalCount;
    
    public ZephyrInstance(String cls, String name, int unreadCount, int totalCount) {
        this.cls = cls;
        this.name = name;
        this.unreadCount = unreadCount;
        this.totalCount = totalCount;
    }
    
    public Query getQuery() {
        return (new Query()).cls(this.cls).instance(this);
    }
    
    public String getCls() {
        return cls;
    }
    
    public String getName() {
        return name;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public int getTotalCount() {
        return totalCount;
    }

    @Override
    public String toString() {
        return "ZephyrInstance [cls=" + cls + ", name=" + name
                + ", unreadCount=" + unreadCount + ", totalCount=" + totalCount
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cls == null) ? 0 : cls.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + totalCount;
        result = prime * result + unreadCount;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ZephyrInstance other = (ZephyrInstance) obj;
        if (cls == null) {
            if (other.cls != null)
                return false;
        }
        else if (!cls.equals(other.cls))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        if (totalCount != other.totalCount)
            return false;
        if (unreadCount != other.unreadCount)
            return false;
        return true;
    }
}
