package com.joshsoftware.templateDrivenDocumentService.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestTemplateInlinerTest {

    @Test
    void inlinesStylesheetAndImages() {
        String html = TestTemplateInliner.inlinedStatementTemplate();

        assertThat(html).contains("<style type=\"text/css\">/*<![CDATA[*/");
        assertThat(html).contains("@page");
        assertThat(markupOnly(html)).doesNotContain("<link");
        assertThat(markupOnly(html)).contains("src=\"data:image/png;base64,");
    }

    @Test
    void leavesNoRelativeLinks() {
        String html = TestTemplateInliner.inlinedStatementTemplate();

        assertThat(markupOnly(html)).doesNotContainPattern("(href|src)=\"(?!data:)[^\"#]");
    }

    // Drops comments and the inlined CSS, whose own comments mention the original link
    private static String markupOnly(String html) {
        return html.replaceAll("(?s)<!--.*?-->", "").replaceAll("(?s)<style.*?</style>", "");
    }

    @Test
    void loadsAnonymisedSamples() {
        assertThat(Fixtures.json("samples/payload-complete.json").path("AccountDetails").path("AccountId").asString())
                .isEqualTo("10000000000001");
        assertThat(Fixtures.json("contract.json").has("fields")).isTrue();
        assertThat(Fixtures.json("mapping.json").has("dateFormats")).isTrue();
    }
}
