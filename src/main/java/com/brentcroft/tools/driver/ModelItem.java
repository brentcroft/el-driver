package com.brentcroft.tools.driver;

import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;
import com.brentcroft.tools.model.AbstractModelItem;
import com.brentcroft.tools.model.Model;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class ModelItem extends AbstractModelItem implements ModelElement
{
    private static BiFunction<String, Map<String,Object>, String> expander;
    private static BiFunction<String, Map<String,Object>, Object> evaluator;

    static {
        JstlTemplateManager jstl = new JstlTemplateManager();
        ModelItem.expander = jstl::expandText;
        ModelItem.evaluator = jstl::eval;
    }

    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }

    /**
     * Expands a value using the expander
     * or else just returns the value.
     *
     * @param value the value to be expanded
     * @return the expanded value
     */
    @Override
    public String expand( String value )
    {
        Map<String, Object> bindings = newContainer();
        return Optional
                .ofNullable(expander)
                .map(exp -> exp.apply( value, bindings ) )
                .orElse( value );
    }
    /**
     * Evaluates a value using the evaluator
     * or else just returns the value.
     *
     * @param value the value to be evaluated
     * @return the evaluated value
     */
    @Override
    public Object eval( String value )
    {
        if (evaluator == null) {
            return null;
        }
        Map<String, Object> bindings = newContainer();
        List<String> steps = Stream
                .of(value.split( "\\s*[;\\n\\r]+\\s*" ))
                .map( String::trim )
                .filter( v -> !v.isEmpty() && !v.startsWith( "#" ) )
                .collect( Collectors.toList());
        Object[] lastResult = {null};
        steps.forEach( step -> {
            lastResult[0] = evaluator.apply( step, bindings );
        });
        return lastResult[0];
    }

    private Map<String, Object> newContainer() {
        MapBindings bindings = new MapBindings(this);
        bindings.put( "$self", getSelf() );
        bindings.put( "$parent", getParent() );
        return bindings;
    }

    @Override
    public WebDriver getWebDriver()
    {
        return (WebDriver) getRoot().get( "$driver" );
    }
}
