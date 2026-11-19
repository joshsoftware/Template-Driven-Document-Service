package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.ListOf;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.NullValue;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.Path;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.PickOne;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.SystemCall;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.SystemFunction;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import com.joshsoftware.templateDrivenDocumentService.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MappingReaderTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final DateFormats STATEMENT_FORMATS = DateFormats.of("yyyyMMdd", "yyyy-MM-dd'T'HH:mm:ss");
    private static final ContractField TEXT = ContractField.scalar(FieldType.STRING, true);

    // The real statement mapping compiles, and each form becomes its rule type
    @Test
    void compilesTheStatementMapping() {
        Map<String, MappingRule> mapping = load(STATEMENT_FORMATS, Fixtures.json("mapping.json").get("transform"));

        assertThat(mapping.get("ifsc")).isEqualTo(new Path(path("AccountDetails", "IFSC")));
        assertThat(mapping.get("statementDate")).isEqualTo(new SystemCall(SystemFunction.TODAY));
        assertThat(mapping.get("generatedAt")).isEqualTo(new SystemCall(SystemFunction.NOW));
        assertThat(mapping.get("transactions")).isInstanceOf(ListOf.class);
        assertThat(((PickOne) mapping.get("address")).select()).containsEntry("AddressType", "Resedential Address");
    }

    @Test
    void readsNullAndIndexes() {
        Map<String, MappingRule> mapping = load(STATEMENT_FORMATS, json("{'v':null,'w':'A[1][0].B'}"));

        assertThat(mapping.get("v")).isEqualTo(new NullValue());
        assertThat(mapping.get("w")).isEqualTo(new Path(new PayloadPath(List.of(
                new PayloadPath.Segment("A", List.of(1, 0)), new PayloadPath.Segment("B", List.of())))));
    }

    // Shared across requests, so no level of the tree can be changed
    @Test
    void compiledMappingCannotBeChanged() {
        Map<String, MappingRule> mapping = load(STATEMENT_FORMATS, Fixtures.json("mapping.json").get("transform"));
        ListOf transactions = (ListOf) mapping.get("transactions");
        PickOne address = (PickOne) mapping.get("address");

        assertThatThrownBy(() -> mapping.put("x", new NullValue())).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> transactions.map().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> address.select().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> transactions.from().segments().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // No payload here: every rule is checked at load, including ones under a list no payload reaches
    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenRules")
    void rejectsBrokenRuleAtLoadNamingTheField(String scenario, String transform, String message) {
        assertThatThrownBy(() -> load(STATEMENT_FORMATS, json(transform)))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage(message);
    }

    static Stream<Arguments> brokenRules() {
        return Stream.of(
                arguments("empty path segment", "{'v':'A..B'}", "mapping field 'v': bad path"),
                arguments("bad index", "{'v':'A[x]'}", "mapping field 'v': bad path"),
                arguments("index too large", "{'v':'A[99999999999]'}", "mapping field 'v': bad path"),
                arguments("bad path inside a list", "{'v':{'from':'Rows','map':{'amt':'Amt.'}}}",
                        "mapping field 'v.amt': bad path"),
                arguments("bad path inside a pick-one", "{'v':{'from':'A','select':{},'map':{'c':'C..'}}}",
                        "mapping field 'v.c': bad path"),
                arguments("bad rule two forms deep",
                        "{'a':{'from':'X','map':{'b':{'from':'Y','map':{'c':5}}}}}",
                        "mapping field 'a.b.c': must be a path, system function, object or null"),
                arguments("unknown system function", "{'v':'system.yesterday()'}",
                        "mapping field 'v': unknown system function"),
                arguments("system function without ()", "{'v':'system.today'}", "mapping field 'v': bad system function"),
                arguments("number instead of a rule", "{'v':5}",
                        "mapping field 'v': must be a path, system function, object or null"),
                arguments("list without from", "{'v':{'map':{'amt':'Amt'}}}", "mapping field 'v': needs from"),
                arguments("list without map", "{'v':{'from':'Rows'}}", "mapping field 'v': needs map"),
                arguments("list with empty map", "{'v':{'from':'Rows','map':{}}}", "mapping field 'v': needs map"),
                arguments("from with bad path", "{'v':{'from':'Rows..','map':{'amt':'Amt'}}}",
                        "mapping field 'v': bad from path"),
                arguments("unknown key in a form", "{'v':{'from':'Rows','where':'x','map':{'amt':'Amt'}}}",
                        "mapping field 'v': unknown key 'where'"),
                arguments("select value not a string", "{'v':{'from':'Rows','select':{'Type':1},'map':{'amt':'Amt'}}}",
                        "mapping field 'v': select must be an object of strings"));
    }

    // A template with no dates needs no dateFormats; one that calls system.* does, even nested in a list
    @ParameterizedTest(name = "{0}")
    @MethodSource("missingDateFormats")
    void rejectsSystemCallWithoutUsableDateFormat(String scenario, DateFormats formats, String transform,
                                                  String message) {
        assertThatThrownBy(() -> load(formats, json(transform)))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage(message);
    }

    static Stream<Arguments> missingDateFormats() {
        return Stream.of(
                arguments("today without date format", DateFormats.NONE, "{'v':'system.today()'}",
                        "mapping field 'v': dateFormats.date is required"),
                arguments("now without datetime format", DateFormats.of("yyyyMMdd", null), "{'v':'system.now()'}",
                        "mapping field 'v': dateFormats.datetime is required"),
                arguments("today inside a list", DateFormats.NONE,
                        "{'v':{'from':'Rows','map':{'d':'system.today()'}}}",
                        "mapping field 'v.d': dateFormats.date is required"),
                arguments("date format that needs a time", DateFormats.of("yyyyMMdd HH", null),
                        "{'v':'system.today()'}", "mapping field 'v': dateFormats.date cannot print this value"));
    }

    @Test
    void needsNoDateFormatsWithoutSystemCalls() {
        assertThat(load(DateFormats.NONE, json("{'v':'A'}"))).containsKey("v");
    }

    // The definition's own constructor runs the check, so a hand-built mapping is checked too
    @Test
    void checksDatesForPrebuiltMapping() {
        Map<String, MappingRule> mapping = Map.of("v", new SystemCall(SystemFunction.TODAY));

        assertThatThrownBy(() -> new DocumentDefinition(UUID.randomUUID(), "statement", 1, "<html/>",
                Map.of("v", TEXT), DateFormats.NONE, mapping))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage("mapping field 'v': dateFormats.date is required");
    }

    private static Map<String, MappingRule> load(DateFormats formats, JsonNode transform) {
        return new DocumentDefinition(UUID.randomUUID(), "statement", 1, "<html/>", Map.of("v", TEXT), formats,
                transform).mapping();
    }

    private static PayloadPath path(String... names) {
        return new PayloadPath(Stream.of(names).map(name -> new PayloadPath.Segment(name, List.of())).toList());
    }

    // Single quotes keep the tables readable
    private static JsonNode json(String text) {
        return JSON.readTree(text.replace('\'', '"'));
    }
}
