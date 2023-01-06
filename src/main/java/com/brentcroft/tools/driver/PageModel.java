package com.brentcroft.tools.driver;

public class PageModel extends ModelItem
{
    public PageModel() {
        setName("root");
    }
    public PageModel(String siteJson) {
        this();
        appendFromJson( siteJson );
    }
}
