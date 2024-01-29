package com.brentcroft.tools.cucumber;

import javax.swing.*;
import javax.swing.text.StyledDocument;

public interface Highlighter
{
    void highlight( JTextPane textPane, StyledDocument doc );

    static Highlighter gherkinHighlighter()
    {
        return GherkinSyntaxHighlighter::highlight;
    }
}
