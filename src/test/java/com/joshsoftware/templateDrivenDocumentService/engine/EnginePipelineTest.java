package com.joshsoftware.templateDrivenDocumentService.engine;

import com.joshsoftware.templateDrivenDocumentService.engine.contract.ContractValidator;
import com.joshsoftware.templateDrivenDocumentService.engine.contract.DefaultContractValidator;
import com.joshsoftware.templateDrivenDocumentService.engine.contract.Violation;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.DataContractViolationException;
import com.joshsoftware.templateDrivenDocumentService.engine.mapping.DefaultMappingEngine;
import com.joshsoftware.templateDrivenDocumentService.engine.mapping.MappingEngine;
import com.joshsoftware.templateDrivenDocumentService.engine.render.OpenHtmlPdfRenderer;
import com.joshsoftware.templateDrivenDocumentService.engine.render.PdfRenderer;
import com.joshsoftware.templateDrivenDocumentService.engine.render.TemplateRenderer;
import com.joshsoftware.templateDrivenDocumentService.engine.render.ThymeleafTemplateRenderer;
import com.joshsoftware.templateDrivenDocumentService.support.Fixtures;
import com.joshsoftware.templateDrivenDocumentService.support.PdfText;
import com.joshsoftware.templateDrivenDocumentService.support.StatementDefinition;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The whole engine, in the order the service will call it: map -> validate -> XHTML -> PDF
class EnginePipelineTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-06-01T04:30:00Z"), ZoneId.of("Asia/Kolkata"));

    private final DocumentDefinition statement = StatementDefinition.load();
    private final MappingEngine mapping = new DefaultMappingEngine(CLOCK);
    private final ContractValidator validator = new DefaultContractValidator();
    private final TemplateRenderer templates = new ThymeleafTemplateRenderer();
    private final PdfRenderer pdfs = new OpenHtmlPdfRenderer();

    // A payload without customer details or the summary stops at validation, before any rendering
    @Test
    void stopsAtValidationWhenRequiredDataIsMissing() {
        JsonNode mapped = mapping.transform(statement, Fixtures.json("samples/payload.json"));

        assertThatThrownBy(() -> validator.validate(statement, mapped))
                .isInstanceOfSatisfying(DataContractViolationException.class, e ->
                        assertThat(e.getViolations()).extracting(Violation::getField)
                                .contains("customerName", "address", "openingBalance", "closingBalance"));
    }

    @Test
    void turnsABankPayloadIntoAStatementPdf() {
        JsonNode mapped = mapping.transform(statement, Fixtures.json("samples/payload-complete.json"));

        Map<String, Object> model = validator.validate(statement, mapped);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdfs.render(templates.renderXhtml(statement, model), out);

        String text = PdfText.of(out.toByteArray()).text().replaceAll("\\s+", " ");
        assertThat(text).contains("TEST CUSTOMER", "10000000000001", "June 01, 2024",
                "April 10, 2024", "TEST CREDIT-REF0001", "14,200.00");
    }
}
