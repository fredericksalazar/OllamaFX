package com.org.ollamafx.util;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

public class MarkdownRenderer {

    public static javafx.scene.Node render(String markdown) {
        Parser parser = Parser.builder().build();
        org.commonmark.node.Node document = parser.parse(markdown);
        JavaFXVisitor visitor = new JavaFXVisitor();
        document.accept(visitor);
        return visitor.getRoot();
    }

    private static class JavaFXVisitor extends AbstractVisitor {
        private final VBox root = new VBox();
        private TextFlow currentFlow;

        public JavaFXVisitor() {
            root.setSpacing(10);
            startNewFlow();
        }

        public VBox getRoot() {
            return root;
        }

        private void startNewFlow() {
            currentFlow = new TextFlow();
            root.getChildren().add(currentFlow);
        }

        @Override
        public void visit(org.commonmark.node.Text text) {
            javafx.scene.text.Text fxText = new javafx.scene.text.Text(text.getLiteral());
            fxText.getStyleClass().add("markdown-text");
            currentFlow.getChildren().add(fxText);
        }

        @Override
        public void visit(SoftLineBreak softLineBreak) {
            currentFlow.getChildren().add(new javafx.scene.text.Text("\n"));
        }

        @Override
        public void visit(HardLineBreak hardLineBreak) {
            currentFlow.getChildren().add(new javafx.scene.text.Text("\n"));
        }

        @Override
        public void visit(Paragraph paragraph) {
            visitChildren(paragraph);
            // Paragraphs should be separate blocks, so we start a new flow after each
            startNewFlow();
        }

        @Override
        public void visit(Heading heading) {
            startNewFlow();
            javafx.scene.text.Text fxText = new javafx.scene.text.Text();
            fxText.getStyleClass().add("markdown-heading");
            fxText.getStyleClass().add("h" + heading.getLevel());

            // We need to visit children to get the text content of the heading
            // But we need to capture it into our specific fxText
            // For simplicity in this MVP, we'll just extract text from children manually or
            // use a temporary visitor
            // A simple approach:
            StringBuilder sb = new StringBuilder();
            org.commonmark.node.Node child = heading.getFirstChild();
            while (child != null) {
                if (child instanceof org.commonmark.node.Text) {
                    sb.append(((org.commonmark.node.Text) child).getLiteral());
                }
                child = child.getNext();
            }
            fxText.setText(sb.toString());

            currentFlow.getChildren().add(fxText);
            startNewFlow();
        }

        @Override
        public void visit(FencedCodeBlock fencedCodeBlock) {
            startNewFlow();
            Label codeLabel = new Label(fencedCodeBlock.getLiteral());
            codeLabel.getStyleClass().add("markdown-code-block");
            codeLabel.setWrapText(true);
            codeLabel.setMaxWidth(Double.MAX_VALUE);
            root.getChildren().add(codeLabel);
            startNewFlow();
        }

        @Override
        public void visit(Code code) {
            Label codeLabel = new Label(code.getLiteral());
            codeLabel.getStyleClass().add("markdown-code-inline");
            currentFlow.getChildren().add(codeLabel);
        }

        @Override
        public void visit(StrongEmphasis strongEmphasis) {
            // For simplicity, we just render text.
            // To support nested styles (bold inside italic), we'd need a more complex
            // state.
            // Here we just traverse children.
            // Ideally we would push a style to a stack.
            visitChildren(strongEmphasis);
        }

        @Override
        public void visit(Emphasis emphasis) {
            visitChildren(emphasis);
        }
    }
}
