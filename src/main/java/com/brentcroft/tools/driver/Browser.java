package com.brentcroft.tools.driver;

import lombok.Getter;
import lombok.Setter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.io.File;
import java.time.Duration;
import java.util.*;

import static java.lang.String.format;

@Getter
@Setter
public class Browser
{
    private static final double DEFAULT_DELAY_SECONDS = 0.1;
    private static final double DEFAULT_IMPLICIT_WAIT_SECONDS = 5;
    private Downloads downloads = new Downloads();
    private boolean autoQuit = true;
    private boolean quitAfterAll = false;
    private boolean allowInteractive = true;
    private boolean headless = false;

    private WebDriver webDriver;
    private PageModel pageModel = new PageModel();

    private final Stack<Long> delayStack = new Stack<>();
    private final Stack<Long> implicitWaitStack = new Stack<>();

    private final Map<String, Runnable> beforeAlls = new HashMap<String, Runnable>() {
        public Runnable put(String key, Runnable action){
            if (!containsKey( key )) {
                super.put(key, action );
                action.run();
            }
            return null;
        }
    };
    private final Map<String, Runnable> afters = new LinkedHashMap<>();
    private final Map<String, Runnable> afterAlls = new LinkedHashMap<>();

    public void close() {
        try {
            afters
                    .forEach( (script, after) -> {
                        try {
                            after.run();
                        } catch (Exception e) {
                            System.out.printf( "Error running after: %s; %s%n", script, e);
                            e.printStackTrace();
                        }
                    } );
            afters.clear();
        } finally {
            if (!quitAfterAll) {
                quitDriver();
            }
        }
    }

    public void closeCompletely() {
        try {
            afterAlls
                    .forEach( (script, after) -> {
                        try {
                            after.run();
                        } catch (Exception e) {
                            System.out.printf( "Error running after all: %s; %s%n", script, e);
                            e.printStackTrace();
                        }
                    } );
            afterAlls.clear();
        } finally {
            quitDriver();
        }
    }

    public void quitDriver() {
        if ( webDriver != null && (isAutoQuit() || isHeadless())) {
            webDriver.quit();
            webDriver = null;
        }
    }

    public void setPageModel(PageModel pageModel) {
        this.pageModel = pageModel;
        pageModel.setBrowser( this );
    }

    public void open() {
        if ( webDriver != null ) {
        } else if (!pageModel.containsKey( "$driverPath" )) {
            throw new IllegalArgumentException( "PageModel does not contain: $driverPath" );

        } else if (!pageModel.containsKey( "$driverModel" )) {
            throw new IllegalArgumentException( "PageModel does not contain: $driverModel" );

        } else if (!pageModel.containsKey( "$downloadDir" )) {
            throw new IllegalArgumentException( "PageModel does not contain: $downloadDir" );

        } else {

            if (pageModel.containsKey( "$downloadDir" )) {
                File downloadFile = new File( pageModel.get( "$downloadDir" ).toString());
                if (!downloadFile.exists()|| !downloadFile.isDirectory()) {
                    throw new IllegalArgumentException(format("Either the directory does not exist or it's not a directory: $downloadDir: %s", downloadFile));
                }
                downloads.setDirectory(downloadFile);
            }

            String driverPath = (String)pageModel.get("$driverPath");
            String driverModel = (String)pageModel.get("$driverModel");

            Map<String,Object> prefs = new HashMap<>();
            prefs.put( "download.default_directory", downloads.getDownloadPath() );
            prefs.put( "download.prompt_for_download", false );

            switch (driverModel) {
                case "chrome":
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.setHeadless( headless );
                    chromeOptions.addArguments( "--disable-extensions" );
                    chromeOptions.setExperimentalOption( "prefs", prefs );
                    System.setProperty( "webdriver.chrome.driver", driverPath );
                    webDriver = new ChromeDriver(chromeOptions);
                    break;

                case "edge":
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.setHeadless( headless );
                    edgeOptions.addArguments( "--disable-extensions" );
                    edgeOptions.setExperimentalOption( "prefs", prefs );
                    System.setProperty( "webdriver.edge.driver", driverPath );
                    webDriver = new EdgeDriver(edgeOptions);
                    break;

                case "safari":
                    SafariOptions safariOptions = new SafariOptions();
                    System.setProperty( "webdriver.safari.driver", driverPath );
                    webDriver = new SafariDriver(safariOptions);
                    break;

                default:
                    throw new IllegalArgumentException( "Not implemented for driver model: " + driverModel );
            }
        }

        if (pageModel.containsKey( "$allowInteractive" ))
        {
            allowInteractive = Boolean.parseBoolean( pageModel.get( "$allowInteractive" ).toString() );
        }

        if (pageModel.containsKey( "$headless" ))
        {
            headless = Boolean.parseBoolean( pageModel.get( "$headless" ).toString() );
            if (headless) {
                allowInteractive = false;
            }
        }
        if (pageModel.containsKey( "$quitAfterAll" ))
        {
            quitAfterAll = Boolean.parseBoolean( pageModel.get( "$quitAfterAll" ).toString() );
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

        webDriver.get( (String)pageModel.get("$url"));
        webDriver.manage().window().maximize();
    }
    public void setDelay( long delayMillis) {
        getDelayStack().push( delayMillis );
    }
    public void resetDelay() {
        getDelayStack().pop();
    }
    public long getDelay() {
        return getDelayStack().peek();
    }
    public void setImplicitWait( long implicitWaitMillis) {
        getImplicitWaitStack().push( implicitWaitMillis );
    }
    public void resetImplicitWait() {
        getImplicitWaitStack().pop();
    }
    public long getImplicitWait() {
        return getImplicitWaitStack().peek();
    }
}
