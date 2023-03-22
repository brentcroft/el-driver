package com.brentcroft.tools.driver;

import lombok.Getter;

import static java.lang.String.format;

@Getter
public class VolatileElementException extends RuntimeException
{
    private final ModelElement modelElement;
    private final int volatileRetries;

    public VolatileElementException( ModelElement modelElement, int volatileRetries )
    {
        super(format("Stale element [%s] after %s retries.", modelElement, volatileRetries));
        this.modelElement = modelElement;
        this.volatileRetries = volatileRetries;
    }
}
