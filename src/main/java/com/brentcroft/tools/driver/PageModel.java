package com.brentcroft.tools.driver;

import lombok.Getter;
import lombok.Setter;

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
}
