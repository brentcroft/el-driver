package com.brentcroft.tools.driver;

import com.brentcroft.tools.model.Model;
import org.openqa.selenium.By;

import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class IPath
{
    private String frame;
    private String xpath;
    private int index;
    private String cssSelector;
    private String id;
    private String className;

    public IPath( Model model) {
        frame = Optional
                .ofNullable(model.get("$frame"))
                .map(Object::toString)
                .orElse(".");

        if ( model.containsKey("$id") ) {
            id = (String)model.get("$id");
        }
        if ( model.containsKey("$className") ) {
            className = (String)model.get("$className");
        }
        if ( model.containsKey("$cssSelector") ) {
            cssSelector = (String)model.get("$cssSelector");
        }
        if ( model.containsKey("$index") ) {
            index = Integer.parseInt((String)model.get("$index"));
        }
        if ( model.containsKey("$xpath") ) {
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
                    .orElse(null);
        }
        if (isNull(xpath) && isNull(cssSelector) && isNull( id ) && isNull( className )) {
            throw new IllegalArgumentException("IPath has no value for: xpath, cssSelector, id, className");
        }
    }

    public By by() {
        if (nonNull(xpath)) {
            return By.xpath( xpath );

        } else if (nonNull(cssSelector)) {
            return By.cssSelector( cssSelector );

        } else if (nonNull(id)) {
            return By.id( id );

        } else if (nonNull(className)) {
            return By.id( className );
        }
        throw new IllegalArgumentException("IPath has no value for: xpath, cssSelector, id, className");
    }

    public String frame() {
        return frame;
    }
    public String xpath() {
        return xpath;
    }

    public String id() {
        return id;
    }

    public String className() {
        return className;
    }

    public String cssSelector() {
        return cssSelector;
    }
    public int index() {
        return index;
    }
    public String toString() {
        if (nonNull(xpath)) {
            return format("{%s} %s {index:%d}", frame, xpath, index);

        }
        if (nonNull(cssSelector)) {
            return format("{%s} %s {index:%d}", frame, cssSelector, index);

        } else if (nonNull(id)) {
            return format("{%s} #%s {index:%d}", frame, id, index);

        } else if (nonNull(className)) {
            return format("{%s} .%s {index:%d}", frame, className, index);

        } else {
            return null;
        }
    }
}
