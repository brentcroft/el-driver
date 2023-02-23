package com.brentcroft.tools.driver;

import com.brentcroft.tools.model.ModelEvent;
import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.String.format;

@Getter
@Setter
public class Browser
{
    private static final double DEFAULT_DELAY_SECONDS = 0.1;
    private static final double DEFAULT_IMPLICIT_WAIT_SECONDS = 5;
    private Downloads downloads = new Downloads();
    private static int screenshotId = 0;
    private boolean autoQuit = true;
    private boolean quitAfterAll = false;
    private boolean allowInteractive = true;
    private boolean headless = false;
    private String screenshotDirectory = "target/screenshots";

    private WebDriver webDriver;
    private final PageModel pageModel = new PageModel();

    private final Stack< Long > delayStack = new Stack<>();
    private final Stack< Long > implicitWaitStack = new Stack<>();

    private final Map< String, Runnable > beforeAlls = new HashMap< String, Runnable >()
    {
        public Runnable put( String key, Runnable action )
        {
            if ( ! containsKey( key ) )
            {
                super.put( key, action );
                action.run();
            }
            return null;
        }
    };
    private final Map< String, Runnable > afters = new LinkedHashMap<>();
    private final Map< String, Runnable > afterAlls = new LinkedHashMap<>();

    public Browser()
    {
        pageModel.setBrowser( this );
    }

    public void close()
    {
        try
        {
            afters
                    .forEach( ( script, after ) -> {
                        try
                        {
                            after.run();
                        }
                        catch ( Exception e )
                        {
                            getPageModel()
                                    .notifyModelEvent(
                                            ModelEvent
                                                    .EventType
                                                    .EXCEPTION
                                                    .newEvent( getPageModel(), format( "Error running after: %s; %s", script, e ), e ) );
                        }
                    } );
            afters.clear();
        }
        finally
        {
            if ( ! quitAfterAll )
            {
                quitDriver();
            }
        }
    }

    public void closeCompletely()
    {
        try
        {
            afterAlls
                    .forEach( ( script, after ) -> {
                        try
                        {
                            after.run();
                        }
                        catch ( Exception e )
                        {
                            getPageModel()
                                    .notifyModelEvent(
                                            ModelEvent
                                                    .EventType
                                                    .EXCEPTION
                                                    .newEvent( getPageModel(), format( "Error running after all: %s; %s", script, e ), e ) );
                        }
                    } );
            afterAlls.clear();
        }
        finally
        {
            quitDriver();
        }
    }

    public void quitDriver()
    {
        if ( webDriver != null && ( isAutoQuit() || isHeadless() ) )
        {
            System.out.printf( "Quitting driver: auto=%s, headless=%s, after-all=%s%n", isAutoQuit(), isHeadless(), isQuitAfterAll() );
            webDriver.quit();
            webDriver = null;
        }
    }

