package com.joshsoftware.templateDrivenDocumentService.engine.render;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.RenderException;
import com.joshsoftware.templateDrivenDocumentService.support.StatementDefinition;
import com.joshsoftware.templateDrivenDocumentService.support.StatementModel;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThymeleafTemplateRendererTest {

    private static final DocumentDefinition STATEMENT = StatementDefinition.load();

    private final TemplateRenderer renderer = new ThymeleafTemplateRenderer();

    @Test
    void fillsTheStatementTemplate() {
        String xhtml = renderer.renderXhtml(STATEMENT, StatementModel.load("samples/model.json"));

        assertThat(xhtml).contains("TEST CUSTOMER", "10000000000001", "TEST0000001",
                "April 10, 2024", "TEST CREDIT-REF0001", "14,200.00");
        // Every th:* attribute was processed and removed
        assertThat(xhtml).doesNotContainPattern("\\sth:\\w+=");
    }

    // Bank text is data, never markup: a script in a description must come out as escaped text
    @Test
    void escapesMarkupInPayloadText() {
        String xhtml = renderer.renderXhtml(STATEMENT, StatementModel.load("samples/model-script-in-desc.json"));

        assertThat(xhtml).contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; &lt;img src=x&gt;");
        assertThat(xhtml).doesNotContain("<script>alert(1)");
    }

    @Test
    void showsTheEmptyMessageWhenThereAreNoTransactions() {
        String xhtml = renderer.renderXhtml(STATEMENT, StatementModel.load("samples/model-empty.json"));

        assertThat(xhtml).contains("No transactions for this period");
    }

    // A server set to another locale must still print English month names
    @Test
    void printsEnglishMonthsWhateverTheServerLocale() {
        Map<String, Object> model = StatementModel.load("samples/model.json");
        Locale serverLocale = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);
        try {
            assertThat(renderer.renderXhtml(STATEMENT, model)).contains("April 10, 2024").doesNotContain("avril");
        } finally {
            Locale.setDefault(serverLocale);
        }
    }

    @Test
    void reportsABrokenTemplateAsRenderError() {
        DocumentDefinition broken = new DocumentDefinition(STATEMENT.templateVersionId(), "statement", 1,
                "<p th:text=\"${1 +}\"></p>", STATEMENT.contract(), STATEMENT.dateFormats(), STATEMENT.mapping());

        assertThatThrownBy(() -> renderer.renderXhtml(broken, Map.of()))
                .isInstanceOf(RenderException.class)
                .hasMessage("template could not be rendered");
    }
}
