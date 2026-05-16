package com.org.ollamafx.ui.markdown;

import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.util.ast.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import com.org.ollamafx.ui.markdown.InlineRenderer.InlineStyle;

public final class MarkdownRenderer {

    private MarkdownRenderer() {
    }

    public static javafx.scene.Node render(Node node) {
        if (node instanceof Heading) {
            return renderHeading((Heading) node);
        }
        if (node instanceof Paragraph) {
            return renderParagraph((Paragraph) node);
        }
        if (node instanceof BulletList) {
            return renderList((BulletList) node, false);
        }
        if (node instanceof OrderedList) {
            return renderList((OrderedList) node, true);
        }
        if (node instanceof BlockQuote) {
            return renderBlockQuote((BlockQuote) node);
        }
        if (node instanceof ThematicBreak) {
            Separator sep = new Separator();
            sep.getStyleClass().add("md-hr");
            return sep;
        }
        if (node instanceof TableBlock) {
            return MarkdownTableNode.build((TableBlock) node);
        }
        // Fallback: render children into a VBox if it has block children, else a paragraph
        return renderInlineAsParagraph(node);
    }

    private static javafx.scene.Node renderHeading(Heading h) {
        TextFlow flow = new TextFlow();
        int level = Math.max(1, Math.min(6, h.getLevel()));
        flow.getStyleClass().addAll("md-heading", "md-h" + level);
        InlineRenderer.renderChildren(h, flow, InlineStyle.EMPTY);
        VBox wrap = new VBox(flow);
        wrap.getStyleClass().add("md-heading-block");
        return wrap;
    }

    private static javafx.scene.Node renderParagraph(Paragraph p) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("md-paragraph");
        InlineRenderer.renderChildren(p, flow, InlineStyle.EMPTY);
        return flow;
    }

    private static javafx.scene.Node renderInlineAsParagraph(Node n) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("md-paragraph");
        InlineRenderer.renderChildren(n, flow, InlineStyle.EMPTY);
        return flow;
    }

    private static javafx.scene.Node renderList(ListBlock list, boolean ordered) {
        VBox box = new VBox();
        box.getStyleClass().add(ordered ? "md-ordered-list" : "md-bullet-list");
        box.setSpacing(4);

        int index = 1;
        Node child = list.getFirstChild();
        while (child != null) {
            if (child instanceof ListItem) {
                box.getChildren().add(renderListItem((ListItem) child, ordered, index));
                index++;
            }
            child = child.getNext();
        }
        return box;
    }

    private static javafx.scene.Node renderListItem(ListItem item, boolean ordered, int index) {
        HBox row = new HBox();
        row.getStyleClass().add("md-list-item");
        row.setSpacing(8);
        row.setAlignment(Pos.TOP_LEFT);

        javafx.scene.Node marker;
        if (item instanceof TaskListItem) {
            CheckBox cb = new CheckBox();
            cb.setSelected(((TaskListItem) item).isItemDoneMarker());
            cb.setDisable(true);
            cb.getStyleClass().add("md-task-checkbox");
            marker = cb;
        } else if (ordered) {
            Label l = new Label(index + ".");
            l.getStyleClass().add("md-list-marker");
            marker = l;
        } else {
            Label l = new Label("•");
            l.getStyleClass().add("md-list-marker");
            marker = l;
        }

        VBox content = new VBox();
        content.setSpacing(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        // List items can contain paragraphs (loose) or inline children (tight)
        // Flexmark wraps inline content in a Paragraph node when block; otherwise inline children directly.
        Node child = item.getFirstChild();
        boolean anyBlock = false;
        while (child != null) {
            if (isBlockNode(child)) {
                content.getChildren().add(render(child));
                anyBlock = true;
            }
            child = child.getNext();
        }

        if (!anyBlock) {
            TextFlow flow = new TextFlow();
            flow.getStyleClass().add("md-paragraph");
            InlineRenderer.renderChildren(item, flow, InlineStyle.EMPTY);
            content.getChildren().add(flow);
        }

        row.getChildren().addAll(marker, content);
        return row;
    }

    private static javafx.scene.Node renderBlockQuote(BlockQuote bq) {
        VBox inner = new VBox();
        inner.setSpacing(6);
        Node child = bq.getFirstChild();
        while (child != null) {
            if (isBlockNode(child)) {
                inner.getChildren().add(render(child));
            }
            child = child.getNext();
        }
        if (inner.getChildren().isEmpty()) {
            inner.getChildren().add(renderInlineAsParagraph(bq));
        }
        VBox wrap = new VBox(BlockQuoteNode.wrap(inner));
        wrap.getStyleClass().add("md-blockquote-wrap");
        wrap.setPadding(new Insets(2, 0, 2, 0));
        return wrap;
    }

    private static boolean isBlockNode(Node n) {
        return n instanceof Paragraph
                || n instanceof Heading
                || n instanceof BulletList
                || n instanceof OrderedList
                || n instanceof BulletListItem
                || n instanceof OrderedListItem
                || n instanceof BlockQuote
                || n instanceof ThematicBreak
                || n instanceof TableBlock
                || n instanceof ListItem;
    }
}
