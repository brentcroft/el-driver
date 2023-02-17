package com.brentcroft.tools.driver;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class BrowsersTest
{
    @Test
    public void executesScript() throws IOException
    {
        Object result = Browsers
                .instance()
                .executeFile( "src/test/resources/scripts/brentcroft-play.steps" );

        assertEquals("ok", result);
    }

    @Test
    public void runsMain() throws IOException
    {
        String[] args = { "src/test/resources/scripts/brentcroft-play.steps" };
        Browsers.main(  args );
    }
}
