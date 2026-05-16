package com.org.ollamafx.ui.markdown;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyntaxHighlighter {

    private SyntaxHighlighter() {
    }

    private static final Map<String, Pattern> PATTERNS = new HashMap<>();

    static {
        PATTERNS.put("java", javaPattern());
        PATTERNS.put("kotlin", javaPattern());
        PATTERNS.put("python", pythonPattern());
        PATTERNS.put("py", pythonPattern());
        PATTERNS.put("javascript", jsPattern());
        PATTERNS.put("js", jsPattern());
        PATTERNS.put("typescript", jsPattern());
        PATTERNS.put("ts", jsPattern());
        PATTERNS.put("json", jsonPattern());
        PATTERNS.put("bash", bashPattern());
        PATTERNS.put("sh", bashPattern());
        PATTERNS.put("shell", bashPattern());
        PATTERNS.put("sql", sqlPattern());
        PATTERNS.put("xml", xmlPattern());
        PATTERNS.put("html", xmlPattern());
    }

    public static StyleSpans<Collection<String>> computeHighlight(String text, String language) {
        if (text == null || text.isEmpty()) {
            return emptySpans(0);
        }
        String lang = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        Pattern pattern = PATTERNS.get(lang);
        if (pattern == null) {
            return emptySpans(text.length());
        }
        return highlight(text, pattern);
    }

    private static StyleSpans<Collection<String>> emptySpans(int length) {
        StyleSpansBuilder<Collection<String>> b = new StyleSpansBuilder<>();
        b.add(Collections.emptyList(), Math.max(0, length));
        return b.create();
    }

    private static StyleSpans<Collection<String>> highlight(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        StyleSpansBuilder<Collection<String>> b = new StyleSpansBuilder<>();
        int last = 0;
        while (m.find()) {
            String styleClass = matchedClass(m);
            if (styleClass == null) {
                continue;
            }
            b.add(Collections.emptyList(), m.start() - last);
            b.add(Collections.singletonList(styleClass), m.end() - m.start());
            last = m.end();
        }
        b.add(Collections.emptyList(), text.length() - last);
        return b.create();
    }

    private static String matchedClass(Matcher m) {
        if (m.group("KEYWORD") != null) return "code-keyword";
        if (m.group("STRING") != null) return "code-string";
        if (m.group("COMMENT") != null) return "code-comment";
        if (m.group("NUMBER") != null) return "code-number";
        if (m.group("ANNOT") != null) return "code-annotation";
        return null;
    }

    private static Pattern build(String keywords, String stringRe, String commentRe, String annotRe) {
        String kwRe = "\\b(" + keywords + ")\\b";
        StringBuilder sb = new StringBuilder();
        sb.append("(?<KEYWORD>").append(kwRe).append(")");
        sb.append("|(?<STRING>").append(stringRe).append(")");
        sb.append("|(?<COMMENT>").append(commentRe).append(")");
        sb.append("|(?<NUMBER>\\b\\d+(?:\\.\\d+)?\\b)");
        sb.append("|(?<ANNOT>").append(annotRe).append(")");
        return Pattern.compile(sb.toString(), Pattern.DOTALL);
    }

    private static Pattern javaPattern() {
        String kw = "abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|"
                + "do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|"
                + "instanceof|int|interface|long|native|new|package|private|protected|public|return|"
                + "short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|"
                + "void|volatile|while|true|false|null|var|record|sealed|permits|yield";
        return build(kw,
                "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'",
                "//[^\\n]*|/\\*.*?\\*/",
                "@\\w+");
    }

    private static Pattern pythonPattern() {
        String kw = "False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|else|"
                + "except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|"
                + "return|try|while|with|yield|self|cls";
        return build(kw,
                "\"\"\".*?\"\"\"|'''.*?'''|\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'",
                "#[^\\n]*",
                "@\\w+");
    }

    private static Pattern jsPattern() {
        String kw = "abstract|any|as|async|await|boolean|break|case|catch|class|const|continue|debugger|"
                + "default|delete|do|else|enum|export|extends|false|finally|for|from|function|get|if|"
                + "implements|import|in|instanceof|interface|let|new|null|number|of|package|private|"
                + "protected|public|readonly|return|set|static|string|super|switch|this|throw|true|try|"
                + "type|typeof|undefined|var|void|while|with|yield";
        return build(kw,
                "`([^`\\\\]|\\\\.)*`|\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'",
                "//[^\\n]*|/\\*.*?\\*/",
                "@\\w+");
    }

    private static Pattern jsonPattern() {
        String kw = "true|false|null";
        return build(kw,
                "\"([^\"\\\\]|\\\\.)*\"",
                "//[^\\n]*",
                "(?!)");
    }

    private static Pattern bashPattern() {
        String kw = "if|then|else|elif|fi|case|esac|for|select|while|until|do|done|in|function|"
                + "time|coproc|return|exit|break|continue|export|local|readonly|declare|let|"
                + "echo|cd|pwd|ls|cat|grep|sed|awk|find|mkdir|rm|cp|mv|chmod|chown|sudo|"
                + "source|alias|unset|true|false";
        return build(kw,
                "\"([^\"\\\\]|\\\\.)*\"|'[^']*'",
                "#[^\\n]*",
                "\\$\\w+|\\$\\{[^}]+\\}");
    }

    private static Pattern sqlPattern() {
        String kw = "SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|DROP|ALTER|"
                + "ADD|COLUMN|INDEX|VIEW|JOIN|LEFT|RIGHT|INNER|OUTER|FULL|ON|GROUP|BY|ORDER|HAVING|"
                + "LIMIT|OFFSET|UNION|ALL|AS|AND|OR|NOT|NULL|IS|IN|LIKE|BETWEEN|CASE|WHEN|THEN|ELSE|"
                + "END|DISTINCT|COUNT|SUM|AVG|MIN|MAX|PRIMARY|KEY|FOREIGN|REFERENCES|DEFAULT|"
                + "select|from|where|insert|into|values|update|set|delete|create|table|drop|alter|"
                + "add|column|index|view|join|left|right|inner|outer|full|on|group|by|order|having|"
                + "limit|offset|union|all|as|and|or|not|null|is|in|like|between|case|when|then|else|"
                + "end|distinct|count|sum|avg|min|max|primary|key|foreign|references|default";
        return build(kw,
                "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'",
                "--[^\\n]*|/\\*.*?\\*/",
                "(?!)");
    }

    private static Pattern xmlPattern() {
        StringBuilder sb = new StringBuilder();
        sb.append("(?<KEYWORD></?[a-zA-Z][\\w:-]*|/?>)");
        sb.append("|(?<STRING>\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*')");
        sb.append("|(?<COMMENT><!--.*?-->)");
        sb.append("|(?<NUMBER>\\b\\d+\\b)");
        sb.append("|(?<ANNOT>\\b[a-zA-Z][\\w:-]*(?==))");
        return Pattern.compile(sb.toString(), Pattern.DOTALL);
    }
}
