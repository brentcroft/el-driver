package com.brentcroft.tools.cucumber;

import io.cucumber.core.gherkin.Feature;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FeatureTabs extends JPanel implements ActionListener {
    private final JTabbedPane tabbedPane;
    private final JFileChooser fileChooser;

    public FeatureTabs() {
        super(new BorderLayout());
        tabbedPane = new JTabbedPane();
        add(tabbedPane);
        fileChooser = new JFileChooser();
    }

    private void saveCurrentTabContentAs() {
        getCurrentFeaturePanel().ifPresent( FeaturePanel::save );
    }

    private void increaseFontSize(JTextPane textPane) {
        Font currentFont = textPane.getFont();
        int newSize = currentFont.getSize() + 2;
        Font newFont = currentFont.deriveFont((float) newSize);
        textPane.setFont(newFont);
    }

    private void decreaseFontSize(JTextPane textPane) {
        Font currentFont = textPane.getFont();
        int newSize = Math.max(currentFont.getSize() - 2, 10);
        Font newFont = currentFont.deriveFont((float) newSize);
        textPane.setFont(newFont);
    }

    private void closeCurrentTab() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1) {
            tabbedPane.removeTabAt(selectedIndex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getActionCommand().equals("New")) {
            FeaturePanel featureTab = new FeaturePanel();
            featureTab.getCloseButton().addActionListener( this );
            featureTab.loadContent( "Feature: example\n\n  Scenario: example" );
            addFeature( "Untitled", featureTab );

        } else if (e.getActionCommand().equals("Close")) {
            closeCurrentTab();

        } else if (e.getActionCommand().equals("Open")) {
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {

                File selectedFile = fileChooser.getSelectedFile();
                FeaturePanel featureTab = new FeaturePanel(selectedFile);
                featureTab.getCloseButton().addActionListener( this );
                tabbedPane.addTab(selectedFile.getName(), featureTab);

            }
        } else if (e.getActionCommand().equals("Save")) {
            getCurrentFeaturePanel().ifPresent( FeaturePanel::save );

        } else if (e.getActionCommand().equals("Increase Font Size")) {
            getCurrentFeaturePane().ifPresent( this::increaseFontSize );

        } else if (e.getActionCommand().equals("Decrease Font Size")) {
            getCurrentFeaturePane().ifPresent( this::decreaseFontSize );
        }
    }

    public Optional< JTextPane> getCurrentFeaturePane()
    {
        return getCurrentFeaturePanel()
                .map( FeaturePanel::getFeatureTextPane );
    }

    public Optional< FeaturePanel> getFeaturePanel(int i)
    {
        if (i >= tabbedPane.getTabCount()) {
            return Optional.empty();
        } else {
            return Optional.of( (FeaturePanel) tabbedPane.getComponentAt(i));
        }
    }

    public Optional< FeaturePanel> getCurrentFeaturePanel()
    {
        int index = tabbedPane.getSelectedIndex();
        if (index < 0) {
            return Optional.empty();
        } else {
            return Optional.of( (FeaturePanel) tabbedPane.getComponentAt(index));
        }
    }

    public void addFeature( String title, FeaturePanel featureTab )
    {
        featureTab.getSaveButton().addActionListener( this );
        tabbedPane.addTab(title, featureTab);
        tabbedPane.setSelectedComponent( featureTab );
    }


    public void addFeatureTab( Feature feature )
    {
        int existingTab = IntStream
                .range( 0,  tabbedPane.getTabCount())
                        .filter( i -> getFeaturePanel(i)
                                .filter( fp -> fp.hasFeature(feature) )
                                .isPresent() )
                        .findFirst()
                                .orElse( -1 );
        if (existingTab < 0) {
            addFeature(
                    FeatureNode.getFeatureName(feature),
                    new FeaturePanel(feature));
        } else {
            tabbedPane.setSelectedIndex( existingTab );
        }
    }
}

class SyntaxHighlighter {
    public static void setTextColor(StyledDocument doc, int start, int length, Color color) {
        Style style = doc.addStyle("TextColor", null);
        StyleConstants.setForeground(style, color);
        doc.setCharacterAttributes(start, length, style, false);
    }
}
//syntax highlighter without regex
class GherkinSyntaxHighlighter {

    @RequiredArgsConstructor
    @Getter
    public static class StylePair {
        private final String styleName;
        private final Color color;
        private final String regex;
        @Getter
        @Setter
        private Style style;

        public static StylePair of(String styleName, Color color, String regex) {
            return new StylePair(styleName, color, regex);
        }
    }

    private static final StylePair[] STYLE_PAIRS = {
            StylePair.of( "KeywordStyle", Color.BLUE,"\\b(Feature:)|(Background:)|(Scenario:)|(Scenario Outline:)|(Examples:)\\b" ),
            StylePair.of( "ELStyle", Color.GREEN,"\\b(\\* EL)\\b" ),
            StylePair.of("NumberStyle", Color.MAGENTA, "\\b\\d+\\.?\\d*\\b" ),
            StylePair.of("StringStyle", Color.ORANGE, "\".*?\"" ),
            StylePair.of("StringSingleQuoteStyle", Color.PINK, "'.*?'" ),
            StylePair.of("BlockStyle", Color.RED, "\"\"\"(.|\\R)*?\"\"\"" ),
    };


