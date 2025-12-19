package com.org.ollamafx.ui;

import atlantafx.base.util.BBCodeParser;
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
                // Use createLayout to handle block elements
                // Note: AtlantaFX BBCodeParser might be strict or limited.
                // If it fails, we fall back to raw text, which explains the [h2] visible.
                Node bbCodeNode = BBCodeParser.createFormattedText(block.content); // Try createFormattedText instead of
                                                                                   // createLayout? Or debug.

                // createLayout is usually for full blocks. createFormattedText for inline.
                // Let's stick to createLayout but debug the failure.
                // Actually, let's look at the imports. atlantafx.base.util.BBCodeParser

                bbCodeNode = BBCodeParser.createLayout(block.content);

                // Fix Text Wrapping
                if (bbCodeNode instanceof Region) {
                    Region region = (Region) bbCodeNode;
                    region.setMinWidth(0);
                    region.prefWidthProperty().bind(this.widthProperty().subtract(40));
                    region.maxWidthProperty().bind(this.widthProperty().subtract(40));
                }
                bbCodeNode.getProperties().put("bbcode", block.content);
                return bbCodeNode;
            } catch (Exception e) {
                // Formatting failed, likely due to unsupported tags.
                System.err.println("BBCode Parsing Failed for content: " + block.content);
                e.printStackTrace();

                // Fallback: Strip tags for cleaner failure? Or return TextFlow with basic text.
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
