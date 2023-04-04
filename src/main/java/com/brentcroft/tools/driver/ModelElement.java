package com.brentcroft.tools.driver;

import com.brentcroft.tools.model.Model;
import com.brentcroft.tools.model.ModelEvent;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

public interface ModelElement
{
    int VOLATILE_RETRIES = 5;

    Model getSelf();

    Browser getBrowser();

    WebDriver getWebDriver();

    default IPath getIPath()
    {
        return new IPath( getSelf() );
    }

    default String xpath()
    {
        return getIPath().xpath();
    }

    default void volatileElement( BiConsumer< Model, WebElement > consumer )
    {
        int retries = VOLATILE_RETRIES;
        while ( true )
        {
            try
            {
                consumer.accept( getSelf(), getWebElement() );
                break;
            }
            catch ( StaleElementReferenceException e )
            {
                retries--;
                if ( retries < 1 )
                {
                    throw new VolatileElementException( this, VOLATILE_RETRIES );
                }
                System.out.printf( "stale element: %s; retries=%s%n", this, retries );
                getSelf().maybeDelay();
            }
        }
    }

    default < V > V volatileValue( BiFunction< Model, WebElement, V > consumer )
    {
        int retries = VOLATILE_RETRIES;
        while ( true )
        {
            try
            {
                return consumer.apply( getSelf(), getWebElement() );
            }
            catch ( StaleElementReferenceException e )
            {
                retries--;
                if ( retries < 1 )
                {
                    throw new VolatileElementException( this, VOLATILE_RETRIES );
                }
                System.out.printf( "stale element: %s; retries=%s%n", this, retries );
                getSelf().maybeDelay();
            }
        }
    }

    default void switchFrame()
    {
        Model item = getSelf();
        Model root = item.getRoot();
        IPath ipath = getIPath();
        final String currentFrameKey = "$currentFrame";
        String currentFrame = ( String ) root.get( currentFrameKey );
        if ( currentFrame != null && currentFrame.equals( ipath.innerFrame() ) )
        {
            return;
        }
        WebDriver driver = getWebDriver();

        driver.switchTo().defaultContent();

        if ( ipath == null || ".".equals( ipath.innerFrame() ) )
        {
            root.remove( currentFrameKey );
        }
        else
        {
            SearchContext[] frameElement = { driver };
            ipath
                    .frames()
                    .forEach( frame -> {
                        List< WebElement > frames = frameElement[ 0 ].findElements( By.xpath( frame ) );
                        if ( frames.size() > 0 )
                        {
                            frameElement[ 0 ] = frames.get( 0 );
                            driver.switchTo().frame( frames.get( 0 ) );
                            root.put( currentFrameKey, frame );

                        }
                        else
                        {
                            String msg = format( "'%s' -> frame not found: %s; current=%s", item.path(), frame, currentFrame );
                            getSelf()
                                    .notifyModelEvent(
                                            ModelEvent
                                                    .EventType
                                                    .MESSAGE
                                                    .newEvent( getSelf(), msg ) );
                        }
                    } );
        }
    }

    default SearchContext navigateShadows()
    {
        Function< Model, Model > pf = ( model ) -> ( Model ) Optional
                .ofNullable( model.getParent() )
                .filter( p -> p instanceof Model )
                .orElse( null );
        LinkedList< ModelItem > hosts = new LinkedList<>();
        Model parentModel = pf.apply( getSelf() );
        while ( nonNull( parentModel ) )
        {
            ModelItem item = ( ModelItem ) parentModel;
            if ( item.hasShadowRoot() )
            {
                hosts.addFirst( item );
            }
            parentModel = pf.apply( parentModel );
        }
        SearchContext context = getWebDriver();
        for ( ModelItem host : hosts )
        {
            context = host.getWebElement( context ).getShadowRoot();
        }
        return context;
    }

    default boolean hasShadowRoot()
    {
        return getSelf().containsKey( "$shadow" );
    }

    default WebElement getWebElement()
    {
        return getWebElement( navigateShadows() );
    }

    default WebElement getWebElement( SearchContext context )
    {
        switchFrame();
        IPath ipath = getIPath();
        try
        {
            List< WebElement > elements = context.findElements( ipath.by() );
            if ( elements.size() > ipath.index() )
            {
                return elements.get( ipath.index() );
            }
            throw new NoSuchElementException( "Insufficient elements: " + elements.size() );
        }
        catch ( NoSuchElementException e )
        {
            throw new NoSuchElementException(
                    format( "%s -> %s", getSelf().path(), ipath ), e );
        }
    }

    default List< WebElement > getWebElements()
    {
        return getWebElements( navigateShadows() );
    }

    default List< WebElement > getWebElements( SearchContext context )
    {
        switchFrame();
        IPath ipath = getIPath();
        try
        {
            return context.findElements( ipath.by() );
        }
        catch ( NoSuchElementException e )
        {
            throw new NoSuchElementException(
                    format( "%s -> %s", getSelf().path(), ipath ), e );
        }
    }

    default int count()
    {
        switchFrame();
        IPath ipath = getIPath();
        try
        {
            return getWebDriver().findElements( ipath.by() ).size();
        }
        catch ( NoSuchElementException e )
        {
            throw new NoSuchElementException(
                    format( "%s -> %s", getSelf().path(), ipath ), e );
        }
    }

