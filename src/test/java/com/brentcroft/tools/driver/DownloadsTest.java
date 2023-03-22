package com.brentcroft.tools.driver;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static com.brentcroft.tools.el.ELFunctions.textToFile;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DownloadsTest
{
    private File downloadsDirectory;

    @Before
    public void prepareDownloadsDirectory() {
        downloadsDirectory = Paths.get("target", "downloads").toFile();
        downloadsDirectory.mkdirs();
    }

    private String createFile( String filename ) throws IOException
    {
        textToFile( "123", new File( downloadsDirectory , filename ).getPath() );
        return filename;
    }


    @Test()
    public void throwsExceptionForFileNotDetected()
    {
        try {
            Downloads downloads = new Downloads();
            downloads.setDirectory( new File(".") );
            downloads.detect( "non-existing.file", 500 );
            fail("Expected exception!");

        } catch (IllegalArgumentException e) {
            System.out.println( e );
        }
    }

    @Test()
    public void movesNoFiles()
    {
        Downloads downloads = new Downloads();
        downloads.setDirectory( downloadsDirectory );
        int numMoved = downloads.move( "non-existing-files", "target" );
        assertEquals(0, numMoved);
    }


    @Test()
    public void movesFiles() throws IOException
    {
        File targetDirectory = Paths.get("target", "moved").toFile();
        targetDirectory.mkdirs();

        String fileToMove = createFile("existing-file.xyz");

        Downloads downloads = new Downloads();
        downloads.setDirectory( downloadsDirectory );
        int numMoved = downloads.move( fileToMove, targetDirectory.getPath() );
        assertEquals(1, numMoved);
    }


    @Test()
    public void removesNoFiles()
    {
        Downloads downloads = new Downloads();
        downloads.setDirectory( downloadsDirectory );
        int numRemoved = downloads.remove( "non-existing-files" );
        assertEquals(0, numRemoved);
    }

    @Test()
    public void removesFiles() throws IOException
    {
        Downloads downloads = new Downloads();
        downloads.setDirectory( downloadsDirectory );

        String fileToRemove = createFile("existing-file.xyz");
        int numRemoved = downloads.remove( fileToRemove );
        assertEquals(1, numRemoved);
    }

    @Test()
    public void clearsFiles() throws IOException
    {
        Downloads downloads = new Downloads();
        downloads.setDirectory( downloadsDirectory );

        int numFiles = 10;
        for (int i = 0; i < numFiles;i++) {
            createFile(format("existing-file-%s.xyz", i));
        }

        int numRemoved = downloads.clear();
        assertEquals(numFiles, numRemoved);
    }
}
