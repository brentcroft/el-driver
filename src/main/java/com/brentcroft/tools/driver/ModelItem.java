package com.brentcroft.tools.driver;

import com.brentcroft.tools.el.*;
import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.model.AbstractModelItem;
import com.brentcroft.tools.model.Model;
import jakarta.el.ImportHandler;
import org.openqa.selenium.*;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

public class ModelItem extends AbstractModelItem implements ModelElement, Parented
{
    protected static final JstlTemplateManager jstl = new JstlTemplateManager();

    static
    {
        try
        {
            ELTemplateManager em = jstl.getELTemplateManager();

            AbstractModelItem.scopeStack = em.getELContextFactory().getScopeStack();
            AbstractModelItem.staticModel = em.getELContextFactory().getStaticModel();

            BrowserELFunctions.install( em );

            ImportHandler ih = em
                    .getELContextFactory()
                    .getImportHandler();

            ih.importClass( Keys.class.getTypeName() );
            ih.importClass( Point.class.getTypeName() );
            ih.importClass( Dimension.class.getTypeName() );

            ih.importClass( Paths.class.getTypeName() );
            ih.importClass( File.class.getTypeName() );

            ih.importClass( LocalDateTime.class.getTypeName() );
            ih.importClass( LocalDate.class.getTypeName() );
            ih.importClass( LocalTime.class.getTypeName() );

            // selenium classes
            ih.importClass( By.ByXPath.class.getTypeName() );
            ih.importClass( By.ByCssSelector.class.getTypeName() );

            ih.importClass( Browser.class.getTypeName() );
            ih.importClass( Browsers.class.getTypeName() );

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
        bindings.put( "$self", this );
        bindings.put( "$parent", getParent() );
        return bindings;
    }

    public static JstlTemplateManager getJstl() {
        return jstl;
    }

    public static ELTemplateManager getELTemplateManager() {
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
    public ELCompiler getELCompiler()
    {
        return jstl::compile;
    }

    @Override
    public WebDriver getWebDriver()
    {
        return Optional
                .ofNullable( getBrowser().getWebDriver() )
                .orElseThrow(() -> new IllegalArgumentException( format("No WebDriver available for item: %s", this)));
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
                .orElseThrow( () -> new IllegalArgumentException( format("No browser available for item: %s", this) ) );
    }
}