    default boolean isDisplayed()
    {
        return volatileValue( ( i, e ) -> e.isDisplayed() );
    }

    default boolean isEnabled()
    {
        return volatileValue( ( i, e ) -> e.isEnabled() );
    }

    default boolean isSelected()
    {
        return volatileValue( ( i, e ) -> e.isSelected() );
    }

    default boolean isClickable()
    {
        return volatileValue( ( i, e ) -> e.isDisplayed() && e.isEnabled() );
    }

    default boolean exists()
    {
        return getWebElements().size() > 0;
    }

    default boolean notExists()
    {
        return getWebElements().size() == 0;
    }

    default boolean assertExists()
    {
        if ( exists() )
        {
            return true;
        }
        throw new AssertionError( "Element does not exist: " + getSelf() );
    }

    default boolean assertNotExists()
    {
        if ( notExists() )
        {
            return true;
        }
        throw new AssertionError( "Element does exist: " + getSelf() );
    }

    default String getAttribute(String key) {
        return volatileValue( ( i, e ) -> e.getAttribute( key ) );
    }

    default String getText()
    {
        return volatileValue( ( i, e ) -> {
            switch ( e.getTagName().toLowerCase() )
            {
                case "select":
                    return new Select( e ).getFirstSelectedOption().getText();
                case "button":
                    return e.getText();
                default:
                    return Optional
                            .ofNullable( e.getAttribute( "value" ) )
                            .orElseGet( e::getText );
            }
        } );
    }

    default boolean containsText( String text )
    {
        return Optional
                .ofNullable( getText() )
                .filter( t -> t.contains( text ) )
                .isPresent();
    }

    default boolean assertContainsText( String text )
    {
        if ( containsText( text ) )
        {
            return true;
        }
        Model item = getSelf();
        throw new AssertionError( format( "Element '%s' does not contain text: %s", item.path(), text ) );
    }

    default boolean equalsText( String text )
    {
        return Optional
                .ofNullable( getText() )
                .filter( t -> t.equals( text ) )
                .isPresent();
    }

    default boolean assertEqualsText( String text )
    {
        if ( equalsText( text ) )
        {
            return true;
        }
        Model item = getSelf();
        throw new AssertionError( format( "Element '%s' does not equal text: %s", item.path(), text ) );
    }

    default boolean matchesRegex( String regex )
    {
        Pattern pattern = Pattern.compile( regex );
        return Optional
                .ofNullable( getText() )
                .filter( t -> pattern.matcher( t ).matches() )
                .isPresent();
    }

    default boolean assertMatchesRegex( String regex )
    {
        if ( matchesRegex( regex ) )
        {
            return true;
        }
        Model item = getSelf();
        throw new AssertionError( format( "Element '%s' does not match regex: %s", item.path(), regex ) );
    }

    default ModelElement click()
    {
        volatileElement( ( i, e ) -> e.click() );
        getSelf().maybeDelay();
        return this;
    }

    default ModelElement setText( CharSequence... keys )
    {
        volatileElement( ( i, e ) -> {
            e.sendKeys( Keys.chord( Keys.CONTROL, "a" ) );
            e.sendKeys( Keys.DELETE );
            if ( keys != null && keys.length > 0 )
            {
                e.sendKeys( keys );
            }
        } );
        getSelf().maybeDelay();
        return this;
    }

    default ModelElement sendKeys( CharSequence... keys )
    {
        volatileElement( ( i, e ) -> e.sendKeys( keys ) );
        getSelf().maybeDelay();
        return this;
    }

    default ModelElement tab()
    {
        volatileElement( ( i, e ) -> e.sendKeys( Keys.TAB ) );
        getSelf().maybeDelay();
        return this;
    }

    default ModelElement enter()
    {
        volatileElement( ( i, e ) -> e.sendKeys( Keys.ENTER ) );
        getSelf().maybeDelay();
        return this;
    }

    default ModelElement space()
    {
        volatileElement( ( i, e ) -> e.sendKeys( Keys.SPACE ) );
        getSelf().maybeDelay();
        return this;
    }

    default ModelElement selectByText( String text )
    {
        volatileElement( ( i, e ) -> new Select( e ).selectByVisibleText( text ) );
        getSelf().maybeDelay();
        return this;
    }

    default ModelElement selectByValue( String text )
    {
        volatileElement( ( i, e ) -> new Select( e ).selectByValue( text ) );
        getSelf().maybeDelay();
        return this;
    }

    default ModelElement selectByIndex( int index )
    {
        volatileElement( ( i, e ) -> new Select( e ).selectByIndex( index ) );
        getSelf().maybeDelay();
        return this;
    }

    default ModelElement setAttribute( String key, Object value )
    {
        String script = format( "arguments[0].setAttribute( '%s', '%s' )", key, value );
        ( ( JavascriptExecutor ) getWebDriver() ).executeScript( script, getWebElement() );
        return this;
    }

    default ModelElement setStyleAttribute( String key, Object value )
    {
        String script = format( "arguments[0].style.%s = '%s'", key, value );
        ( ( JavascriptExecutor ) getWebDriver() ).executeScript( script, getWebElement() );
        return this;
    }
}
