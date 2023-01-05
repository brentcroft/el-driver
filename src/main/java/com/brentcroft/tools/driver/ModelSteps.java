package com.brentcroft.tools.driver;

import com.brentcroft.tools.model.Model;

import java.util.Optional;

import static java.lang.String.format;

public class ModelSteps implements Runnable
{
    private final String steps;
    private final Model parent;

    public ModelSteps( Model model )
    {
        this.parent = Optional
                .ofNullable( model.getParent() )
                .filter( p -> p instanceof Model )
                .map( p -> (Model)p )
                .orElseThrow(() -> new IllegalArgumentException(format("Item [%s] parent is not a Model", model.path() ) ) );

        this.steps = Optional
                .ofNullable(model.get("$steps"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(format("Item [%s] has no value for $steps", model.path())));
    }

    public void run()
    {
        String expandedSteps = parent.expand( steps );
        parent.eval( expandedSteps );
    }
}