    public void open()
    {
        if ( webDriver != null )
        {
        }
        else if ( ! pageModel.containsKey( "$driverPath" ) )
        {
            throw new IllegalArgumentException( "PageModel does not contain: $driverPath" );
        }
        else if ( ! pageModel.containsKey( "$driverModel" ) )
        {
            throw new IllegalArgumentException( "PageModel does not contain: $driverModel" );
        }
        else if ( ! pageModel.containsKey( "$downloadDir" ) )
        {
            throw new IllegalArgumentException( "PageModel does not contain: $downloadDir" );
        }
        else
        {
            if ( pageModel.containsKey( "$allowInteractive" ) )
            {
                allowInteractive = Boolean.parseBoolean( pageModel.get( "$allowInteractive" ).toString() );
            }

            if ( pageModel.containsKey( "$headless" ) )
            {
                headless = Boolean.parseBoolean( pageModel.get( "$headless" ).toString() );
                if ( headless )
                {
                    allowInteractive = false;
                }
            }

            if ( pageModel.containsKey( "$downloadDir" ) )
            {
                File downloadFile = new File( pageModel.get( "$downloadDir" ).toString() );
                if ( ! downloadFile.exists() || ! downloadFile.isDirectory() )
                {
                    throw new IllegalArgumentException( format( "Either the directory does not exist or it's not a directory: $downloadDir: %s", downloadFile ) );
                }
                downloads.setDirectory( downloadFile );
            }

            String driverPath = ( String ) pageModel.get( "$driverPath" );
            String driverModel = ( String ) pageModel.get( "$driverModel" );

            Map< String, Object > prefs = new HashMap<>();
            prefs.put( "download.default_directory", downloads.getDownloadPath() );
            prefs.put( "download.prompt_for_download", false );

            switch ( driverModel )
            {
                case "chrome":
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.setHeadless( headless );
                    chromeOptions.addArguments( "--disable-extensions" );
                    chromeOptions.setExperimentalOption( "prefs", prefs );
                    System.setProperty( "webdriver.chrome.driver", driverPath );
                    webDriver = new ChromeDriver( chromeOptions );
                    break;

                case "edge":
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.setHeadless( headless );
                    edgeOptions.addArguments( "--disable-extensions" );
                    edgeOptions.setExperimentalOption( "prefs", prefs );
                    System.setProperty( "webdriver.edge.driver", driverPath );
                    webDriver = new EdgeDriver( edgeOptions );
                    break;

                case "safari":
                    SafariOptions safariOptions = new SafariOptions();
                    System.setProperty( "webdriver.safari.driver", driverPath );
                    webDriver = new SafariDriver( safariOptions );
                    break;

                default:
                    throw new IllegalArgumentException( "Not implemented for driver model: " + driverModel );
            }
        }

        if ( pageModel.containsKey( "$quitAfterAll" ) )
        {
            quitAfterAll = Boolean.parseBoolean( pageModel.get( "$quitAfterAll" ).toString() );
        }
        if ( pageModel.containsKey( "$screenshotDirectory" ) )
        {
            screenshotDirectory = pageModel.get( "$screenshotDirectory" ).toString();
        }

        {
            double delaySeconds = pageModel.containsKey( "$delay" )
                                  ? Double.parseDouble( pageModel.get( "$delay" ).toString() )
                                  : DEFAULT_DELAY_SECONDS;
            setDelay( Double.valueOf( delaySeconds * 1000 ).longValue() );
        }
        {
            double implicitWaitSeconds = pageModel.containsKey( "$implicitWait" )
                                         ? Double.parseDouble( pageModel.get( "$implicitWait" ).toString() )
                                         : DEFAULT_IMPLICIT_WAIT_SECONDS;
            setImplicitWait( Double.valueOf( implicitWaitSeconds * 1000 ).longValue() );
        }

        webDriver.manage().timeouts().implicitlyWait( Duration.ofMillis( getImplicitWait() ) );

        webDriver.get( ( String ) pageModel.get( "$url" ) );

        if ( pageModel.containsKey( "$position" ) )
        {
            String position = ( String ) pageModel.get( "$position" );
            int[] coords = Stream
                    .of( position.split( "\\s*,\\s*" ) )
                    .mapToInt( Integer::parseInt )
                    .toArray();
            if ( coords.length < 4 )
            {
                throw new IllegalArgumentException( "$position must be q comma separated list of 4 integers." );
            }
            Point point = new Point( coords[ 0 ], coords[ 1 ] );
            webDriver.manage().window().setPosition( point );

            Dimension dimension = new Dimension( coords[ 2 ], coords[ 3 ] );
            webDriver.manage().window().setSize( dimension );
        }
        if ( pageModel.containsKey( "$maximize" ) && Boolean.parseBoolean( ( String ) pageModel.get( "$maximize" ) ) )
        {
            webDriver.manage().window().maximize();
        }
    }

    public void executeScript( String script, List< Object > args )
    {
        ( ( JavascriptExecutor ) webDriver ).executeScript( script, args.toArray() );
    }

    public void setDelay( long delayMillis )
    {
        getDelayStack().push( delayMillis );
    }

    public void resetDelay()
    {
        getDelayStack().pop();
    }

    public long getDelay()
    {
        return getDelayStack().peek();
    }

    public void setImplicitWait( long implicitWaitMillis )
    {
        getImplicitWaitStack().push( implicitWaitMillis );
    }

    public void resetImplicitWait()
    {
        getImplicitWaitStack().pop();
    }

    public long getImplicitWait()
    {
        return getImplicitWaitStack().peek();
    }

    public void saveScreenshot()
    {
        if (webDriver == null) {
            return;
        }

        String filename = format("screenshot-%4d.jpg", screenshotId++);

        File tempScreenshot = (( TakesScreenshot )webDriver).getScreenshotAs( OutputType.FILE );
        Path targetScreenshot = Paths.get(screenshotDirectory, filename);

        if (!targetScreenshot.toFile().getParentFile().mkdirs()) {
            System.out.printf( "Failed to make screenshot directories: %s%n", screenshotDirectory);
        }

        try
        {
            Files.copy(tempScreenshot.toPath(), targetScreenshot, StandardCopyOption.REPLACE_EXISTING);

            System.out.printf( "Saved screenshot: %s%n", targetScreenshot);

            if (!tempScreenshot.delete() ) {
                System.out.printf( "Failed to delete temp screenshot: %s%n", tempScreenshot);
            }
        }
        catch ( IOException e )
        {
            throw new IllegalArgumentException(format("Failed to copy screenshot: %s", targetScreenshot), e);
        }
    }
}
