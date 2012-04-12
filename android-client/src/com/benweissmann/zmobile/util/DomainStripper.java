package com.benweissmann.zmobile.util;

import com.benweissmann.zmobile.service.ZephyrService;

public class DomainStripper {
    public static String stripDomain(String unstripped) {
        String[] senderParts = unstripped.split("@");
        
        if(senderParts.length != 2) {
            return unstripped;
        }
        
        if(senderParts[1].equalsIgnoreCase(ZephyrService.HOME_DOMAIN)) {
            return senderParts[0];
        }
        
        return unstripped;
    }
}
