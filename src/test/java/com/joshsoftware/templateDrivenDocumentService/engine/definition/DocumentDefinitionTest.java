package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DocumentDefinitionTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID ID = UUID.randomUUID();
    private static final ContractField TEXT = ContractField.scalar(FieldType.STRING, true);

    // A loaded version must stay exactly as loaded, whatever the caller does with its inputs
    @Test
    void cannotBeChangedAfterLoad() {
        Map<String, ContractField> contract = new LinkedHashMap<>();
        contract.put("customerName", TEXT);
        contract.put("ifsc", TEXT);
        ObjectNode transform = transform();

        DocumentDefinition def = new DocumentDefinition(ID, "statement", 1, "<html/>", contract,
                DateFormats.of("yyyyMMdd", null), transform);
        contract.put("added", TEXT);
        transform.put("ifsc", "Changed.Path");

        assertThat(def.contract().keySet()).containsExactly("customerName", "ifsc");
        assertThat(def.mapping().get("ifsc")).isEqualTo(new MappingRule.Path(
                new PayloadPath(List.of(segment("AccountDetails"), segment("IFSC")))));
        assertThatThrownBy(() -> def.contract().put("x", TEXT)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> def.mapping().put("x", new MappingRule.NullValue()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("definitionsThatCannotRun")
    void rejectsDefinitionsThatCannotRun(String templateId, int version, Map<String, ContractField> contract,
                                         JsonNode transform, String message) {
        assertThatThrownBy(() -> new DocumentDefinition(ID, templateId, version, "<html/>", contract,
                DateFormats.NONE, transform))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage(message);
    }

    static Stream<Arguments> definitionsThatCannotRun() {
        Map<String, ContractField> contract = Map.of("ifsc", TEXT);
        return Stream.of(
                arguments(" ", 1, contract, transform(), "templateId is blank"),
                arguments("statement", 0, contract, transform(), "version must be 1 or more"),
                arguments("statement", 1, Map.of(), transform(), "contract: needs fields"),
                arguments("statement", 1, contract, JSON.createArrayNode(), "mapping: transform must be an object"));
    }

    private static PayloadPath.Segment segment(String name) {
        return new PayloadPath.Segment(name, List.of());
    }

    private static ObjectNode transform() {
        ObjectNode transform = JSON.createObjectNode();
        transform.put("ifsc", "AccountDetails.IFSC");
        return transform;
    }
}
