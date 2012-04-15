package com.benweissmann.zmobile.service.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Immutable class representing a set of Zephyrgrams returned by the server.
 * @author Ben Weissmann <bsw@mit.edu>
 */
public final class ZephyrgramResultSet implements Iterable<Zephyrgram> {
    private final IQuery query;
    private final String filterId;
    private final int offset;
    final List<Zephyrgram> zephyrgrams;
    
    /**
     * Creates a new ZephyrgramResultSet
     * @param query        The Query used to get this ResultSet
     * @param filterId     The id of the server-side filter
     * @param page         The page number of this result set
     * @param resultLength The number of results for the query, ignoring pagination
     * @param zephyrgrams  A list of Zephyrgrams on this page of the results
     */
    public ZephyrgramResultSet(IQuery query, String filterId, int offset, List<Zephyrgram> zephyrgrams) {
        this.query = query;
        this.filterId = filterId;
        this.offset = offset;
        this.zephyrgrams = new ArrayList<Zephyrgram>(zephyrgrams);
    }
    
    /**
     * Returns this page's offset (the index of its first element)
     */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Returns the query used to request these results. We use this when refreshing
     * these results.
     */
    public IQuery getQuery() {
        return query;
    }
    
    /**
     * Returns the ID of the server-side filter we can use to get other pages
     * of these results
     */
    public String getFilterId() {
        return filterId;
    }

    /**
     * Returns the number of results on this page
     */
    public int getPageLength() {
        return zephyrgrams.size();
    }

    /**
     * Returns this page of Zephyrgrams as an unmodifiable list.
     */
    public List<Zephyrgram> getZephyrgrams() {
        return Collections.unmodifiableList(zephyrgrams);
    }
    
    public Zephyrgram get(int i) {
        return this.zephyrgrams.get(i);
    }
    
    /**
     * Return the iterator over this page of Zephyrgrams.
     */
    public Iterator<Zephyrgram> iterator() {
        return this.getZephyrgrams().iterator();
    }

    @Override
    public String toString() {
        return "ZephyrgramResultSet [query=" + query + ", filterId=" + filterId
                + ", offset=" + offset + ", zephyrgrams=" + zephyrgrams + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((filterId == null) ? 0 : filterId.hashCode());
        result = prime * result + offset;
        result = prime * result + ((query == null) ? 0 : query.hashCode());
        result = prime * result
                + ((zephyrgrams == null) ? 0 : zephyrgrams.hashCode());
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
        ZephyrgramResultSet other = (ZephyrgramResultSet) obj;
        if (filterId == null) {
            if (other.filterId != null)
                return false;
        }
        else if (!filterId.equals(other.filterId))
            return false;
        if (offset != other.offset)
            return false;
        if (query == null) {
            if (other.query != null)
                return false;
        }
        else if (!query.equals(other.query))
            return false;
        if (zephyrgrams == null) {
            if (other.zephyrgrams != null)
                return false;
        }
        else if (!zephyrgrams.equals(other.zephyrgrams))
            return false;
        return true;
    }
}
