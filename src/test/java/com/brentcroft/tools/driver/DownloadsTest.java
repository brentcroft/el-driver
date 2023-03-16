package com.brentcroft.tools.driver;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DownloadsTest
{
    @Test()
    public void throwsExceptionForFileNotDetected()
    {
        try {
            Downloads downloads = new Downloads();
            downloads.setDirectory( new File(".") );
            downloads.detect( "non-existing.file", 500 );
            fail("Expected exception!");

        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }
    }

    @Test()
    public void movesNoFiles()
    {
        Downloads downloads = new Downloads();
        downloads.setDirectory( new File(".") );
        int numMoved = downloads.move( "non-existing-files", "target" );
        assertEquals(0, numMoved);
    }

    @Test()
    public void removesNoFiles()
    {
        Downloads downloads = new Downloads();
        downloads.setDirectory( new File(".") );
        int numRemoved = downloads.remove( "non-existing-files" );
        assertEquals(0, numRemoved);
    }
}
