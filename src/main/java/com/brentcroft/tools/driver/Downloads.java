package com.brentcroft.tools.driver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;


public class Downloads
{
    private WebDriver driver;
    private File dir;

    public void setDirectory( File dir )
    {
        this.dir = dir;
    }

    public void setDriver( WebDriver driver )
    {
        this.driver = driver;
    }

    public String getDownloadPath()
    {
        if (dir == null) {
            throw new IllegalArgumentException("dir is null");
        }
        return dir.getAbsolutePath();
    }

    public int remove(String prefix) {
        if (dir == null) {
            throw new IllegalArgumentException("dir is null");
        }
        int[] numDeleted = {0};
        Stream
                .of( Objects.requireNonNull( dir.listFiles( f -> f.getName().startsWith( prefix ) ) ) )
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
                .of( Objects.requireNonNull( dir.listFiles( f -> f.getName().startsWith( prefix ) ) ) )
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
        new WebDriverWait( driver, Duration.ofMillis( millisTimeout ) )
                .until( driver -> new File(dir,filename).exists()  );
    }
}
