package com.brentcroft.tools.cucumber;

import io.cucumber.core.gherkin.Feature;
import lombok.Getter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

@Getter
public class FeatureNode extends DefaultMutableTreeNode
{
    private final Feature feature;

    public FeatureNode( Feature feature )
    {
        this.feature = feature;
    }

    public String getKey()
    {
        return feature.getUri().getPath();
    }

    public String toString() {
        return getFeatureName(feature);
    }

    public Icon getIcon()
    {
        return new ImageIcon( getImageURL( "/container.png" ) );
    }

    private URL getImageURL( String url ) {
        return getClass().getResource( url );
    }

    public static String getFeatureName(Feature feature) {
        String path = feature
                .getUri()
                .getPath();
        int p = path.lastIndexOf( "/" );
        if (p < 0) {
            return path;
        }
        return path.substring( p + 1 );
    }

    public static File getFeatureFile( Feature feature) {
        return Paths.get(feature.getUri()).toFile();
    }
}
