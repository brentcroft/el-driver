package com.brentcroft.tools.driver;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Stream;

@Getter
@Setter
public class Downloads
{
    private File directory;

    public String getDownloadPath()
    {
        if ( directory == null) {
            throw new IllegalArgumentException("directory is null");
        }
        return directory.getAbsolutePath();
    }

    public int remove(String prefix) {
        if ( directory == null) {
            throw new IllegalArgumentException("directory is null");
        }
        int[] numDeleted = {0};
        Stream
                .of( Objects.requireNonNull( directory.listFiles( f -> f.getName().startsWith( prefix ) ) ) )
                .forEach( file -> {
                    if (file.exists() && file.isFile()) {
                        if (file.delete()) {
                            numDeleted[0]++;
                        }
                    }
                } );
        return numDeleted[0];
    }

    public int move(String prefix, String targetDirectory) {
        File targetDir = new File(targetDirectory);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        int[] numMoved = {0};
        Stream
                .of( Objects.requireNonNull( directory.listFiles( f -> f.getName().startsWith( prefix ) ) ) )
                .forEach( file -> {
                    File newFile = new File(targetDir, file.getName());
                    try {
                        Files.move(file.toPath(), newFile.toPath());
                        numMoved[0]++;
                    } catch ( IOException e) {
                        e.printStackTrace();
                    }
                } );
        return numMoved[0];
    }

    public void detect(String filename, long millisTimeout) {
        long timeoutMillis = System.currentTimeMillis() + millisTimeout;
        while(!new File( directory,filename).exists() && timeoutMillis < System.currentTimeMillis()) {
            try
            {
                Thread.sleep( 100 );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        }
    }
}
