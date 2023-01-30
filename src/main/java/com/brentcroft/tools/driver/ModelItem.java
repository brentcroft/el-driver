package com.brentcroft.tools.driver;

import com.brentcroft.tools.el.ELTemplateManager;
import com.brentcroft.tools.el.Parented;
import com.brentcroft.tools.el.SimpleMapELResolver;
import com.brentcroft.tools.el.ThreadLocalStackELResolver;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;
import com.brentcroft.tools.model.AbstractModelItem;
import com.brentcroft.tools.model.Model;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.Optional;

public class ModelItem extends AbstractModelItem implements ModelElement, Parented
{
    private static final JstlTemplateManager jstl = new JstlTemplateManager();

    static
    {
        try
        {
            ELTemplateManager em = jstl.getELTemplateManager();
            em.addPrimaryResolvers( new ThreadLocalStackELResolver( em, em, AbstractModelItem.scopeStack ) );
            em.addSecondaryResolvers( new SimpleMapELResolver( AbstractModelItem.staticModel ) );
            BrowserELFunctions.install( em );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    @Override
    public Class< ? extends Model > getModelClass()
    {
        return ModelItem.class;
    }

    @Override
    public Map< String, Object > newContainer()
    {
        MapBindings bindings = new MapBindings( this );
        bindings.put( "$local", getScopeStack().peek() );
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
    public Browser getBrowser()
    {
        return Optional
                .ofNullable( getRoot() )
                // the root should have overridden
                // this method to supply an actual Browser
                .filter( p -> p != this )
                .filter( p -> p instanceof ModelItem )
                .map( p -> ( ( ModelItem ) p ).getBrowser() )
                .orElseThrow( () -> new IllegalArgumentException( "No WebDriver available!" ) );
    }
}
