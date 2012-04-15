package com.benweissmann.zmobile.service.objects;

import java.util.Arrays;

public class OrQuery implements IQuery {
    private static final long serialVersionUID = 1L;
    private Query[] clauses;
    
    private OrQuery(Query[] clauses) {
        this.clauses = clauses;
    }
    
    public static OrQuery of(Query... clauses) {
        return new OrQuery(clauses);
    }
    
    public Query[] queryArray() {
        return this.clauses;
    }

    @Override
    public String toString() {
        return "OrQuery [clauses=" + Arrays.toString(clauses) + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(clauses);
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
        OrQuery other = (OrQuery) obj;
        if (!Arrays.equals(clauses, other.clauses))
            return false;
        return true;
    }
}
