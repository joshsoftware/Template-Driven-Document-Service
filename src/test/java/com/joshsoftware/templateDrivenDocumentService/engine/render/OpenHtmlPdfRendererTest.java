package com.joshsoftware.templateDrivenDocumentService.engine.render;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.RenderException;
import com.joshsoftware.templateDrivenDocumentService.support.Fixtures;
import com.joshsoftware.templateDrivenDocumentService.support.PdfText;
import com.joshsoftware.templateDrivenDocumentService.support.StatementDefinition;
import com.joshsoftware.templateDrivenDocumentService.support.StatementModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenHtmlPdfRendererTest {

    private static final DocumentDefinition STATEMENT = StatementDefinition.load();

    private final TemplateRenderer templates = new ThymeleafTemplateRenderer();
    private final PdfRenderer pdfs = new OpenHtmlPdfRenderer();

    @Test
    void rendersTheStatement() {
        PdfText pdf = render("samples/model.json");

        assertThat(pdf.pageCount()).isGreaterThanOrEqualTo(1);
        assertThat(flat(pdf.text())).contains("TEST CUSTOMER", "10000000000001", "April 10, 2024",
                "TEST CREDIT-REF0001", "NEFT DR-REF0002");
    }

    @Test
    void rendersAnEmptyStatement() {
        assertThat(flat(render("samples/model-empty.json").text())).contains("No transactions for this period");
    }

    // 300 rows: the table header repeats on each page it continues on, and the footer counts pages
    @Test
    void splitsALongStatementAcrossPages() {
        PdfText pdf = render("samples/model-long-300.json");

        assertThat(pdf.pageCount()).isGreaterThan(1);
        assertThat(flat(pdf.pages().getFirst())).contains("Page 1 of " + pdf.pageCount());
        assertThat(pdf.pages()).filteredOn(page -> page.contains("REF0"))
                .hasSizeGreaterThan(1)
                // "Description" is the one header cell that never wraps onto two lines
                .allSatisfy(page -> assertThat(page).contains("Description"));
    }

    // The escaped script prints as the bank sent it; it never runs or becomes markup
    @Test
    void printsMarkupInPayloadTextLiterally() {
        assertThat(flat(render("samples/model-script-in-desc.json").text()))
                .contains("<script>alert(1)</script>");
    }

    @Test
    void loadsEmbeddedDataImages() {
        String png = "data:image/png;base64," + Base64.getEncoder().encodeToString(Fixtures.bytes("assets/images/bank-header.png"));

        assertThat(pdfBytes(page("<img src=\"" + png + "\"/>"))).isNotEmpty();
    }

    // A template must never make the renderer call the network or read the disk
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"http://example.com/logo.png", "https://example.com/logo.png",
            "file:///etc/passwd", "logo.png", "/assets/logo.png", "DATA:image/png;base64,AAAA"})
    void refusesImagesThatAreNotEmbedded(String src) {
        assertThatThrownBy(() -> pdfBytes(page("<img src=\"" + src + "\"/>")))
                .isInstanceOf(RenderException.class)
                .hasMessage("PDF could not be rendered");
    }

    @Test
    void reportsBrokenXhtmlAsRenderError() {
        assertThatThrownBy(() -> pdfBytes("<html><body><p>not closed</body></html>"))
                .isInstanceOf(RenderException.class);
    }

    // ---- helpers ----

    private PdfText render(String sample) {
        return PdfText.of(pdfBytes(templates.renderXhtml(STATEMENT, StatementModel.load(sample))));
    }

    private byte[] pdfBytes(String xhtml) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdfs.render(xhtml, out);
        return out.toByteArray();
    }

    private static String page(String body) {
        return "<html><head></head><body>" + body + "</body></html>";
    }

    // PDF text breaks lines where the layout wrapped them
    private static String flat(String text) {
        return text.replaceAll("\\s+", " ");
    }
}
