package com.brentcroft.tools.cucumber;

import io.cucumber.core.options.CommandlineOptionsParser;
import io.cucumber.core.options.CucumberProperties;
import io.cucumber.core.options.CucumberPropertiesParser;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.plugin.PrettyFormatter;
import io.cucumber.core.runtime.Runtime;

import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;


public class Cucumber
{
    private final OutputStream outputStream;

    public Cucumber( OutputStream outputStream )
    {
        this.outputStream = outputStream;
    }

    public boolean runFeature( String featureText) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        RuntimeOptions propertiesFileOptions = (new CucumberPropertiesParser()).parse( CucumberProperties.fromPropertiesFile()).build();
        RuntimeOptions environmentOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromEnvironment()).build(propertiesFileOptions);
        RuntimeOptions systemOptions = (new CucumberPropertiesParser()).parse(CucumberProperties.fromSystemProperties()).build(environmentOptions);
        CommandlineOptionsParser commandlineOptionsParser = new CommandlineOptionsParser(System.out);

        String[] argv = {};

        RuntimeOptions runtimeOptions = commandlineOptionsParser
                .parse(argv)
                .addGlue( URI.create("classpath:com.brentcroft.tools.cucumber") )
                .setMonochrome(true)
                .setPublish( false )
                .addDefaultGlueIfAbsent()
                .addDefaultSummaryPrinterIfNotDisabled()
                .build(systemOptions);

        Runtime runtime = Runtime
                .builder()
                .withFeatureSupplier( () -> new FeatureParser( UUID::randomUUID )
                        .parseFeature( featureText )
                        .map( Collections::singletonList )
                        .orElseThrow( () -> new IllegalArgumentException("Failed to parse a feature from: " + featureText) ) )
                .withAdditionalPlugins( new PrettyFormatter(outputStream))
                .withRuntimeOptions(runtimeOptions)
                .withClassLoader(() -> classLoader )
                .build();

        runtime.run();

        return 0x0 == runtime.exitStatus();
    }
}