    public static void highlight(JTextPane textPane, StyledDocument doc) {
        String pattern = Stream
                .of(STYLE_PAIRS)
                .peek( sp -> {
                    sp.setStyle( textPane.addStyle(sp.getStyleName(), null) );
                    StyleConstants.setForeground(sp.getStyle(),sp.getColor());
                } )
                .map( sp -> String.format( "(%s)", sp.getRegex() ) )
                .collect( Collectors.joining("|"));

        Pattern compiledPattern = Pattern.compile(pattern);

        try {
            String text = doc.getText(0, doc.getLength());
            Matcher matcher = compiledPattern.matcher(text);

            while (matcher.find()) {
                IntStream
                        .range(0, STYLE_PAIRS.length)
                        .filter( i -> matcher.group(i) != null )
                        .findFirst()
                        .ifPresent( i -> {
                            doc.setCharacterAttributes(
                                    matcher.start(i),
                                    matcher.end(i) - matcher.start(i),
                                    STYLE_PAIRS[i].getStyle(),
                                    true);
                        } );
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}


class JavaSyntaxHighlighter {
    public static void highlight(JTextPane textPane, StyledDocument doc) {
        // Define syntax highlighting styles for Java
        Style defaultStyle = textPane.getStyle(StyleContext.DEFAULT_STYLE);
        Style keywordStyle = textPane.addStyle("KeywordStyle", defaultStyle);
        StyleConstants.setForeground(keywordStyle, Color.BLUE);

        // Apply styles to keywords
        String[] keywords = {
                "abstract", "boolean", "break", "class", "extends", "for", "if", "new", "return",
                "while", "public", "private", "static", "void", "int", "double"
        };

        for (String keyword : keywords) {
            String text = textPane.getText();
            int pos = 0;

            while ((pos = text.indexOf(keyword, pos)) >= 0) {
                SyntaxHighlighter.setTextColor(doc, pos, keyword.length(), Color.BLUE);
                pos += keyword.length();
            }
        }
    }
}
//Regex to match syntax and highlight
class CSyntaxHighlighter {
    public static void highlight(JTextPane textPane, StyledDocument doc) {
        // Define syntax highlighting patterns
        String keywords = "\\b(int|char|float|double|long|short|void|if|else|while|for|switch|case|break|return)\\b";
        String comments = "//.*|/\\*(.|\\R)*?\\*/ ";
        String dataTypes = "\\b([A-Za-z_]\\w*)\\b";
        String macros = "#\\w+";
        String numbers = "\\b\\d+\\.?\\d*\\b";
        String strings = "\".*?\"";
        String functions = "\\b[A-Za-z_]\\w*\\s*\\(";

        // Set font style attributes for each type
        Style keywordStyle = textPane.addStyle("KeywordStyle", null);
        StyleConstants.setForeground(keywordStyle, Color.BLUE);

        Style commentStyle = textPane.addStyle("CommentStyle", null);
        StyleConstants.setForeground(commentStyle, Color.GREEN);

        Style dataTypeStyle = textPane.addStyle("DataTypeStyle", null);
        StyleConstants.setForeground(dataTypeStyle, Color.BLUE);

        Style macroStyle = textPane.addStyle("MacroStyle", null);
        StyleConstants.setForeground(macroStyle, Color.BLUE);

        Style numberStyle = textPane.addStyle("NumberStyle", null);
        StyleConstants.setForeground(numberStyle, Color.MAGENTA);

        Style stringStyle = textPane.addStyle("StringStyle", null);
        StyleConstants.setForeground(stringStyle, Color.MAGENTA);

        Style functionStyle = textPane.addStyle("FunctionStyle", null);
        StyleConstants.setForeground(functionStyle, Color.YELLOW);

        // Combine patterns into a single pattern
        String pattern = String.format("(%s)|(%s)|(%s)|(%s)|(%s)|(%s)|(%s)",
                keywords, comments, dataTypes, macros, numbers, strings, functions);

        Pattern compiledPattern = Pattern.compile(pattern);

        try {
            String text = doc.getText(0, doc.getLength());
            Matcher matcher = compiledPattern.matcher(text);

            while (matcher.find()) {
                if (matcher.group(1) != null) {
                    doc.setCharacterAttributes(matcher.start(1), matcher.end(1) - matcher.start(1), keywordStyle, false);
                } else if (matcher.group(2) != null) {
                    doc.setCharacterAttributes(matcher.start(2), matcher.end(2) - matcher.start(2), commentStyle, false);
                } else if (matcher.group(3) != null) {
                    doc.setCharacterAttributes(matcher.start(3), matcher.end(3) - matcher.start(3), dataTypeStyle, false);
                } else if (matcher.group(4) != null) {
                    doc.setCharacterAttributes(matcher.start(4), matcher.end(4) - matcher.start(4), macroStyle, false);
                } else if (matcher.group(5) != null) {
                    doc.setCharacterAttributes(matcher.start(5), matcher.end(5) - matcher.start(5), numberStyle, false);
                } else if (matcher.group(6) != null) {
                    doc.setCharacterAttributes(matcher.start(6), matcher.end(6) - matcher.start(6), stringStyle, false);
                } else if (matcher.group(7) != null) {
                    doc.setCharacterAttributes(matcher.start(7), matcher.end(7) - matcher.start(7), functionStyle, false);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}