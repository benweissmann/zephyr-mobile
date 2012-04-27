package com.benweissmann.zmobile.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.net.Uri;

public class URIs {
    private static final Pattern URL_PATTERN =
            Pattern.compile("\\(?\\bhttps?://[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]");
    
    /**
     * Returns an intent that will open the given url.
     */
    public static Intent intentFor(String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }
    
    /**
     * Returns a list of urls contained in the given text
     */
    public static List<String> extractUrls(String text) {
        List<String> matches = new ArrayList<String>();
        Matcher matcher = URL_PATTERN.matcher(text);
        
        while(matcher.find()) {
            String s = matcher.group();
            if(s.startsWith("(") && s.endsWith(")")) {
                s = s.substring(1, s.length()-1);
            }
            matches.add(s);
        }
        
        return matches;
    }
}
