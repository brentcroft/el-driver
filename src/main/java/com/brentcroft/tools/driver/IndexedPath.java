package com.brentcroft.tools.driver;

import com.brentcroft.tools.model.Model;

import java.util.Optional;

import static java.lang.String.format;

public class IndexedPath
{
    private final String frame;
    private final String xpath;
    private int index;

    public IndexedPath( Model model) {
        index = 0;
        xpath = Optional
                .ofNullable(model.get("$xpath"))
                .map(Object::toString)
                .map( path -> {
                    int indexStart = path.lastIndexOf( '[' );
                    int indexEnd = path.lastIndexOf( ']' );
                    if (indexStart > -1 && indexEnd > indexStart && indexEnd == path.length() - 1) {
                        String indexText = path.substring( indexStart + 1, indexEnd ).trim();
                        try {
                            index = Integer.parseInt(indexText) - 1;
                            return path.substring( 0, indexStart ).trim();
                        } catch (Exception ignored ) {
                        }
                    }
                    return path;
                })
                .orElseThrow(() -> new IllegalArgumentException(format("Item [%s] has no value for $xpath", model.path())));

        frame = Optional
                .ofNullable(model.get("$frame"))
                .map(Object::toString)
                .orElse(".");
    }
    public String frame() {
        return frame;
    }
    public String xpath() {
        return xpath;
    }
    public int index() {
        return index;
    }
    public String toString() {
        return format("{%s}%s;index:[%d]", frame, xpath, index);
    }
}
