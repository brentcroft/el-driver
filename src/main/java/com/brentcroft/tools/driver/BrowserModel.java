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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;

@Getter
@Setter
public class BrowserModel
{
    private Downloads downloads = new Downloads();
    private boolean autoQuit = true;
    private boolean quitAfterAll = false;
    private boolean allowInteractive = false;
    private boolean headless = true;

    private WebDriver driver;
    private PageModel pageModel = new PageModel();
    private ModelItem staticModel = new ModelItem();

    private LinkedHashMap<String, Runnable> afters = new LinkedHashMap<>();
    private LinkedHashMap<String, Runnable> afterAlls = new LinkedHashMap<>();

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
        if (driver != null && (isAutoQuit() || isHeadless())) {
            driver.quit();
            driver = null;
        }
    }

    public void setPageModel(PageModel pageModel) {
        this.pageModel = pageModel;
        pageModel.putAll( staticModel );
        pageModel.put( "Downloads", downloads );
    }

    public void open() {
        if (driver != null ) {

        } else if (!pageModel.containsKey( "$driverPath" )) {
            throw new IllegalArgumentException( "PageModel does not contain: $driverPath" );
        } else if (!pageModel.containsKey( "$driverModel" )) {
            throw new IllegalArgumentException( "PageModel does not contain: $driverModel" );
        } else if (!pageModel.containsKey( "$downloadDir" )) {
            throw new IllegalArgumentException( "PageModel does not contain: $downloadDir" );
        }

        if (pageModel.containsKey( "$downloadDir" )) {
            File downloadFile = new File( pageModel.get( "$downloadDir" ).toString());
            if (!downloadFile.exists()|| !downloadFile.isDirectory()) {
                throw new IllegalArgumentException(format("Either the directory does not exist or it's not a directory: $downloadDir: %s", downloadFile));
            }
            downloads.setDirectory(downloadFile);
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
                driver = new ChromeDriver(chromeOptions);
                break;

            case "edge":
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.setHeadless( headless );
                edgeOptions.addArguments( "--disable-extensions" );
                edgeOptions.setExperimentalOption( "prefs", prefs );
                System.setProperty( "webdriver.edge.driver", driverPath );
                driver = new EdgeDriver(edgeOptions);
                break;

            case "safari":
                SafariOptions safariOptions = new SafariOptions();
//                safariOptions.setHeadless( headless );
//                safariOptions.addArguments( "--disable-extensions" );
//                safariOptions.setExperimentalOption( "prefs", prefs );
                System.setProperty( "webdriver.safari.driver", driverPath );
                driver = new SafariDriver(safariOptions);
                break;

            default:
                throw new IllegalArgumentException( "Not implemented for driver model: " + driverModel );
        }
        downloads.setDriver(driver);
        pageModel.put( "$driver", driver);

        driver.get( (String)pageModel.get("$url"));
        driver.manage().window().maximize();
    }
}
