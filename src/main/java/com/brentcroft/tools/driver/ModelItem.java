package com.brentcroft.tools.driver;

import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;
import com.brentcroft.tools.model.AbstractModelItem;
import com.brentcroft.tools.model.Model;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class ModelItem extends AbstractModelItem implements ModelElement
{
    private static final JstlTemplateManager jstl = new JstlTemplateManager();
    private static final ThreadLocal< Stack<Map<String, Object>> > scopeStack = ThreadLocal.withInitial( Stack::new );

    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }

    @Override
    public Map<String, Object> newContainer() {
        MapBindings bindings = new MapBindings(this);
        bindings.put( "$local", bindings );
        bindings.put( "$self", this );
        bindings.put( "$parent", getParent() );
        bindings.put( "$static", getStaticModel() );
        return bindings;
    }

    @Override
    public Expander getExpander()
    {
        return jstl::expandText;
    }

    @Override
    public Evaluator getEvaluator()
    {
        return jstl::eval;
    }

    @Override
    public WebDriver getWebDriver()
    {
        return getBrowser().getWebDriver();
    }

    @Override
    public Browser getBrowser() {
        return Optional
                .ofNullable(getRoot())
                // the root should have overridden
                // this method to supply an actual Browser
                .filter( p -> p != this )
                .filter( p -> p instanceof ModelItem )
                .map( p -> ((ModelItem)p).getBrowser())
                .orElseThrow(() -> new IllegalArgumentException("No WebDriver available!"));
    }

    @Override
    public Map<String, Object> getCurrentScope()
    {
        return scopeStack.get().empty()
               ? newContainer()
               : ((MapBindings)newContainer())
                       .withParent( scopeStack.get().peek() );
    }

    @Override
    public void newCurrentScope() {
        scopeStack.get().push( getCurrentScope() );
    }

    @Override
    public void dropCurrentScope() {
        if (! scopeStack.get().empty()) {
            scopeStack.get().pop();
        }
    }
}
