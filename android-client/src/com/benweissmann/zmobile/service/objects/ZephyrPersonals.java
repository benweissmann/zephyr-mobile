package com.benweissmann.zmobile.service.objects;

import java.io.Serializable;

/**
 * An immutable class representing a set of personals from a particular
 * user.
 */
public final class ZephyrPersonals implements Serializable, ZephyrgramSet {
    private static final long serialVersionUID = 1L;
    private final String sender;
    private final int unreadCount;
    private final int totalCount;
    
    public ZephyrPersonals(String sender, int unreadCount, int totalCount) {
        this.sender = sender;
        this.unreadCount = unreadCount;
        this.totalCount = totalCount;
    }
    
    public Query getQuery() {
        return new Query().cls(Zephyrgram.PERSONALS_CLASS).sender(sender);
    }
    
    public String getName() {
        return sender;
    }

    public String getSender() {
        return sender;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sender == null) ? 0 : sender.hashCode());
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
        ZephyrPersonals other = (ZephyrPersonals) obj;
        if (sender == null) {
            if (other.sender != null)
                return false;
        }
        else if (!sender.equals(other.sender))
            return false;
        if (totalCount != other.totalCount)
            return false;
        if (unreadCount != other.unreadCount)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ZephyrPersonals [sender=" + sender + ", unreadCount="
               + unreadCount + ", totalCount=" + totalCount + "]";
    }
}
