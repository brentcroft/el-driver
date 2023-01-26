package com.brentcroft.tools.driver;

import com.brentcroft.tools.model.Model;
import com.brentcroft.tools.model.ModelEvent;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

public interface ModelElement
{
    Model getSelf();
    Browser getBrowser();
    WebDriver getWebDriver();

    default IPath getIPath() {
        return new IPath(getSelf());
    }

    default String xpath() {
        return getIPath().xpath();
    }

    default void volatileElement( BiConsumer<Model, WebElement> consumer ) {
        int retries = 5;
        while ( true ) {
            try {
                consumer.accept( getSelf(), getWebElement() );
                break;
            } catch (StaleElementReferenceException e) {
                retries--;
                if ( retries < 1) {
                    throw e;
                }
            }
        }
    }

    default void switchFrame()
    {
        Model item = getSelf();
        Model root = item.getRoot();
        IPath ipath = getIPath();
        final String currentFrameKey = "$currentFrame";
        String currentFrame = (String) root.get(currentFrameKey);
        if (currentFrame != null && currentFrame.equals( ipath.frame() )) {
            return;
        }
        WebDriver driver = getWebDriver();
        driver.switchTo().defaultContent();
        if (ipath == null || ".".equals( ipath.frame() )) {
            root.remove(currentFrameKey);
        } else {
            List< WebElement > frames = driver.findElements( By.xpath(ipath.frame()) );
            if (frames.size() > 0) {
                driver.switchTo().frame( frames.get( 0 ) );
                root.put(currentFrameKey, ipath.frame());

            } else {
                String msg = format("'%s' -> frame not found: %s; current=%s", item.path(), ipath.frame(), currentFrame);
                getSelf()
                        .notifyModelEvent(
                                ModelEvent
                                        .EventType
                                        .MESSAGE
                                        .newEvent(getSelf(), msg) );
            }
        }
    }

    default SearchContext navigateShadows() {
        Function<Model, Model> pf = (model)-> ( Model ) Optional
                .ofNullable( model.getParent() )
                .filter( p -> p instanceof Model )
                .orElse( null );
        LinkedList<ModelItem> hosts = new LinkedList<>();
        Model parentModel = pf.apply(getSelf());
        while (nonNull(parentModel)) {
            ModelItem item = (ModelItem)parentModel;
            if (item.hasShadowRoot()) {
                hosts.addFirst( item );
            }
            parentModel = pf.apply(parentModel);
        }
        SearchContext context = getWebDriver();
        for (ModelItem host : hosts) {
            context = host.getWebElement(context).getShadowRoot();
        }
        return context;
    }

    default boolean hasShadowRoot() {
        return getSelf().containsKey( "$shadow" );
    }

    default WebElement getWebElement() {
        return getWebElement(navigateShadows());
    }

    default WebElement getWebElement(SearchContext context) {
        switchFrame();
        IPath ipath = getIPath();
        try {
            List<WebElement> elements = context.findElements( ipath.by() );
            if ( elements.size() > ipath.index()) {
                return elements.get( ipath.index() );
            }
            throw new NoSuchElementException("Insufficient elements: " + elements.size());
        } catch ( NoSuchElementException e) {
            throw new NoSuchElementException(
                    format("%s -> %s", getSelf().path(), ipath), e);
        }
    }

    default List<WebElement> getWebElements() {
        return getWebElements(navigateShadows());
    }

    default List<WebElement> getWebElements(SearchContext context) {
        switchFrame();
        IPath ipath = getIPath();
        try {
            return context.findElements( ipath.by() );
        } catch ( NoSuchElementException e) {
            throw new NoSuchElementException(
                    format("%s -> %s", getSelf().path(), ipath), e);
        }
    }

    default int count() {
        switchFrame();
        IPath ipath = getIPath();
        try {
            return getWebDriver().findElements( ipath.by() ).size();
        } catch ( NoSuchElementException e) {
            throw new NoSuchElementException(
                    format("%s -> %s", getSelf().path(), ipath), e);
        }
    }

