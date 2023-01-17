package com.brentcroft.tools.glue;

import com.brentcroft.tools.driver.Browser;
import com.brentcroft.tools.driver.Browsers;
import io.cucumber.java.en.Given;

import static java.lang.String.format;

public class ModelSteps
{
    private final Browser browser = Browsers
            .instance()
            .getDefaultBrowser();

    @Given("^apply steps(| after| after all| before all) \"([^\"]*)\"$")
    public void apply_steps_inline(String after, String steps) {
        final Runnable action = () -> browser
                .getPageModel()
                .steps( steps );

        switch (after.trim()) {
            case "before all":
                browser.getBeforeAlls().put( steps, action );
                break;
            case "after all":
                browser.getAfterAlls().put( steps, action );
                break;
            case "after":
                browser.getAfters().put( steps, action );
                break;
            default:
                action.run();
        }
    }

    @Given("^apply steps(| after| after all| before all)$")
    public void apply_steps_multiline(String after, String steps)
    {
        apply_steps_inline( after, steps );
    }

    @Given( "site {string} is open" )
    public void site_is_open(String siteFile) {
        if (siteFile.endsWith( ".json" ))
        {
            browser.getPageModel().appendFromJson( format("{'$json': '%s'}", siteFile) );
        }
        else if (siteFile.endsWith( ".xml" ))
        {
            browser.getPageModel().appendFromJson( format("{'$xml': '%s'}", siteFile) );
        }
        else
        {
            throw new IllegalArgumentException(format("Site file does not have a JSON or XML extension: %s", siteFile));
        }
        browser.open();
    }

    @Given("^auto quit (on|off)$")
    public void set_auto_quit(String value) {
        browser.setAutoQuit( "on".equalsIgnoreCase( value ) );
    }

    @Given("^step delay (\\d)$")
    public void set_step_delay(String value) {
    }
    @Given("^reset step delay$")
    public void reset_step_delay() {
    }
}
