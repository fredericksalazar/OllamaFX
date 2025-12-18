package com.org.ollamafx.ui;

import org.commonmark.node.*;

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
        // Use standard [h] tags which AtlantaFX BBCodeParser should handle
        // or we can style via CSS if needed.
        sb.append("\n[h").append(heading.getLevel()).append("]");
        visitChildren(heading);
        sb.append("[/h").append(heading.getLevel()).append("]\n\n");
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
