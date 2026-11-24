package com.joshsoftware.templateDrivenDocumentService.config;

import com.joshsoftware.templateDrivenDocumentService.engine.contract.ContractValidator;
import com.joshsoftware.templateDrivenDocumentService.engine.contract.DefaultContractValidator;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DbDocumentDefinitionRegistry;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinitionParser;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinitionRegistry;
import com.joshsoftware.templateDrivenDocumentService.engine.mapping.DefaultMappingEngine;
import com.joshsoftware.templateDrivenDocumentService.engine.mapping.MappingEngine;
import com.joshsoftware.templateDrivenDocumentService.engine.render.OpenHtmlPdfRenderer;
import com.joshsoftware.templateDrivenDocumentService.engine.render.PdfRenderer;
import com.joshsoftware.templateDrivenDocumentService.engine.render.TemplateRenderer;
import com.joshsoftware.templateDrivenDocumentService.engine.render.ThymeleafTemplateRenderer;
import com.joshsoftware.templateDrivenDocumentService.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.ZoneId;

// Wires the engine into Spring. The engine classes are plain Java and stay free of Spring annotations;
// all of them are stateless and thread-safe, so one instance each is shared by every request.
@Configuration
public class EngineConfig {

    // Zone for "today" in documents (statement date, generated at). Bank-local time, not server time.
    @Bean
    public Clock clock(@Value("${tdds.engine.zone:Asia/Kolkata}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    // Uses Spring's JsonMapper so payload and config are read with the same settings
    @Bean
    public DocumentDefinitionParser documentDefinitionParser(JsonMapper jsonMapper) {
        return new DocumentDefinitionParser(jsonMapper);
    }

    // Reads the ACTIVE template row and parses it on every call (no cache in Stage 1)
    @Bean
    public DocumentDefinitionRegistry documentDefinitionRegistry(TemplateRepository templateRepository,
                                                                 DocumentDefinitionParser parser) {
        return new DbDocumentDefinitionRegistry(templateRepository, parser);
    }

    @Bean
    public MappingEngine mappingEngine(Clock clock) {
        return new DefaultMappingEngine(clock);
    }

    @Bean
    public ContractValidator contractValidator() {
        return new DefaultContractValidator();
    }

    @Bean
    public TemplateRenderer templateRenderer() {
        return new ThymeleafTemplateRenderer();
    }

    @Bean
    public PdfRenderer pdfRenderer() {
        return new OpenHtmlPdfRenderer();
    }
}
