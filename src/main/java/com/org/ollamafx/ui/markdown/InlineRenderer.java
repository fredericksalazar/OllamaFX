package com.org.ollamafx.ui.markdown;

import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.util.ast.Node;
import javafx.scene.text.TextFlow;

import java.awt.Desktop;
import java.net.URI;

final class InlineRenderer {

    private InlineRenderer() {
    }

    static void renderChildren(Node parent, TextFlow target, InlineStyle style) {
        Node child = parent.getFirstChild();
        while (child != null) {
            renderNode(child, target, style);
            child = child.getNext();
        }
    }

    private static void renderNode(Node node, TextFlow target, InlineStyle style) {
        if (node instanceof Text) {
            target.getChildren().add(buildText(node.getChars().toString(), style));
        } else if (node instanceof StrongEmphasis) {
            renderChildren(node, target, style.with(InlineStyle.Flag.BOLD));
        } else if (node instanceof Emphasis) {
            renderChildren(node, target, style.with(InlineStyle.Flag.ITALIC));
        } else if (node instanceof Strikethrough) {
            renderChildren(node, target, style.with(InlineStyle.Flag.STRIKE));
        } else if (node instanceof Code) {
            javafx.scene.text.Text t = buildText(((Code) node).getText().toString(), style.with(InlineStyle.Flag.CODE));
            t.getStyleClass().add("md-inline-code");
            target.getChildren().add(t);
        } else if (node instanceof Link) {
            Link link = (Link) node;
            String url = link.getUrl().toString();
            InlineStyle linkStyle = style.with(InlineStyle.Flag.LINK);
            int startIdx = target.getChildren().size();
            renderChildren(node, target, linkStyle);
            for (int i = startIdx; i < target.getChildren().size(); i++) {
                javafx.scene.Node n = target.getChildren().get(i);
                if (!n.getStyleClass().contains("md-link")) {
                    n.getStyleClass().add("md-link");
                }
                n.setOnMouseClicked(e -> openLink(url));
            }
        } else if (node instanceof SoftLineBreak) {
            target.getChildren().add(new javafx.scene.text.Text(" "));
        } else if (node instanceof HardLineBreak) {
            target.getChildren().add(new javafx.scene.text.Text("\n"));
        } else {
            // Unknown inline → recurse to extract text from children if any
            if (node.getFirstChild() != null) {
                renderChildren(node, target, style);
            } else {
                target.getChildren().add(buildText(node.getChars().toString(), style));
            }
        }
    }

    private static javafx.scene.text.Text buildText(String content, InlineStyle style) {
        javafx.scene.text.Text t = new javafx.scene.text.Text(content);
        t.getStyleClass().add("md-text");
        if (style.has(InlineStyle.Flag.BOLD))   t.getStyleClass().add("md-bold");
        if (style.has(InlineStyle.Flag.ITALIC)) t.getStyleClass().add("md-italic");
        if (style.has(InlineStyle.Flag.STRIKE)) t.getStyleClass().add("md-strike");
        // CODE class is added by the caller for inline code (md-inline-code).
        // LINK class is added by the Link branch.
        return t;
    }

    private static void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }

    static final class InlineStyle {
        enum Flag { BOLD, ITALIC, STRIKE, CODE, LINK }

        private final int mask;

        static final InlineStyle EMPTY = new InlineStyle(0);

        private InlineStyle(int mask) {
            this.mask = mask;
        }

        InlineStyle with(Flag f) {
            return new InlineStyle(mask | (1 << f.ordinal()));
        }

        boolean has(Flag f) {
            return (mask & (1 << f.ordinal())) != 0;
        }
    }
}
