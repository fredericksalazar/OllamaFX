package com.org.ollamafx;

import atlantafx.base.util.BBCodeParser;
import javafx.scene.Node;

public class BBCodeCheck {
    public void check() {
        // This line will fail compilation if BBCodeParser is missing or
        // createFormattedText is missing
        Node node = BBCodeParser.createFormattedText("test");
    }
}
