/*
 * FilenameFilterByEnding.java
 */

package EDU.gatech.cc.is.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Create a FilenameFilter that only accepts names whose ending matches
 * a given value.
 * <p>
 * Copyright (c)2000 Tucker Balch
 *
 * @author Tucker Balch
 * @version $Revision: 1.2 $
 */

public class FilenameFilterByEnding implements FilenameFilter {
    private final String ending;

    /**
     * Create a FilenameFilterByEnding object.
     *
     * @param e String, the ending to match.
     */
    public FilenameFilterByEnding(String e) {
        ending = e;
    }

    /**
     * Test the filter.
     */
    public static void main(String[] args) {
        File d = null;
        FilenameFilter joe = new FilenameFilterByEnding(".dsc");
        if (joe.accept(d, "eric.dsc")) System.out.println("OK");
        else System.out.println("FAIL");
        if (joe.accept(d, ".dscasd")) System.out.println("FAIL");
        else System.out.println("OK");
        if (joe.accept(d, "dsc")) System.out.println("FAIL");
        else System.out.println("OK");
    }

    /**
     * Check a filename to see if it meets critera.
     *
     * @param d    File, the file.
     * @param name String, the name of the file.
     * @return true if ending matches, false otherwise.
     */
    public boolean accept(File d, String name) {
        if (name == null)
            return (false);
        else if (name.length() < ending.length())
            return (false);
        else {
            String nameending = name.substring(name.length()
                    - ending.length());
            return nameending.equals(ending);
        }
    }

}
