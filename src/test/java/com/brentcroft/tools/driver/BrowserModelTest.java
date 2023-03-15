package com.brentcroft.tools.driver;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


import java.nio.file.Paths;

import static java.lang.String.format;
import static org.junit.Assert.*;

public class BrowserModelTest
{
    private final PageModel pageModel = Browsers.newBrowser( "alfredo" );

    @Before
    public void installPageModel() {
        pageModel.setCurrentDirectory( Paths.get("src/test/resources/sites") );
    }

    @After
    public void runsAfters() {
        Browsers.instance().close();
    }

    @Test
    public void loadsBrentcroftJBNDTestSite() {
        pageModel.appendFromJson( "{ '$json': 'brentcroft-jbnd-test.json' }" );
        pageModel.getBrowser().open();

        String expected = "((4:2)|5)";
        pageModel.eval( format("scriptText.setText( '%s' ) ; eval.click()",expected) );
        String actual = (String)pageModel.eval( "scriptText.getText().trim()" );
        assertEquals(expected, actual);
    }


    @Test
    public void loadsBrentcroftGameSite() {
        pageModel.appendFromJson( "{ '$json': 'brentcroft-games.json' }" );
        pageModel.getBrowser().open();

        pageModel.steps( "newGameButton.assertExists(); stepButton.assertExists(); gamePlay.assertExists(); stack.assertNotExists()" );
        pageModel.steps( "newGameButton.click(); stack.assertExists()" );

        pageModel.eval( "$self.whileDo( () -> !stack.equalsText('Stack size: 0'), () -> stepButton.click(), 100 )" );

        String expected = "!stack.equalsText('Stack size: 0')";

        assertFalse( (Boolean) pageModel
                .eval( expected ) );

        pageModel.getBrowser().saveScreenshot("stack size is zero");
    }

    @Test
    public void playsBrentcroftGameSite() {
        pageModel.appendFromJson( "{ '$json': 'brentcroft-games.json' }" );
        pageModel.getBrowser().open();

        pageModel.steps("$self.run()");
        assertTrue( (Boolean) pageModel.eval( "stack.equalsText('Stack size: 0')" ) );

        Browsers
                .instance()
                .saveScreenshots("stack size is zero");
    }

    @Test
    public void playsBrentcroftGameSiteXml() {
        pageModel.appendFromJson( "{ '$xml': 'brentcroft-site.xml' }" );
        pageModel.getBrowser().open();

        assertTrue( (Boolean) pageModel.eval( "!shithead.exists()" ) );

        pageModel.steps("openShithead.run()");
        assertTrue( (Boolean) pageModel.eval( "shithead.exists()" ) );
    }

    @Test
    @Ignore
    public void opensBrentcroftOnload() {
        pageModel.appendFromJson( "{ '$json': 'brentcroft-games.json', '$onload': '$self.run()' }" );
        pageModel.getBrowser().open();
        assertTrue( (Boolean) pageModel.eval( "stack.equalsText('Stack size: 0')" ) );
    }
}
