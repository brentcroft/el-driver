package com.brentcroft.tools.cucumber;

import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.gherkin.FeatureParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

import static java.util.Comparator.comparing;

public class FeatureParser
{

    private final Supplier< UUID > idGenerator;

    public FeatureParser() {
        this(UUID::randomUUID);
    }

    public FeatureParser( Supplier< UUID > idGenerator )
    {
        this.idGenerator = idGenerator;
    }

    public Optional< Feature > parseFeature( String featureText )
    {

        URI uri = URI.create( "current-feature" );

        ServiceLoader< io.cucumber.core.gherkin.FeatureParser > services = ServiceLoader
                .load( io.cucumber.core.gherkin.FeatureParser.class );
        Iterator< io.cucumber.core.gherkin.FeatureParser > iterator = services.iterator();
        List< io.cucumber.core.gherkin.FeatureParser > parser = new ArrayList<>();
        while ( iterator.hasNext() )
        {
            parser.add( iterator.next() );
        }
        Comparator< io.cucumber.core.gherkin.FeatureParser > version = comparing(
                io.cucumber.core.gherkin.FeatureParser::version );

        try ( InputStream source = new ByteArrayInputStream( featureText.getBytes() ) )
        {
            return Collections
                    .max( parser, version )
                    .parse( uri, source, idGenerator );

        }
        catch ( IOException e )
        {
            throw new FeatureParserException( "Failed to parse resource at: " + uri, e );
        }
    }
}
