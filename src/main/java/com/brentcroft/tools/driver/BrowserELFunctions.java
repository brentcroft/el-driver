package com.brentcroft.tools.driver;

import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.model.Model;
import com.brentcroft.tools.model.ModelInspectorDialog;

public class BrowserELFunctions
{
    public static void install( JstlTemplateManager jstl ) throws NoSuchMethodException
    {
        jstl.getELTemplateManager()
                .mapFunction(
                        "inspect",
                        BrowserELFunctions.class.getMethod("inspect", Model.class, String.class ) );

        jstl.getELTemplateManager()
                .mapFunction(
                        "delay",
                        BrowserELFunctions.class.getMethod("delay", long.class ) );
    }

    public static void inspect(Model model, String steps) {
        ModelInspectorDialog inspector = new ModelInspectorDialog( model );
        inspector.setSteps( steps );
        inspector.setModal( true );
        inspector.setVisible( true );
    }

    public static String delay(long millis) {
        try
        {
            Thread.sleep(millis);
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
        return "OK";
    }
}
