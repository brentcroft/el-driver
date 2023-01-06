package com.brentcroft.tools.driver;

import com.brentcroft.tools.model.Model;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static java.lang.String.format;

public interface ModelElement
{
    Model getSelf();
    WebDriver getWebDriver();

    default IndexedPath getIndexedPath() {
        return new IndexedPath(getSelf());
    }

    default String xpath() {
        return getIndexedPath().xpath();
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
        IndexedPath ipath = getIndexedPath();
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

//                String msg = format("'%s' -> frame found: %s; current=%s", item.path(), ipath.frame(), currentFrame);
//                System.out.println(msg);
            } else {
                String msg = format("'%s' -> frame not found: %s; current=%s", item.path(), ipath.frame(), currentFrame);
                System.out.println(msg);
            }
        }
    }

    default WebElement getWebElement() {
        switchFrame();
        IndexedPath ipath = getIndexedPath();
        try {
            List<WebElement> elements = getWebDriver().findElements( By.xpath( ipath.xpath() ) );
            if ( elements.size() > ipath.index()) {
                return elements.get( ipath.index() );
            }
            throw new NoSuchElementException("Insufficient elements: " + elements.size());
        } catch ( NoSuchElementException e) {
            throw new NoSuchElementException(
                    format("%s -> %s", getSelf().path(), ipath), e);
        }
    }
    default int count() {
        switchFrame();
        IndexedPath ipath = getIndexedPath();
        try {
            return getWebDriver().findElements( By.xpath( ipath.xpath() ) ).size();
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
        try {
            getWebElement();
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }
    default boolean notExists() {
        return !exists();
    }
    default boolean assertExists() {
        if(exists()) {
            return true;
        }
        throw new AssertionError("Element does not exist: " + getSelf());
    }
    default boolean assertNotExists() {
        if(!exists()) {
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
        return this;
    }
    default ModelElement setText( CharSequence... keys) {
        getWebElement().sendKeys( Keys.chord(Keys.CONTROL, "a"));
        getWebElement().sendKeys( Keys.DELETE);
        getWebElement().sendKeys( keys);
        return this;
    }
    default ModelElement selectByText(String text) {
        volatileElement( (i,e) -> new Select(e).selectByVisibleText( text ) );
        return this;
    }
    default ModelElement selectByValue(String text) {
        volatileElement( (i,e) -> new Select(e).selectByValue( text ) );
        return this;
    }
    default ModelElement selectByIndex(int index) {
        volatileElement( (i,e) -> new Select(e).selectByIndex( index ) );
        return this;
    }
}
