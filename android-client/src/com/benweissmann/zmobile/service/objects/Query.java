package com.benweissmann.zmobile.service.objects;

/**
 * Immutable class to represent a query that can be sent to the server and
 * return a ZephyrResultSet.
 * 
 * Construction of this class is based loosely on the builder pattern, e.g.:
 * (new Query()).cls("foo-class").instance("bar-instance").text("baz"); returns
 * a query that will return Zephyrgrams that have the class "foo-class", the
 * instance "bar-instance", and contain the text "baz". Properties that haven't
 * been set are set to null.
 * 
 * @author Ben Weissmann <bsw@mit.edu>
 */
public final class Query implements IQuery {
    private static final long serialVersionUID = 1L;
    private final String cls;
    private final String instance;
    private final String text;
    private final String sender;
    private final String user;

    public Query() {
        this.cls = null;
        this.instance = null;
        this.text = null;
        this.sender = null;
        this.user = null;
    }

    public Query(String cls, String instance, String text, String sender, String user) {
        this.cls = cls;
        this.instance = instance;
        this.text = text;
        this.sender = sender;
        this.user = user;
    }
    
    public Query[] queryArray() {
        return new Query[]{this};
    }

    public Query cls(String cls) {
        return new Query(cls, this.instance, this.text, this.sender, this.user);
    }

    public Query cls(ZephyrClass cls) {
        return this.cls(cls.getName());
    }

    public Query instance(String instance) {
        return new Query(this.cls, instance, this.text, this.sender, this.user);
    }

    public Query instance(ZephyrInstance instance) {
        return this.instance(instance.getName());
    }

    public Query text(String text) {
        return new Query(this.cls, this.instance, text, this.sender, this.user);
    }

    public Query sender(String sender) {
        return new Query(this.cls, this.instance, this.text, sender, this.user);
    }
    
    public Query user(String user) {
        return new Query(this.cls, this.instance, this.text, this.sender, user);
    }

    public String getCls() {
        return cls;
    }

    public String getInstance() {
        return instance;
    }

    public String getText() {
        return text;
    }

    public String getSender() {
        return sender;
    }
    
    public String getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "Query [cls=" + cls + ", instance=" + instance + ", text="
               + text + ", sender=" + sender + ", user=" + user + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cls == null) ? 0 : cls.hashCode());
        result = prime * result
                 + ((instance == null) ? 0 : instance.hashCode());
        result = prime * result + ((sender == null) ? 0 : sender.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
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
        Query other = (Query) obj;
        if (cls == null) {
            if (other.cls != null)
                return false;
        }
        else if (!cls.equals(other.cls))
            return false;
        if (instance == null) {
            if (other.instance != null)
                return false;
        }
        else if (!instance.equals(other.instance))
            return false;
        if (sender == null) {
            if (other.sender != null)
                return false;
        }
        else if (!sender.equals(other.sender))
            return false;
        if (text == null) {
            if (other.text != null)
                return false;
        }
        else if (!text.equals(other.text))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        }
        else if (!user.equals(other.user))
            return false;
        return true;
    }
}
