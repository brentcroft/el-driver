package com.brentcroft.tools.driver;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class Browsers
{
    private static final Map<String, Browser> browsers = new HashMap<>();
    private static final Browsers instance = new Browsers();
    private static final String DEFAULT = "default";

    public static Browsers instance() {
        return instance;
    }

    private Browsers() {
        setBrowser( DEFAULT, new Browser() );
    }

    public static Browser getDefaultBrowser() {
        if (!browsers.containsKey( DEFAULT )) {
            throw new IllegalArgumentException(format("No such browser with key '%s'.", DEFAULT));
        }
        return browsers.get( DEFAULT );
    }


    public Browser getBrowser( String key ) {
        if (!browsers.containsKey( key )) {
            throw new IllegalArgumentException(format("No such browser with key '%s'.", key));
        }
        return browsers.get( key );
    }
    public void setBrowser( String key, Browser browser ) {
        if (browsers.containsKey( key )) {
            throw new IllegalArgumentException(format("A browser with key '%s' already exists.", key));
        }
        browsers.put( key, browser );
    }
}
