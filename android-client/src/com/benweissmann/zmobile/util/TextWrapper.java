package com.benweissmann.zmobile.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Text wrapping algorithms from Apache Commons
 */
public class TextWrapper {
    private static final int DEFAULT_LINE_LENGTH = 70;
    
    public static String wrap(String text) {
        return wrap(text, DEFAULT_LINE_LENGTH);
    }
    
    /**
     * <p>Wraps a single line of text, identifying words by <code>' '</code>.</p>
     * 
     * <p>New lines will be separated by the system property line separator.
     * Very long words, such as URLs will <i>not</i> be wrapped.</p>
     * 
     * <p>Leading spaces on a new line are stripped.
     * Trailing spaces are not stripped.</p>
     *
     * <pre>
     * WordUtils.wrap(null, *) = null
     * WordUtils.wrap("", *) = ""
     * </pre>
     *
     * @param str  the String to be word wrapped, may be null
     * @param wrapLength  the column to wrap the words at, less than 1 is treated as 1
     * @return a line with newlines inserted, <code>null</code> if null input
     */
    public static String wrap(String str, int wrapLength) {
        return wrap(str, wrapLength, null, false);
    }
    
    /**
     * <p>Wraps a single line of text, identifying words by <code>' '</code>.</p>
     * 
     * <p>Leading spaces on a new line are stripped.
     * Trailing spaces are not stripped.</p>
     * 
     * <pre>
     * WordUtils.wrap(null, *, *, *) = null
     * WordUtils.wrap("", *, *, *) = ""
     * </pre>
     *
     * @param str  the String to be word wrapped, may be null
     * @param wrapLength  the column to wrap the words at, less than 1 is treated as 1
     * @param newLineStr  the string to insert for a new line, 
     *  <code>null</code> uses the system property line separator
     * @param wrapLongWords  true if long words (such as URLs) should be wrapped
     * @return a line with newlines inserted, <code>null</code> if null input
     */
    public static String wrap(String str, int wrapLength, String newLineStr, boolean wrapLongWords) {
        if (str == null) {
            return null;
        }
        if (newLineStr == null) {
            newLineStr = "\n";
        }
        if (wrapLength < 1) {
            wrapLength = 1;
        }
        int inputLineLength = str.length();
        int offset = 0;
        StringBuilder wrappedLine = new StringBuilder(inputLineLength + 32);
        
        while (inputLineLength - offset > wrapLength) {
            if (str.charAt(offset) == ' ') {
                offset++;
                continue;
            }
            int spaceToWrapAt = str.lastIndexOf(' ', wrapLength + offset);

            if (spaceToWrapAt >= offset) {
                // normal case
                wrappedLine.append(str.substring(offset, spaceToWrapAt));
                wrappedLine.append(newLineStr);
                offset = spaceToWrapAt + 1;
                
            } else {
                // really long word or URL
                if (wrapLongWords) {
                    // wrap really long word one line at a time
                    wrappedLine.append(str.substring(offset, wrapLength + offset));
                    wrappedLine.append(newLineStr);
                    offset += wrapLength;
                } else {
                    // do not wrap really long word, just extend beyond limit
                    spaceToWrapAt = str.indexOf(' ', wrapLength + offset);
                    if (spaceToWrapAt >= 0) {
                        wrappedLine.append(str.substring(offset, spaceToWrapAt));
                        wrappedLine.append(newLineStr);
                        offset = spaceToWrapAt + 1;
                    } else {
                        wrappedLine.append(str.substring(offset));
                        offset = inputLineLength;
                    }
                }
            }
        }

        // Whatever is left in line is short enough to just pass through
        wrappedLine.append(str.substring(offset));

        return wrappedLine.toString();
    }
    
    public static String unwrap(String wrapped) {
        List<String> lines = new ArrayList<String>(Arrays.asList(wrapped.split("\n", -1)));
        int i = 0;
        while(i < (lines.size()-1)) {
            String firstWord = getFirstWord(lines.get(i+1));
            if((firstWord != null) &&
               ((lines.get(i).length() + firstWord.length()) > DEFAULT_LINE_LENGTH)) {
                
                lines.set(i, lines.get(i) + " " + lines.get(i+1));
                lines.remove(i+1);
            }
            else {
                i++;
            }
        }
        
        String joined = join(lines, "\n");
        if(joined.charAt(joined.length()-1) == '\n') {
            joined = joined.substring(0, joined.length()-1);
        }
        
        return joined;
    }
    
    
    private static String getFirstWord(String line) {
        String[] words = line.split(" ");
        if((words.length == 0)||(words[0].length() == 0)) {
            return null;
        }
        
        return words[0];
    }
    
    private static String join(List<String> strings, String sep) {
        String s = "";
        for(int i = 0; i < strings.size(); i++) {
            s += strings.get(i);
            if(i < strings.size()-1) {
                s += sep;
            }
        }
        return s;
    }
}
