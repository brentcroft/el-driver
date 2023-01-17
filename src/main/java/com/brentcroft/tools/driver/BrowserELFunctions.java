package com.brentcroft.tools.driver;

import com.brentcroft.tools.jstl.JstlTemplateManager;
import com.brentcroft.tools.model.InteractiveFrame;
import com.brentcroft.tools.model.Model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class BrowserELFunctions
{
    public static void install( JstlTemplateManager jstl ) throws NoSuchMethodException
    {
        jstl.getELTemplateManager()
                .mapFunction(
                        "interact",
                        BrowserELFunctions.class.getMethod("interact", Model.class, String.class ) );

        jstl.getELTemplateManager()
                .mapFunction(
                        "delay",
                        BrowserELFunctions.class.getMethod("delay", long.class ) );
    }

    public static void interact(Model model, String steps) {
            InteractiveFrame frame = new InteractiveFrame( model );
            frame.setSteps( steps );
            frame.setModal( true );
            frame.setVisible( true );
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
