package com.benweissmann.zmobile.service.objects;

import java.util.Date;

//TODO: toString, equals, hashCode

/**
 * Immutable class to represent a Zephyrgram.
 * 
 * @author Ben Weissmann <bsw@mit.edu>
 */
public final class Zephyrgram {
    private final String cls;
    private final String instance;
    private final String sender;
    private final Date timestamp;
    private final boolean read;
    private final String user;
    private final String body;

    public Zephyrgram(String cls, String instance, String sender,
            Date timestamp, boolean read, String user, String body) {
        this.cls = cls;
        this.instance = instance;
        this.sender = sender;
        this.timestamp = (Date) timestamp.clone();
        this.read = read;
        this.user = user;
        this.body = body;
    }

    public Zephyrgram(String cls, String instance, String sender,
            Date timestamp, boolean read, String body) {
        this(cls, instance, sender, timestamp, read, null, body);
    }

    public Zephyrgram(String cls, String instance, String sender, String body) {
        this(cls, instance, sender, new Date(), false, body);
    }

    public Zephyrgram(String sender, Date timestamp, boolean read, String user,
            String body) {
        this("message", "personal", sender, timestamp, read, user, body);
    }

    public Zephyrgram(String sender, String user, String body) {
        this(sender, new Date(), false, user, body);
    }

    public String getCls() {
        return cls;
    }

    public String getInstance() {
        return instance;
    }

    public String getSender() {
        return sender;
    }

    public Date getTimestamp() {
        return (Date) timestamp.clone();
    }

    public boolean isRead() {
        return read;
    }

    public String getUser() {
        return user;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "Zephyrgram [cls=" + cls + ", instance=" + instance
                + ", sender=" + sender + ", timestamp=" + timestamp + ", read="
                + read + ", user=" + user + ", body=\"" + body + "\"]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((body == null) ? 0 : body.hashCode());
        result = prime * result + ((cls == null) ? 0 : cls.hashCode());
        result = prime * result
                + ((instance == null) ? 0 : instance.hashCode());
        result = prime * result + (read ? 1231 : 1237);
        result = prime * result + ((sender == null) ? 0 : sender.hashCode());
        result = prime * result
                + ((timestamp == null) ? 0 : timestamp.hashCode());
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
        Zephyrgram other = (Zephyrgram) obj;
        if (body == null) {
            if (other.body != null)
                return false;
        }
        else if (!body.equals(other.body))
            return false;
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
        if (read != other.read)
            return false;
        if (sender == null) {
            if (other.sender != null)
                return false;
        }
        else if (!sender.equals(other.sender))
            return false;
        if (timestamp == null) {
            if (other.timestamp != null)
                return false;
        }
        else if (!timestamp.equals(other.timestamp))
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
