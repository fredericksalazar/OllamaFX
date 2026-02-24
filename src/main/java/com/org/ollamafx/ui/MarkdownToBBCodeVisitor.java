package com.org.ollamafx.ui;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;

public class MarkdownToBBCodeVisitor extends AbstractVisitor {
    private final StringBuilder sb = new StringBuilder();

    public String getBBCode() {
        return sb.toString();
    }

    public void clear() {
        sb.setLength(0);
    }

    @Override
    public void visit(Text text) {
        sb.append(text.getLiteral());
    }

    @Override
    public void visit(StrongEmphasis strongEmphasis) {
        sb.append("[b]");
        visitChildren(strongEmphasis);
        sb.append("[/b]");
    }

    @Override
    public void visit(Emphasis emphasis) {
        sb.append("[i]");
        visitChildren(emphasis);
        sb.append("[/i]");
    }

    @Override
    public void visit(Paragraph paragraph) {
        visitChildren(paragraph);
        sb.append("\n\n");
    }

    @Override
    public void visit(Heading heading) {
        // AtlantaFX BBCodeParser may not support [h1]..[h6] tags directly.
        // We map them to bold + size scaling.
        int level = heading.getLevel();
        String size = "1.5em"; // Default

        switch (level) {
            case 1:
                size = "2em";
                break;
            case 2:
                size = "1.75em";
                break;
            case 3:
                size = "1.5em";
                break;
            case 4:
                size = "1.25em";
                break;
            case 5:
                size = "1.1em";
                break;
            case 6:
                size = "1em";
                break;
        }

        sb.append("\n\n[size=").append(size).append("][b]");
        visitChildren(heading);
        sb.append("[/b][/size]\n\n");
    }

    @Override
    public void visit(Code code) {
        sb.append("[code]").append(code.getLiteral()).append("[/code]");
    }

    // We handle FencedCodeBlock externally, but if nested?
    // Usually code blocks are top level.
    // If we meet inline code, we use [code].

    @Override
    public void visit(SoftLineBreak softLineBreak) {
        sb.append("\n");
    }

    @Override
    public void visit(HardLineBreak hardLineBreak) {
        sb.append("\n");
    }

    @Override
    public void visit(Link link) {
        sb.append("[url=").append(link.getDestination()).append("]");
        if (link.getTitle() != null) {
            // Title not supported in bbcode usually
        }
        visitChildren(link);
        sb.append("[/url]");
    }

    @Override
    public void visit(BulletList bulletList) {
        sb.append("[ul]\n");
        visitChildren(bulletList);
        sb.append("[/ul]\n");
    }

    @Override
    public void visit(OrderedList orderedList) {
        sb.append("[ol]\n");
        visitChildren(orderedList);
        sb.append("[/ol]\n");
    }

    @Override
    public void visit(ListItem listItem) {
        sb.append("[li]");
        visitChildren(listItem);
        sb.append("[/li]\n");
    }
}
