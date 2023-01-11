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
    private static final Browser bm = new Browser( );
    private final PageModel pageModel = new PageModel();

    @Before
    public void installPageModel() {
        bm.setPageModel( pageModel );
        pageModel.setCurrentDirectory( Paths.get("src/test/resources/sites") );
    }

    @After
    public void runsAfters() {
        bm.close();
    }

    @Test
    public void loadsBrentcroftJBNDTestSite() {
        pageModel.appendFromJson( "{ '$json': 'brentcroft-jbnd-test.json' }" );
        bm.open();

        String expected = "((4:2)|5)";
        pageModel.eval( format("scriptText.setText( '%s' ) ; eval.click()",expected) );
        String actual = (String)pageModel.eval( "scriptText.getText().trim()" );
        assertEquals(expected, actual);
    }

    @Test
    public void loadsJakartaELSite() {
        pageModel.appendFromJson( "{ '$json': 'jakarta-el.json' }" );
        bm.open();

        assertEquals(true, pageModel.eval( "packages.lambdaExpression.exists()" ) );
        assertEquals(false, pageModel.eval( "classes.lambdaExpression.exists()" ) );

        pageModel.eval( "packages.lambdaExpression.click()" );

        assertEquals(true, pageModel.eval( "classes.exists()" ) );
        assertEquals(true, pageModel.eval( "classes.header.containsText( 'LambdaExpression' )" ) );
        assertEquals(true, pageModel.eval( "classes.lambdaExpression.exists()" ) );
    }

    @Test
    public void loadsBrentcroftGameSite() {
        pageModel.appendFromJson( "{ '$json': 'brentcroft-games.json' }" );
        bm.open();

        pageModel.steps( "newGameButton.assertExists(); stepButton.assertExists(); gamePlay.assertExists(); stack.assertNotExists()" );
        pageModel.steps( "newGameButton.click(); stack.assertExists()" );

        String expected = "!stack.equalsText('Stack size: 0')";
        String operation = "stepButton.click()";

        assertFalse( (Boolean) pageModel
                .whileDo( expected, operation, 100 )
                .eval( expected ) );
    }

    @Test
    public void playsBrentcroftGameSite() {
        pageModel.appendFromJson( "{ '$json': 'brentcroft-games.json' }" );
        bm.open();

        pageModel.steps("$self.run()");
        assertTrue( (Boolean) pageModel.eval( "stack.equalsText('Stack size: 0')" ) );
    }


    @Test
    public void playsBrentcroftGameSiteXml() {
        pageModel.appendFromJson( "{ '$xml': 'brentcroft-site.xml' }" );
        bm.open();

        assertTrue( (Boolean) pageModel.eval( "!shithead.exists()" ) );

        pageModel.steps("openShithead.run()");
        assertTrue( (Boolean) pageModel.eval( "shithead.exists()" ) );
    }



    @Test
    public void opensBrentcroftAnimalsCountSiteXml() {
        pageModel.appendFromJson( "{ '$xml': 'brentcroft-site.xml' }" );
        bm.open();


        pageModel.whileDo( "!home.animalsCount.exists()","c:delay(100)", 100 );
        assertTrue( (Boolean) pageModel.eval( "!animalsCount.exists()" ) );

        pageModel.steps("home.animalsCount.click()");

        pageModel.whileDo( "!animalsCount.exists()","c:delay(100)", 100 );
        assertTrue( (Boolean) pageModel.eval( "animalsCount.exists()" ) );
    }


    @Test
    @Ignore
    public void opensBrentcroftOnload() {
        pageModel.appendFromJson( "{ '$json': 'brentcroft-games.json', '$onload': '$self.run()' }" );
        bm.open();
        assertTrue( (Boolean) pageModel.eval( "stack.equalsText('Stack size: 0')" ) );
    }

}
