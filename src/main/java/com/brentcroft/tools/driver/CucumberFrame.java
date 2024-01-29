package com.brentcroft.tools.cucumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;
import java.util.Properties;


public class CucumberFrame  extends JFrame implements TreeSelectionListener
{
    private static Logger log = LoggerFactory.getLogger( CucumberFrame.class );

    private final JSplitPane topPanel;

    private final FeatureTabs featureTabs;

    private final FeatureTree featureTree;

    public CucumberFrame() {
        setTitle("Cucumber Test Runner");
        setSize(1200, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        topPanel = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );

        featureTree = new FeatureTree();
        featureTree.addTreeSelectionListener( this );

        topPanel.setLeftComponent( new JScrollPane( featureTree ) );

        featureTabs = new FeatureTabs();
        topPanel.setRightComponent( featureTabs );

        add( topPanel );


        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
//        ImageIcon fileIcon = new ImageIcon("file.png");
//        fileMenu.setIcon(fileIcon);
        JMenuItem openMenuItem = new JMenuItem("Open");
        JMenuItem newMenuItem = new JMenuItem("New");
        openMenuItem.addActionListener( featureTabs );
        newMenuItem.addActionListener( featureTabs );
        fileMenu.add(newMenuItem);
        fileMenu.add(openMenuItem);
        menuBar.add(fileMenu);

        JMenu viewMenu = new JMenu( "View" );
//        ImageIcon viewIcon = new ImageIcon("view.png");
//        viewMenu.setIcon(viewIcon);
        menuBar.add( viewMenu );

        JMenu editMenu = new JMenu("Edit");
//        ImageIcon editIcon = new ImageIcon("edit.png");
//        editMenu.setIcon(editIcon);
        JMenuItem increaseFontSizeMenuItem = new JMenuItem("Increase Font Size");
        JMenuItem decreaseFontSizeMenuItem = new JMenuItem("Decrease Font Size");
        increaseFontSizeMenuItem.addActionListener( featureTabs );
        decreaseFontSizeMenuItem.addActionListener( featureTabs );
        editMenu.add(increaseFontSizeMenuItem);
        editMenu.add(decreaseFontSizeMenuItem);
        menuBar.add(editMenu);
        setJMenuBar(menuBar);
    }

    private void customizeUI() {
        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
            SwingUtilities.updateComponentTreeUI(this);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {

        // args[0] may be a properties file
        Properties initialProperties = Optional
                .ofNullable(args)
                .filter( a -> a.length > 0 )
                .map( a -> a[0].trim() )
                .filter( a -> !a.isEmpty() )
                .map( a -> {
                    Properties p = new Properties();
                    try ( FileInputStream fis = new FileInputStream( a ) ) {
                        p.load( fis );
                    } catch (Exception e) {
                        // failed loading properties
                        log.warn( "Failed to load properties file: " + a, e );
                    }
                    return p;
                } )
                .orElseGet( Properties::new );

        SwingUtilities.invokeLater(() -> {
            CucumberFrame cucumberFrame = new CucumberFrame();
            cucumberFrame.customizeUI();
            cucumberFrame.setVisible(true);

            SwingUtilities.invokeLater(() -> cucumberFrame.scanForFeatures(new File("src/test/resources")) );
        });
    }

    private void scanForFeatures( File file ) {
        featureTree.scanForFeatures( file );
        topPanel.setDividerLocation( 0.2 );
    }



    @Override
    public void valueChanged( TreeSelectionEvent e )
    {
        DefaultMutableTreeNode node = ( DefaultMutableTreeNode ) featureTree.getLastSelectedPathComponent();
        if ( node instanceof FeatureNode )
        {
            FeatureNode featureNode = ( FeatureNode ) node;
            featureTabs.addFeatureTab( featureNode.getFeature() );
        }
    }

}
