package com.brentcroft.tools.cucumber;

import io.cucumber.core.feature.FeatureParser;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.runtime.FeaturePathFeatureSupplier;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class FeatureTree extends JTree
{
    public FeatureTree() {
        super(new DefaultMutableTreeNode("Features"));
        //setCellRenderer(new FeatureTreeCellRenderer());
        setRootVisible( true );
        setMinimumSize( new Dimension(500, 400 ) );
    }

    public void scanForFeatures( File rootDirectory )
    {
        FeaturePathFeatureSupplier fpfs = new FeaturePathFeatureSupplier(
                () -> Thread.currentThread().getContextClassLoader(),
                () -> Collections.singletonList( rootDirectory.toURI() ),
                new FeatureParser( UUID::randomUUID ) );

        SwingUtilities.invokeLater(() -> addFeaturesToTree( fpfs.get()) );
    }

    private void addFeaturesToTree( List< Feature > features )
    {
        buildFeatureNodes(features, ( DefaultMutableTreeNode ) getModel().getRoot());
        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
    }

    private void buildFeatureNodes( List< Feature> features, DefaultMutableTreeNode parent )
    {
        features.forEach( feature -> parent.add( new FeatureNode( feature ) ) );
    }

    static class FeatureTreeCellRenderer implements TreeCellRenderer
    {
        private final JLabel label;

        FeatureTreeCellRenderer() {
            label = new JLabel();
        }

        public Component getTreeCellRendererComponent( JTree tree, Object value, boolean selected, boolean expanded,
                                                       boolean leaf, int row, boolean hasFocus) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof FeatureNode ) {
                FeatureNode modelNode = ( FeatureNode )userObject;
                label.setIcon(modelNode.getIcon());
                label.setText(modelNode.toString());
            } else {
                label.setIcon(null);
                label.setText(value.toString());
            }
            return label;
        }
    }

}
