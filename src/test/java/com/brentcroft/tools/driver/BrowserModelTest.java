package com.brentcroft.tools.driver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

import static java.lang.String.format;
import static org.junit.Assert.*;

public class BrowserModelTest
{
    private static final BrowserModel bm = new BrowserModel( );
    private final PageModel pageModel = new PageModel();

    @Before
    public void installPageModel() {
        bm.setPageModel( pageModel );
    }

    @After
    public void runsAfters() {
        bm.close();
    }

    @Test
    public void loadsBrentcroftJBNDTestSite() {
        pageModel.appendFromJson( "{ '$json': 'src/test/resources/sites/brentcroft-jbnd-test.json' }" );
        bm.open();

        String expected = "((4:2)|5)";
        pageModel.eval( format("scriptText.setText( '%s' ) ; eval.click()",expected) );
        String actual = (String)pageModel.eval( "scriptText.getText()" );
        assertEquals(expected, actual);
    }


    @Test
    public void loadsBrentcroftGameSite() {
        pageModel.appendFromJson( "{ '$json': 'src/test/resources/sites/brentcroft-games.json' }" );
        bm.open();

        pageModel.eval( "newGameButton.assertExists()" );
        pageModel.eval( "stepButton.assertExists()" );
        pageModel.eval( "gamePlay.assertExists()" );
        pageModel.eval( "stack.assertNotExists()" );
        pageModel.eval( "newGameButton.click()" );
        pageModel.eval( "stack.assertExists()" );

        String expected = "!stack.equalsText('Stack size: 0')";
        String operation = "stepButton.click()";

        assertFalse( (Boolean) pageModel
                .whileDo( expected, operation, 100 )
                .eval( expected ) );
    }

    @Test
    public void playsBrentcroftGameSite() {
        pageModel.appendFromJson( "{ '$json': 'src/test/resources/sites/brentcroft-games.json' }" );
        bm.open();

        pageModel.run();
        assertTrue( (Boolean) pageModel.eval( "stack.equalsText('Stack size: 0')" ) );
    }


    @Test
    public void loadsJakartaELSite() {
        pageModel.appendFromJson( "{ '$json': 'src/test/resources/sites/jakarta-el.json' }" );
        bm.open();

        assertEquals(true, pageModel.eval( "packages.lambdaExpression.exists()" ) );
        assertEquals(false, pageModel.eval( "classes.lambdaExpression.exists()" ) );

        pageModel.eval( "packages.lambdaExpression.click()" );

        assertEquals(true, pageModel.eval( "classes.exists()" ) );
        assertEquals(true, pageModel.eval( "classes.header.containsText( 'LambdaExpression' )" ) );
        assertEquals(true, pageModel.eval( "classes.lambdaExpression.exists()" ) );
    }
}
