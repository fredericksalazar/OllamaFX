package com.org.ollamafx.ui;

import atlantafx.base.util.BBCodeParser;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.List;

public class MarkdownOutput extends VBox {

    private final Parser parser;

    public MarkdownOutput() {
        this.parser = Parser.builder().build();
        this.getStyleClass().add("markdown-area");
        this.setFillWidth(true);
        this.setSpacing(10);
    }

    private String originalMarkdown;

    public void updateContent(String markdown) {
        setMarkdown(markdown);
    }

    public String getMarkdown() {
        return originalMarkdown;
    }

    public void setMarkdown(String markdown) {
        if (markdown == null)
            markdown = "";

        this.originalMarkdown = markdown;

        // 1. Parse into abstract "Block" list to allow diffing
        List<BlockData> newBlocks = parseToBlocks(markdown);

        // 2. Sync with children
        syncChildren(newBlocks);
    }

    private void syncChildren(List<BlockData> newBlocks) {
        var children = this.getChildren();

        // Remove excess children
        if (children.size() > newBlocks.size()) {
            children.remove(newBlocks.size(), children.size());
        }

        for (int i = 0; i < newBlocks.size(); i++) {
            BlockData block = newBlocks.get(i);

            if (i < children.size()) {
                Node child = children.get(i);

                if (block.type == BlockType.CODE && child instanceof CodeBlockCard) {
                    // Update Code in-place (Efficient)
                    ((CodeBlockCard) child).updateCode(block.content);
                } else if (block.type == BlockType.PROSE && child instanceof Region
                        && !(child instanceof CodeBlockCard)) {
                    // Check if Prose content changed
                    String oldContent = (String) child.getProperties().get("bbcode");
                    if (oldContent == null || !oldContent.equals(block.content)) {
                        // Content changed, replace node
                        children.set(i, createNode(block));
                    }
                    // Else: content identical, do nothing (No jump!)
                } else {
                    // Type mismatch (e.g. Prose -> Code), replace
                    children.set(i, createNode(block));
                }
            } else {
                // Append new node
                children.add(createNode(block));
            }
        }
    }

    // Revised Sync Logic with Prose Replacement

    private Node createNode(BlockData block) {
        if (block.type == BlockType.CODE) {
            return new CodeBlockCard(block.content, block.info);
        } else {
            try {
                // Use createLayout to handle block elements (lists, etc.) properly regarding
                // wrapping
                Node bbCodeNode = BBCodeParser.createLayout(block.content);

                // Fix Text Wrapping: Bind maxWidth AND prefWidth to MarkdownOutput width
                if (bbCodeNode instanceof Region) {
                    Region region = (Region) bbCodeNode;
                    region.setMinWidth(0); // Allow shrinking
                    // Account for CSS padding (20px) + Safety Buffer
                    region.prefWidthProperty().bind(this.widthProperty().subtract(40));
                    region.maxWidthProperty().bind(this.widthProperty().subtract(40));
                }
                bbCodeNode.getProperties().put("bbcode", block.content);
                return bbCodeNode;
            } catch (Exception e) {
                return new javafx.scene.control.Label(block.content);
            }
        }
    }

    // --- Parsing Logic ---

    private enum BlockType {
        PROSE, CODE
    }

    private static class BlockData {
        BlockType type;
        String content;
        String info; // for code

        BlockData(BlockType type, String content, String info) {
            this.type = type;
            this.content = content;
            this.info = info;
        }
    }

    private List<BlockData> parseToBlocks(String markdown) {
        List<BlockData> blocks = new ArrayList<>();
        org.commonmark.node.Node document = parser.parse(markdown);

        MarkdownToBBCodeVisitor visitor = new MarkdownToBBCodeVisitor();

        org.commonmark.node.Node node = document.getFirstChild();
        while (node != null) {
            boolean isCode = (node instanceof FencedCodeBlock) || (node instanceof IndentedCodeBlock);

            if (isCode) {
                // Flush Prose
                String prose = visitor.getBBCode();
                if (!prose.isEmpty()) {
                    blocks.add(new BlockData(BlockType.PROSE, prose, null));
                    visitor.clear();
                }

                String code = "";
                String info = "";

                if (node instanceof FencedCodeBlock) {
                    FencedCodeBlock f = (FencedCodeBlock) node;
                    code = f.getLiteral();
                    info = f.getInfo();
                } else {
                    code = ((IndentedCodeBlock) node).getLiteral();
                }

                blocks.add(new BlockData(BlockType.CODE, code, info));

            } else {
                node.accept(visitor);
            }
            node = node.getNext();
        }

        // Final flush
        String prose = visitor.getBBCode();
        if (!prose.isEmpty()) {
            blocks.add(new BlockData(BlockType.PROSE, prose, null));
        }

        return blocks;
    }
}
