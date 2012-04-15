package com.benweissmann.zmobile.util;

import com.benweissmann.zmobile.service.objects.IQuery;
import com.benweissmann.zmobile.service.objects.OrQuery;
import com.benweissmann.zmobile.service.objects.Query;
import com.benweissmann.zmobile.service.objects.Zephyrgram;

public class QueryBuilder {
    public static IQuery personalQuery(String user) {
        Query inbound = new Query().cls(Zephyrgram.PERSONALS_CLASS).sender(user);
        Query outbound = new Query().cls(Zephyrgram.PERSONALS_CLASS).user(user);
        Query outboundStripped = new Query().cls(Zephyrgram.PERSONALS_CLASS).
                                             user(DomainStripper.stripDomain(user));
        return OrQuery.of(inbound, outbound, outboundStripped);
    }
    
}
