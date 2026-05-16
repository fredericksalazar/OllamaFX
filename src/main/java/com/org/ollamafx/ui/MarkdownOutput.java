package com.org.ollamafx.ui;

import com.org.ollamafx.ui.markdown.MarkdownRenderer;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarkdownOutput extends VBox {

    private static final String SIG_KEY = "md-block-signature";

    private final Parser parser;
    private String originalMarkdown;

    public MarkdownOutput() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                TaskListExtension.create(),
                StrikethroughExtension.create()
        ));
        this.parser = Parser.builder(options).build();

        getStyleClass().add("markdown-area");
        setFillWidth(true);
        setSpacing(8);
    }

    public void updateContent(String markdown) {
        setMarkdown(markdown);
    }

    public String getMarkdown() {
        return originalMarkdown;
    }

    public void setMarkdown(String markdown) {
        if (markdown == null) {
            markdown = "";
        }
        this.originalMarkdown = markdown;

        List<BlockEntry> blocks = parseToBlocks(markdown);
        syncChildren(blocks);
    }

    private void syncChildren(List<BlockEntry> blocks) {
        var children = getChildren();

        if (children.size() > blocks.size()) {
            children.remove(blocks.size(), children.size());
        }

        for (int i = 0; i < blocks.size(); i++) {
            BlockEntry entry = blocks.get(i);

            if (i < children.size()) {
                javafx.scene.Node existing = children.get(i);
                String oldSig = (String) existing.getProperties().get(SIG_KEY);

                if (entry.kind == BlockKind.CODE && existing instanceof CodeBlockCard) {
                    CodeBlockCard card = (CodeBlockCard) existing;
                    if (!entry.signature.equals(oldSig)) {
                        card.updateCode(entry.codeContent, entry.codeLanguage);
                        existing.getProperties().put(SIG_KEY, entry.signature);
                    }
                } else if (entry.kind == BlockKind.NODE
                        && !(existing instanceof CodeBlockCard)
                        && entry.signature.equals(oldSig)) {
                    // identical block — keep
                } else {
                    children.set(i, createNode(entry));
                }
            } else {
                children.add(createNode(entry));
            }
        }
    }

    private javafx.scene.Node createNode(BlockEntry entry) {
        if (entry.kind == BlockKind.CODE) {
            CodeBlockCard card = new CodeBlockCard(entry.codeContent, entry.codeLanguage);
            card.getProperties().put(SIG_KEY, entry.signature);
            return card;
        }
        javafx.scene.Node rendered = MarkdownRenderer.render(entry.astNode);
        rendered.getProperties().put(SIG_KEY, entry.signature);
        if (rendered instanceof Region) {
            Region region = (Region) rendered;
            region.setMinWidth(0);
        }
        return rendered;
    }

    private List<BlockEntry> parseToBlocks(String markdown) {
        List<BlockEntry> blocks = new ArrayList<>();
        Document document = parser.parse(markdown);

        Node node = document.getFirstChild();
        while (node != null) {
            if (node instanceof FencedCodeBlock) {
                FencedCodeBlock f = (FencedCodeBlock) node;
                String code = f.getContentChars().toString();
                String info = f.getInfo().toString();
                blocks.add(BlockEntry.code(code, info));
            } else if (node instanceof IndentedCodeBlock) {
                IndentedCodeBlock i = (IndentedCodeBlock) node;
                blocks.add(BlockEntry.code(i.getContentChars().toString(), ""));
            } else {
                String source = node.getChars().toString();
                blocks.add(BlockEntry.astNode(node, source));
            }
            node = node.getNext();
        }
        return blocks;
    }

    private enum BlockKind { CODE, NODE }

    private static final class BlockEntry {
        final BlockKind kind;
        final Node astNode;
        final String codeContent;
        final String codeLanguage;
        final String signature;

        private BlockEntry(BlockKind kind, Node astNode, String code, String lang, String signature) {
            this.kind = kind;
            this.astNode = astNode;
            this.codeContent = code;
            this.codeLanguage = lang;
            this.signature = signature;
        }

        static BlockEntry code(String content, String language) {
            String lang = language == null ? "" : language;
            return new BlockEntry(BlockKind.CODE, null, content, lang, "code|" + lang + "|" + content);
        }

        static BlockEntry astNode(Node node, String source) {
            return new BlockEntry(BlockKind.NODE, node, null, null,
                    node.getClass().getSimpleName() + "|" + source);
        }
    }
}
