package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import com.joshsoftware.templateDrivenDocumentService.entity.Template;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import com.joshsoftware.templateDrivenDocumentService.enums.TemplateStatus;
import com.joshsoftware.templateDrivenDocumentService.exception.ConfigNotFoundException;
import com.joshsoftware.templateDrivenDocumentService.repository.TemplateRepository;
import com.joshsoftware.templateDrivenDocumentService.support.Fixtures;
import com.joshsoftware.templateDrivenDocumentService.support.TestTemplateInliner;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Mocked repository, real parser on the statement fixtures
class DbDocumentDefinitionRegistryTest {

    private static final UUID V1_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TemplateRepository templates = mock(TemplateRepository.class);
    private final DbDocumentDefinitionRegistry registry =
            new DbDocumentDefinitionRegistry(templates, new DocumentDefinitionParser(JsonMapper.builder().build()));

    @Test
    void getLoadsTheActiveVersion() {
        when(templates.findByTemplateIdAndStatus("statement", TemplateStatus.ACTIVE))
                .thenReturn(Optional.of(statement(TemplateStatus.ACTIVE)));

        DocumentDefinition definition = registry.get("statement");

        assertThat(definition.templateVersionId()).isEqualTo(V1_ID);
        assertThat(definition.templateId()).isEqualTo("statement");
        assertThat(definition.version()).isEqualTo(1);
    }

    @Test
    void getWithNoActiveVersionIsNotFound() {
        when(templates.findByTemplateIdAndStatus("statement", TemplateStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registry.get("statement"))
                .isInstanceOf(ConfigNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFIG_NOT_FOUND);
    }

    @Test
    void getWithEmptyHtmlIsInvalid() {
        Template template = statement(TemplateStatus.ACTIVE);
        template.setTemplateHtml(" ");
        when(templates.findByTemplateIdAndStatus("statement", TemplateStatus.ACTIVE)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> registry.get("statement")).isInstanceOf(InvalidDefinitionException.class);
    }

    @Test
    void getByVersionIdLoadsAnActiveVersion() {
        when(templates.findById(V1_ID)).thenReturn(Optional.of(statement(TemplateStatus.ACTIVE)));

        assertThat(registry.getByVersionId(V1_ID).templateVersionId()).isEqualTo(V1_ID);
    }

    @Test
    void getByVersionIdLoadsADisabledVersion() {
        when(templates.findById(V1_ID)).thenReturn(Optional.of(statement(TemplateStatus.DISABLED)));

        DocumentDefinition definition = registry.getByVersionId(V1_ID);

        assertThat(definition.templateVersionId()).isEqualTo(V1_ID);
        assertThat(definition.version()).isEqualTo(1);
    }

    @Test
    void getByVersionIdWithUnknownIdIsNotFound() {
        UUID unknown = UUID.randomUUID();
        when(templates.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registry.getByVersionId(unknown))
                .isInstanceOf(ConfigNotFoundException.class)
                .hasMessageContaining(unknown.toString());
    }

    @Test
    void getByVersionIdWithEmptyHtmlIsInvalid() {
        Template template = statement(TemplateStatus.DISABLED);
        template.setTemplateHtml(null);
        when(templates.findById(V1_ID)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> registry.getByVersionId(V1_ID)).isInstanceOf(InvalidDefinitionException.class);
    }

    private static Template statement(TemplateStatus status) {
        Template template = new Template();
        template.setId(V1_ID);
        template.setTemplateId("statement");
        template.setVersion(1);
        template.setStatus(status);
        template.setTemplateHtml(TestTemplateInliner.inlinedStatementTemplate());
        template.setContractJson(Fixtures.text("contract.json"));
        template.setMappingJson(Fixtures.text("mapping.json"));
        return template;
    }
}
