package com.brentcroft.tools.driver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class Browsers extends HashMap<String, Browser>
{
    private static final Browsers instance = new Browsers();
    private static final String DEFAULT = "default";
    private final Browser defaultBrowser = new Browser();

    public static Browsers instance() {
        return instance;
    }

    private Browsers() {
        put( DEFAULT, defaultBrowser );
    }

    public Browser put(String key, Browser browser) {
        // static copies of all page models
        browser.getPageModel().putStatic( key, browser.getPageModel() );
        return super.put(key, browser);
    }

    public Browser getDefaultBrowser() {
        return defaultBrowser;
    }

    public void close() {
        forEach((key, browser) -> {
            try {
                System.out.printf( "Closing browser: %s%n", key);
                browser.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }
    public void closeCompletely() {
        forEach((key, browser) -> {
            try {
                System.out.printf( "Closing browser completely: %s%n", key);
                browser.closeCompletely();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Object execute( String steps) {
        return defaultBrowser
                .getPageModel()
                .steps( steps );
    }


    public Object executeFile( String filename) throws IOException
    {
        String steps = String
                .join( "\n", Files
                .readAllLines( Paths.get( filename ) ) );

        return execute( steps );
    }

    public static void main(String[] args) throws IOException
    {
        if (args == null || args.length < 1) {
            throw new IllegalArgumentException("Must supply a steps file");
        }

        try {
            instance().executeFile( args[0] );
        } finally {
            instance().close();
            instance().closeCompletely();
        }
    }
}
