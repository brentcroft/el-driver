package com.brentcroft.tools.driver;

import com.brentcroft.tools.el.ELTemplateManager;
import com.brentcroft.tools.model.ModelInspectorDialog;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BrowserELFunctions
{
    public static void install( ELTemplateManager el ) throws NoSuchMethodException
    {
        el.mapFunction(
                        "inspect",
                        BrowserELFunctions.class.getMethod("inspect", Map.class, String.class ) );

        el.mapFunction(
                        "nextWorkingDay",
                        BrowserELFunctions.class.getMethod("nextWorkingDay", LocalDateTime.class) );

        el.mapFunction(
                        "millisBetween",
                        BrowserELFunctions.class.getMethod("millisBetween", LocalDateTime.class, LocalDateTime.class) );

        el.mapFunction(
                        "dateRange",
                        BrowserELFunctions.class.getMethod("dateRange", LocalDateTime.class, LocalDateTime.class) );
    }

    public static void inspect( Map< String, ? > model, String steps) {
        ModelInspectorDialog inspector = new ModelInspectorDialog( model );
        inspector.setSteps( steps );
        inspector.setModal( true );
        inspector.setVisible( true );
    }

    public static long millisBetween(LocalDateTime earlier, LocalDateTime later) {
        return earlier.until( later, ChronoUnit.MILLIS );
    }

    public static LocalDateTime nextWorkingDay(LocalDateTime candidate) {
        if (candidate.getHour() < 9) {
            candidate = candidate.plus( 9 - candidate.getHour(), ChronoUnit.HOURS );
        } else if (candidate.getHour() > 18) {
            candidate = candidate.plus( 24 - candidate.getHour() - 9, ChronoUnit.HOURS );
        }
        switch (candidate.getDayOfWeek()) {
            case SATURDAY:
                candidate = candidate.plus( 2, ChronoUnit.DAYS );
                break;
            case SUNDAY:
                candidate = candidate.plus( 1, ChronoUnit.DAYS );
                break;
            default:
        }
        return candidate;
    }

    public static boolean isWorkingDay(LocalDateTime candidate) {
        switch (candidate.getDayOfWeek()) {
            case SATURDAY:
            case SUNDAY:
                return false;
            default:
                return true;
        }
    }

    public static List< LocalDateTime > dateRange(LocalDateTime from, LocalDateTime to) {
        List< LocalDateTime > dates = new ArrayList<>();
        while (!from.isAfter( to )) {
            if (isWorkingDay(from)) {
                dates.add( from );
            }
            from = from.plusDays( 1 );
        }
        return dates;
    }
}
