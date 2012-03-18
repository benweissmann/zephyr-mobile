package com.benweissmann.zmobile.service.objects;

//TODO: toString, equals, hashCode

/**
 * Immutable class that represents a Zephyr class.
 * 
 * @author Ben Weissmann <bsw@mit.edu>
 */
public final class ZephyrClass {
    private final String name;
    private final int unreadCount;
    private final int readCount;

    public ZephyrClass(String name, int unreadCount, int readCount) {
        this.name = name;
        this.unreadCount = unreadCount;
        this.readCount = readCount;
    }

    /**
     * Returns a Query that can be used to fetch all zephyrgrams in this class.
     */
    public Query getQuery() {
        return (new Query()).cls(this);
    }

    public String getName() {
        return name;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public int getReadCount() {
        return readCount;
    }

    @Override
    public String toString() {
        return "ZephyrClass [name=" + name + ", unreadCount=" + unreadCount
                + ", readCount=" + readCount + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + readCount;
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
        if (readCount != other.readCount)
            return false;
        if (unreadCount != other.unreadCount)
            return false;
        return true;
    }
}
