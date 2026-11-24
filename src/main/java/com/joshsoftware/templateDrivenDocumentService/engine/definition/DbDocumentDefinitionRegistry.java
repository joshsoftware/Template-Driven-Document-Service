package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import com.joshsoftware.templateDrivenDocumentService.entity.Template;
import com.joshsoftware.templateDrivenDocumentService.enums.TemplateStatus;
import com.joshsoftware.templateDrivenDocumentService.exception.ConfigNotFoundException;
import com.joshsoftware.templateDrivenDocumentService.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// DB-backed registry: loads the ACTIVE templates row and parses it into a DocumentDefinition.
// Stage 1 has no config cache, so every request reads and parses the row again.
// Plain Java like the rest of the engine; EngineConfig creates the bean.
@Slf4j
@RequiredArgsConstructor
public class DbDocumentDefinitionRegistry implements DocumentDefinitionRegistry {

    private final TemplateRepository templateRepository;
    private final DocumentDefinitionParser parser;

    // ConfigNotFoundException (404) when there is no ACTIVE version,
    // InvalidDefinitionException (500) when its stored config is broken
    @Override
    public DocumentDefinition get(String templateId) {
        Template template = templateRepository.findByTemplateIdAndStatus(templateId, TemplateStatus.ACTIVE)
                .orElseThrow(() -> new ConfigNotFoundException(templateId));
        if (template.getTemplateHtml() == null || template.getTemplateHtml().isBlank()) {
            throw new InvalidDefinitionException("template: template_html is empty");
        }
        DocumentDefinition definition = parser.parse(template.getId(), template.getTemplateId(),
                template.getVersion(), template.getTemplateHtml(), template.getContractJson(),
                template.getMappingJson());
        log.debug("[DbDocumentDefinitionRegistry#get] loaded templateId={} version={} templateVersionId={}",
                templateId, definition.version(), definition.templateVersionId());
        return definition;
    }
}
