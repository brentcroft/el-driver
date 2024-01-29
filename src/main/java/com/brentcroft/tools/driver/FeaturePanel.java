package com.brentcroft.tools.cucumber;

import io.cucumber.core.gherkin.Feature;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class FeaturePanel extends JPanel implements ActionListener
{
    @Getter
    private File file;
    @Getter
    @Setter
    private Feature feature;
    private final JTextPane featureTextPane;
    private final UndoManager undoManager = new UndoManager();
    private final JButton runButton;
    private final JButton saveButton;
    private final JButton closeButton;
    private final JButton formatButton;

    private final JToolBar toolbarPanel;

    private final JPanel resultsPanel;
    private final JTextArea resultText;

    private final Highlighter highlighter;

    public FeaturePanel(File file) {
        this();
        this.file = file;
        loadContent(file);
    }
    public FeaturePanel(Feature feature) {
        this();
        this.feature = feature;
        this.file = FeatureNode.getFeatureFile(feature);
        loadContent(feature);
    }

    public FeaturePanel() {
        super(new BorderLayout());
        this.highlighter = GherkinSyntaxHighlighter::highlight;

        featureTextPane = new JTextPane();
        featureTextPane.getDocument().addUndoableEditListener(undoManager);

        featureTextPane.addKeyListener( new KeyAdapter() {
            @Override
            public void keyPressed( KeyEvent e )
            {
                if (e.isControlDown()) {
                    System.out.println("Key code: " + e.getKeyCode());
                    if (e.getKeyCode() == KeyEvent.VK_Z) {
                        undoManager.undo();
                    } else if (e.getKeyChar() == KeyEvent.VK_Y) {
                        undoManager.redo();
                    }
                }
            }
        } );

        JScrollPane scrollPane = new JScrollPane( featureTextPane );

        runButton = new JButton( "Run" );
        runButton.addActionListener( this );

        saveButton = new JButton( "Save" );
        saveButton.addActionListener( this );

        closeButton = new JButton( "Close" );
        // close is attached on FeatureTabs

        formatButton = new JButton( "Format" );
        formatButton.addActionListener( this );


        toolbarPanel = new JToolBar( );
        toolbarPanel.add( saveButton );
        toolbarPanel.add( closeButton );

        toolbarPanel.addSeparator();
        toolbarPanel.add( formatButton );
        toolbarPanel.addSeparator();

        toolbarPanel.add( runButton );




        add( toolbarPanel, BorderLayout.NORTH );

        resultsPanel = new JPanel( new BorderLayout() );
        JLabel resultLabel = new JLabel( "Result:" );
        resultsPanel.add( resultLabel, BorderLayout.NORTH );
        resultText = new JTextArea();
        resultsPanel.add( new JScrollPane( resultText ), BorderLayout.CENTER );

        JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
        splitPane.setTopComponent( scrollPane );
        splitPane.setBottomComponent( resultsPanel );

        add( splitPane, BorderLayout.CENTER );

        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation( 0.8 ) );
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    private void clearResultsPanel() {
        resultText.setText( "" );
    }

    public void loadContent(Feature feature)
    {
        try {
            featureTextPane.setText( feature.getSource() );
            highlight();
        } catch (Exception e) {
            resultText.append( e.toString() );
        }
    }

    public void loadContent(String featureText)
    {
        try {
            featureTextPane.setText( featureText );

            highlight();

            SwingUtilities.invokeLater( () -> new FeatureParser()
                    .parseFeature( getFeatureTextPane().getText() )
                    .ifPresent( this::setFeature ) );
        } catch (Exception e) {
            resultText.append( e.toString() );
        }
    }

    public void loadContent(File file)
    {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            StyledDocument doc = featureTextPane.getStyledDocument();

            String line;
            while ((line = reader.readLine()) != null) {
                doc.insertString(doc.getLength(), line + "\n", null);
            }
            reader.close();

            highlight();

            SwingUtilities.invokeLater( () -> new FeatureParser()
                    .parseFeature( getFeatureTextPane().getText() )
                    .ifPresent( this::setFeature ) );

        } catch (Exception e) {
            resultText.append( e.toString() );
        }
    }

//    public void setText(String text) {
//        highlight();
//
//        SwingUtilities.invokeLater( () -> new FeatureParser()
//                .parseFeature( getFeatureTextPane().getText() )
//                .ifPresent( this::setFeature ) );
//    }

    public void highlight() {
        highlighter.highlight(featureTextPane, featureTextPane.getStyledDocument());
    }


    public void save() {
        if (file == null) {
            JFileChooser fileChooser = new JFileChooser();
            int returnVal = fileChooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
            } else {
                return;
            }
        }
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(this.featureTextPane.getText());
            fileWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {
        if (saveButton.equals( e.getSource() ) ) {
            SwingUtilities.invokeLater( this::save );
        } else if (formatButton.equals( e.getSource() ) ) {
            highlight();
        } else if (runButton.equals( e.getSource() ) ) {
            SwingUtilities.invokeLater( () -> {
                clearResultsPanel();
                TextComponentOutputStream tos = new TextComponentOutputStream(resultText);

                PrintStream oldOut = System.out;

                try {
                    System.setOut( tos.getPrintStream() );

                    Cucumber cucumber = new Cucumber( tos );
                    boolean ok = cucumber.runFeature( featureTextPane.getText() );

                    tos.println( "OK: " + ok );
                } catch (Exception ex) {
                    String msg = ex + "\n    " +
                        Stream
                                .of(ex.getStackTrace())
                                .map( StackTraceElement::toString )
                                .collect( Collectors.joining("\n    "));
                    resultText.setText( msg );
                } finally {
                    System.setOut( oldOut );
                }
            } );
        }
    }

    public boolean hasFeature( Feature feature )
    {
        return Optional
                .ofNullable(this.feature)
                .map( f -> f.equals( feature ) )
                .orElse( false );
    }

    static class TextComponentOutputStream extends OutputStream
    {
        private final JTextArea textPane;
        private final CharArrayWriter currentLine = new CharArrayWriter();

        @Getter
        private final PrintStream printStream = new PrintStream(this);

        public TextComponentOutputStream( JTextArea textPane) {
            this.textPane = textPane;
        }

        public void println(String text) throws IOException
        {
            write( text.getBytes( StandardCharsets.UTF_8 ) );
            write('\n');
            flush();
        }

        public void write(int b) {
            if ( b == 10 || b == 13) {
                flush();
            } else {
                currentLine.write( b );
            }
        }

        public void flush() {
            currentLine.flush();
            String newText = currentLine.toString();
            currentLine.reset();
            if (newText.isEmpty()) {
                return;
            }
            SwingUtilities.invokeLater( () -> {
                int p = newText.indexOf( '#' );
                if ( p < 0 ) {
                    textPane.append( newText + "\n" ) ;
                } else {
                    textPane.append( newText.substring( 0, p-1 )  + "\n") ;
                }
            });
        }
    }
}
