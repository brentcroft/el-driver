package com.brentcroft.tools.driver;

import com.brentcroft.tools.el.*;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.jstl.MapBindings;
import com.brentcroft.tools.model.AbstractModelItem;
import com.brentcroft.tools.model.Model;
import jakarta.el.ImportHandler;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.Optional;

public class ModelItem extends AbstractModelItem implements ModelElement, Parented
{
    protected static final JstlTemplateManager jstl = new JstlTemplateManager();

    static
    {
        try
        {
            ELTemplateManager em = jstl.getELTemplateManager();

            em.addPrimaryResolvers(
                    new ThreadLocalStackELResolver(
                            em,
                            em,
                            AbstractModelItem.scopeStack,
                            AbstractModelItem.staticModel
                    )
            );

            em.addSecondaryResolvers(
                    new ConditionalMethodsELResolver(
                            em.getELContextFactory(),
                            AbstractModelItem.scopeStack,
                            AbstractModelItem.staticModel
                    ),
                    new SimpleMapELResolver(
                            AbstractModelItem.staticModel
                    )
            );

            ImportHandler ih = em
                    .getELContextFactory()
                    .getImportHandler();

            ih.importClass( Keys.class.getTypeName() );
            ih.importClass( Point.class.getTypeName() );
            ih.importClass( Dimension.class.getTypeName() );

            ih.importClass( Browser.class.getTypeName() );
            ih.importClass( Browsers.class.getTypeName() );

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

    public ELTemplateManager getELTemplateManager() {
        return jstl.getELTemplateManager();
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
