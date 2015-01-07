package org.lable.dynamicconfig.core.util;

import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Static utility class. Use {@link #gatherConfigurationFiles(String, boolean)} to combine multiple text files in a
 * directory on the classpath into a single {@link InputStream}.
 */
public class TextFileGatherer {
    /**
     * Combine the contents of several files into one stream.
     *
     * @param path                  Path to directory containing the files.
     * @param separateWithLinebreak If true, a linebreak is added between each file.
     * @return The combined InputStream for all files in path.
     */
    public static InputStream gatherConfigurationFiles(String path, boolean separateWithLinebreak)
            throws IOException {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Collection<String> files = ls(path, ".*\\.yml");

        // Combine all files.
        String combined = "";
        for (String fileName : files) {
            InputStream is = cl.getResourceAsStream(fileName);
            combined += IOUtils.toString(is, "UTF-8");
            if (separateWithLinebreak) {
                combined += "\n";
            }
        }

        // Ensure that there is at least something.
        if (combined.isEmpty()) {
            combined = "\n";
        }

        return IOUtils.toInputStream(combined, "UTF-8");
    }

    /**
     * List all files matching pattern on the classpath in the specified location.
     *
     * @param path    Path to look in.
     * @param pattern Pattern to match.
     * @return List of file paths.
     */
    public static Collection<String> ls(String path, String pattern) {
        Reflections reflections = new Reflections(path, new ResourcesScanner());
        Pattern p = Pattern.compile(pattern);
        return reflections.getResources(p);
    }
}
