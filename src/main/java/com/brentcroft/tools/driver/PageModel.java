package com.brentcroft.tools.driver;

import lombok.Getter;
import lombok.Setter;

import static java.lang.String.format;

@Getter
@Setter
public class PageModel extends ModelItem
{
    private Browser browser;

    public PageModel() {
        setName("root");
    }

    public PageModel(String siteJson) {
        this();
        appendFromJson( siteJson );
    }

    public void loadFromFile(String siteFile) {
        if (siteFile.endsWith( ".json" )) {
            appendFromJson(format("{'$json': '%s'}", siteFile));
        }
        else if (siteFile.endsWith( ".xml" )) {
            appendFromJson(format("{'$xml': '%s'}", siteFile));
        }
        else
        {
            throw new IllegalArgumentException(format("Site file does not have a JSON or XML extension: %s", siteFile));
        }
    }

    public void openFromFile(String siteFile) {
        loadFromFile( siteFile );
        browser.open();
    }
}
