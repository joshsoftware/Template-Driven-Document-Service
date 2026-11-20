package com.joshsoftware.templateDrivenDocumentService.support;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Test-only stand-in for the publish bundler: inlines the stylesheet and images of template.html
public final class TestTemplateInliner {

    private static final Pattern STYLESHEET = Pattern.compile("<link[^>]*href=\"([^\"]+\\.css)\"[^>]*/>");
    private static final Pattern IMAGE_SRC = Pattern.compile("src=\"(?!data:)([^\"]+)\"");

    private TestTemplateInliner() {
    }

    public static String inlinedStatementTemplate() {
        return inline(Fixtures.text("template.html"));
    }

    public static String inline(String html) {
        return inlineImages(inlineStylesheet(html));
    }

    // CDATA keeps "<" in CSS comments from being read as tags by the XML parser
    private static String inlineStylesheet(String html) {
        Matcher m = STYLESHEET.matcher(html);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String css = Fixtures.text(m.group(1));
            String style = "<style type=\"text/css\">/*<![CDATA[*/\n" + css + "\n/*]]>*/</style>";
            m.appendReplacement(out, Matcher.quoteReplacement(style));
        }
        return m.appendTail(out).toString();
    }

    private static String inlineImages(String html) {
        Matcher m = IMAGE_SRC.matcher(html);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String path = m.group(1);
            String data = "data:" + mediaType(path) + ";base64," + Base64.getEncoder().encodeToString(Fixtures.bytes(path));
            m.appendReplacement(out, Matcher.quoteReplacement("src=\"" + data + "\""));
        }
        return m.appendTail(out).toString();
    }

    private static String mediaType(String path) {
        if (path.endsWith(".png")) {
            return "image/png";
        }
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (path.endsWith(".svg")) {
            return "image/svg+xml";
        }
        throw new IllegalArgumentException("Unsupported image type: " + path);
    }
}