    default boolean isDisplayed() {
        return getWebElement().isDisplayed();
    }
    default boolean isEnabled() {
        return getWebElement().isEnabled();
    }
    default boolean isSelected() {
        return getWebElement().isSelected();
    }
    default boolean isClickable() {
        WebElement element = getWebElement();
        return element.isDisplayed() && element.isEnabled();
    }

    default boolean exists() {
        return getWebElements().size() > 0;
    }
    default boolean notExists() {
        return getWebElements().size() == 0;
    }
    default boolean assertExists() {
        if(exists()) {
            return true;
        }
        throw new AssertionError("Element does not exist: " + getSelf());
    }
    default boolean assertNotExists() {
        if(notExists()) {
            return true;
        }
        throw new AssertionError("Element does exist: " + getSelf());
    }

    default String getText() {
        WebElement element = getWebElement();
        switch (element.getTagName().toLowerCase()) {
            case "select":
                return new Select(element).getFirstSelectedOption().getText();
            case "button":
                return element.getText();

            default:
                return Optional
                        .ofNullable(element.getAttribute( "value" ))
                        .orElseGet( element::getText );
        }
    }

    default boolean containsText( String text ) {
        return Optional
                .ofNullable( getText() )
                .filter( t-> t.contains( text ) )
                .isPresent();
    }
    default boolean assertContainsText(String text) {
        if(containsText(text)) {
            return true;
        }
        Model item = getSelf();
        throw new AssertionError(format("Element '%s' does not contain text: %s", item.path(), text));
    }

    default boolean equalsText( String text ) {
        return Optional
                .ofNullable( getText() )
                .filter( t-> t.equals( text ) )
                .isPresent();
    }
    default boolean assertEqualsText(String text) {
        if(equalsText(text)) {
            return true;
        }
        Model item = getSelf();
        throw new AssertionError(format("Element '%s' does not equal text: %s", item.path(), text));
    }

    default boolean matchesRegex( String regex ) {
        Pattern pattern = Pattern.compile( regex );
        return Optional
                .ofNullable( getText() )
                .filter( t-> pattern.matcher( t ).matches() )
                .isPresent();
    }
    default boolean assertMatchesRegex(String regex) {
        if(matchesRegex(regex)) {
            return true;
        }
        Model item = getSelf();
        throw new AssertionError(format("Element '%s' does not match regex: %s", item.path(), regex));
    }
    default ModelElement click() {
        volatileElement( (i,e) -> e.click() );
        getSelf().maybeDelay();
        return this;
    }
    default ModelElement setText( CharSequence... keys) {
        getWebElement().sendKeys( Keys.chord(Keys.CONTROL, "a"));
        getWebElement().sendKeys( Keys.DELETE);
        getWebElement().sendKeys( keys);
        getSelf().maybeDelay();
        return this;
    }
    default ModelElement tab() {
        getWebElement().sendKeys( Keys.TAB);
        getSelf().maybeDelay();
        return this;
    }
    default ModelElement enter() {
        getWebElement().sendKeys( Keys.ENTER);
        getSelf().maybeDelay();
        return this;
    }
    default ModelElement selectByText(String text) {
        volatileElement( (i,e) -> new Select(e).selectByVisibleText( text ) );
        getSelf().maybeDelay();
        return this;
    }
    default ModelElement selectByValue(String text) {
        volatileElement( (i,e) -> new Select(e).selectByValue( text ) );
        getSelf().maybeDelay();
        return this;
    }
    default ModelElement selectByIndex(int index) {
        volatileElement( (i,e) -> new Select(e).selectByIndex( index ) );
        getSelf().maybeDelay();
        return this;
    }

    default ModelElement setAttribute(String key, Object value) {
        String script = format("arguments[0].setAttribute( '%s', '%s' )", key, value);
        ((JavascriptExecutor)getWebDriver()).executeScript( script, getWebElement() );
        return this;
    }
}
