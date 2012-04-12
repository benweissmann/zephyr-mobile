package com.benweissmann.zmobile.service.objects;

import java.io.Serializable;

/**
 * Immutable class that represents a Zephyr class.
 * 
 * @author Ben Weissmann <bsw@mit.edu>
 */
public final class ZephyrClass implements Serializable, ZephyrgramSet {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final int unreadCount;
    private final int totalCount;
    private final boolean starred;

    public ZephyrClass(String name, int unreadCount, int totalCount, boolean starred) {
        this.name = name;
        this.unreadCount = unreadCount;
        this.totalCount = totalCount;
        this.starred = starred;
    }

    /**
     * Returns a Query that can be used to fetch all zephyrgrams in this class.
     */
    public Query getQuery() {
        return (new Query()).cls(this);
    }
    
    /**
     * Returns true iff this class corresponds to personal zephyrs
     */
    public boolean isPersonals() {
        return (this.name.equals(Zephyrgram.PERSONALS_CLASS));
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
    
    public boolean isStarred() {
        return starred;
    }

    @Override
    public String toString() {
        return "ZephyrClass [name=" + name + ", unreadCount=" + unreadCount
               + ", totalCount=" + totalCount + ", starred=" + starred + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (starred ? 1231 : 1237);
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
        ZephyrClass other = (ZephyrClass) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        }
        else if (!name.equals(other.name))
            return false;
        if (starred != other.starred)
            return false;
        if (totalCount != other.totalCount)
            return false;
        if (unreadCount != other.unreadCount)
            return false;
        return true;
    }
}
