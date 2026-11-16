package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.ContractField;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DateFormats;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.FieldType;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import com.joshsoftware.templateDrivenDocumentService.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DefaultMappingEngineTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    // 2024-06-01 10:00 in India
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-06-01T04:30:00Z"), ZoneId.of("Asia/Kolkata"));
    private static final DateFormats STATEMENT_FORMATS = DateFormats.of("yyyyMMdd", "yyyy-MM-dd'T'HH:mm:ss");
    private static final ContractField TEXT = ContractField.scalar(FieldType.STRING, true);

    private final MappingEngine engine = new DefaultMappingEngine(CLOCK);

    // ---- the real statement config ----

    @Test
    void statementMappingMatchesStatementContract() {
        assertThatCode(() -> engine.checkConfig(statement())).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "samples/payload.json, samples/expected-mapped.json",
            "samples/payload-complete.json, samples/expected-mapped-complete.json"})
    void mapsBankPayloadToContractShape(String payload, String expected) {
        DocumentDefinition def = statement();
        JsonNode mappingBefore = def.transform().deepCopy();
        JsonNode input = Fixtures.json(payload);

        JsonNode mapped = engine.transform(def, input);

        assertThat(mapped).isEqualTo(Fixtures.json(expected));
        // Definitions are shared once loaded; the engine must leave the mapping and the payload alone
        assertThat(def.transform()).isEqualTo(mappingBefore);
        assertThat(input).isEqualTo(Fixtures.json(payload));
    }

    // A request that runs over midnight must still print one date and one time for the document
    @Test
    void readsTheClockOncePerDocument() {
        Instant beforeMidnight = Instant.parse("2024-06-01T18:29:59Z");
        Instant afterMidnight = Instant.parse("2024-06-01T18:30:01Z");
        MappingEngine ticking = new DefaultMappingEngine(clockReading(beforeMidnight, afterMidnight));
        DocumentDefinition def = definition(Map.of("v", TEXT), STATEMENT_FORMATS,
                json("{'d':'system.today()','t':'system.now()'}"));

        assertThat(ticking.transform(def, json("{}")))
                .isEqualTo(json("{'d':'20240601','t':'2024-06-01T23:59:59'}"));
    }

    // ---- transform: one row per behaviour ----

    @ParameterizedTest(name = "{0}")
    @MethodSource("payloadScenarios")
    void mapsEachForm(String scenario, String transform, String payload, String expected) {
        JsonNode mapped = engine.transform(definition(Map.of("v", TEXT), STATEMENT_FORMATS, json(transform)), json(payload));

        assertThat(mapped).isEqualTo(json(expected));
    }

    static Stream<Arguments> payloadScenarios() {
        String list = "{'v':{'from':'Rows','map':{'amt':'Amount'}}}";
        String pick = "{'v':{'from':'Addr','select':{'Type':'R'},'map':{'city':'City'}}}";
        return Stream.of(
                arguments("path reads a nested value", "{'v':'A.B.C'}", "{'A':{'B':{'C':'x'}}}", "{'v':'x'}"),
                arguments("path reads an index", "{'v':'A[1].B'}", "{'A':[{'B':'x'},{'B':'y'}]}", "{'v':'y'}"),
                arguments("[0] reads a single object", "{'v':'A[0].B'}", "{'A':{'B':'x'}}", "{'v':'x'}"),
                arguments("missing key gives null", "{'v':'A.X.C'}", "{'A':{'B':{'C':'x'}}}", "{'v':null}"),
                arguments("index out of range gives null", "{'v':'A[5].B'}", "{'A':[{'B':'x'}]}", "{'v':null}"),
                arguments("path through a string gives null", "{'v':'A.B'}", "{'A':'text'}", "{'v':null}"),
                arguments("payload null stays null", "{'v':'A'}", "{'A':null}", "{'v':null}"),
                arguments("number and array kept as sent", "{'v':'A','w':'L'}", "{'A':12.50,'L':['a','b']}",
                        "{'v':12.50,'w':['a','b']}"),
                arguments("null mapping gives null", "{'v':null}", "{'A':'x'}", "{'v':null}"),
                arguments("system functions use the clock", "{'d':'system.today()','t':'system.now()'}", "{}",
                        "{'d':'20240601','t':'2024-06-01T10:00:00'}"),
                arguments("list maps every item", list, "{'Rows':[{'Amount':'1'},{'Amount':'2'}]}",
                        "{'v':[{'amt':'1'},{'amt':'2'}]}"),
                arguments("list: single object is one row", list, "{'Rows':{'Amount':'1'}}", "{'v':[{'amt':'1'}]}"),
                arguments("list: empty array gives []", list, "{'Rows':[]}", "{'v':[]}"),
                arguments("list: missing source gives null", list, "{}", "{'v':null}"),
                arguments("list: blank string source gives null, not a row of nulls", list, "{'Rows':''}", "{'v':null}"),
                arguments("list: number source gives null", list, "{'Rows':5}", "{'v':null}"),
                arguments("select: blank string source gives null", pick, "{'Addr':''}", "{'v':null}"),
                arguments("select picks the matching item", pick,
                        "{'Addr':[{'Type':'P','City':'Pune'},{'Type':'R','City':'Mumbai'}]}", "{'v':{'city':'Mumbai'}}"),
                arguments("select falls back to the first item", pick,
                        "{'Addr':[{'Type':'P','City':'Pune'},{'Type':'X','City':'Mumbai'}]}", "{'v':{'city':'Pune'}}"),
                arguments("select on empty list gives null", pick, "{'Addr':[]}", "{'v':null}"),
                arguments("select on missing source gives null", pick, "{}", "{'v':null}"),
                arguments("select {} maps a 3-level nested object",
                        "{'a':{'from':'X','select':{},'map':{'b':{'from':'Y','select':{},'map':{'c':'Z'}}}}}",
                        "{'X':{'Y':{'Z':'deep'}}}", "{'a':{'b':{'c':'deep'}}}"));
    }

    // ---- checkConfig: one base config, each row breaks one thing ----

    // Covers every form: path, system, list, select, nested select {}
    private static final String VALID = """
            {'name':'Customer.Name', 'today':'system.today()', 'note':null,
             'rows':{'from':'Rows','map':{'amount':'Amt'}},
             'address':{'from':'Addr','select':{'Type':'R'},
                        'map':{'city':'City','geo':{'from':'Geo','select':{},'map':{'pin':'Pin','area':'Area'}}}}}""";

    @Test
    void acceptsEveryFormWhenItMatchesTheContract() {
        assertThatCode(() -> engine.checkConfig(checkDefinition(json(VALID), STATEMENT_FORMATS)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenConfigs")
    void rejectsBrokenConfigNamingTheField(String scenario, String pointer, String value, String message) {
        ObjectNode transform = (ObjectNode) json(VALID);
        patch(transform, pointer, value);

        assertThatThrownBy(() -> engine.checkConfig(checkDefinition(transform, STATEMENT_FORMATS)))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage(message);
    }

    static Stream<Arguments> brokenConfigs() {
        return Stream.of(
                arguments("empty path segment", "/name", "'A..B'", "mapping field 'name': bad path"),
                arguments("bad index", "/name", "'A[x]'", "mapping field 'name': bad path"),
                arguments("index too large", "/name", "'A[99999999999]'", "mapping field 'name': bad path"),
                arguments("nested bad path", "/rows/map/amount", "'Amt.'", "mapping field 'rows.amount': bad path"),
                arguments("unknown system function", "/today", "'system.yesterday()'",
                        "mapping field 'today': unknown system function"),
                arguments("system function without ()", "/today", "'system.today'",
                        "mapping field 'today': bad system function"),
                arguments("number instead of a rule", "/name", "5",
                        "mapping field 'name': must be a path, system function, object or null"),
                arguments("list without from", "/rows/from", null, "mapping field 'rows': needs from"),
                arguments("list without map", "/rows/map", null, "mapping field 'rows': needs map"),
                arguments("from with bad path", "/rows/from", "'Rows..'", "mapping field 'rows': bad from path"),
                arguments("system.now() on a date field", "/today", "'system.now()'",
                        "mapping field 'today': system.now() needs a datetime field"),
                arguments("system.today() on a text field", "/name", "'system.today()'",
                        "mapping field 'name': system.today() needs a date field"),
                arguments("unknown key in a form", "/rows/where", "'x'", "mapping field 'rows': unknown key 'where'"),
                arguments("select value not a string", "/address/select", "{'Type':1}",
                        "mapping field 'address': select must be an object of strings"),
                arguments("select on an array field", "/rows/select", "{}",
                        "mapping field 'rows': select needs an object field"),
                arguments("list on an object field", "/address/select", null,
                        "mapping field 'address': from/map needs an array of objects field"),
                arguments("key not in contract", "/extra", "'X'", "mapping field 'extra': not in contract"),
                arguments("nested key not in contract", "/address/map/zip", "'Zip'",
                        "mapping field 'address.zip': not in contract"),
                arguments("contract field not mapped", "/name", null,
                        "mapping field 'name': has no mapping (use null if not supplied)"),
                arguments("nested contract field not mapped", "/address/map/geo/map/pin", null,
                        "mapping field 'address.geo.pin': has no mapping (use null if not supplied)"));
    }

    // A template with no dates needs no dateFormats, one that calls system.* does
    @Test
    void requiresDateFormatsOnlyWhenUsed() {
        ObjectNode noDates = (ObjectNode) json(VALID);
        noDates.putNull("today");

        assertThatCode(() -> engine.checkConfig(checkDefinition(noDates, DateFormats.NONE)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> engine.checkConfig(checkDefinition(json(VALID), DateFormats.NONE)))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage("mapping field 'today': dateFormats.date is required");
    }

    // ---- helpers ----

    private static DocumentDefinition checkDefinition(JsonNode transform, DateFormats formats) {
        Map<String, ContractField> contract = new LinkedHashMap<>();
        contract.put("name", TEXT);
        contract.put("today", ContractField.scalar(FieldType.DATE, true));
        contract.put("note", ContractField.scalar(FieldType.STRING, false));
        contract.put("rows", ContractField.array(true,
                ContractField.object(false, Map.of("amount", ContractField.scalar(FieldType.NUMBER, true)))));
        contract.put("address", ContractField.object(true, Map.of(
                "city", TEXT,
                "geo", ContractField.object(false, Map.of("pin", TEXT, "area", TEXT)))));
        return definition(contract, formats, transform);
    }

    // Hand-built copy of statement/v1/contract.json until the parser exists (step 5)
    private static DocumentDefinition statement() {
        ContractField date = ContractField.scalar(FieldType.DATE, true);
        ContractField number = ContractField.scalar(FieldType.NUMBER, true);
        Map<String, ContractField> address = new LinkedHashMap<>();
        address.put("lines", ContractField.array(true, ContractField.scalar(FieldType.STRING, false)));
        address.put("city", TEXT);
        address.put("state", TEXT);
        address.put("pinCode", TEXT);
        address.put("country", ContractField.scalar(FieldType.STRING, false));
        Map<String, ContractField> row = new LinkedHashMap<>();
        row.put("transactionDate", date);
        row.put("valueDate", date);
        row.put("description", TEXT);
        row.put("amount", number);
        row.put("drCr", ContractField.enumOf(true, List.of("D", "C")));
        row.put("balance", number);

        Map<String, ContractField> contract = new LinkedHashMap<>();
        contract.put("customerName", TEXT);
        contract.put("address", ContractField.object(true, address));
        contract.put("statementDate", date);
        for (String key : List.of("accountNumber", "accountType", "branchDetails", "customerId", "ifsc", "micrCode")) {
            contract.put(key, TEXT);
        }
        contract.put("periodFrom", date);
        contract.put("periodTo", date);
        contract.put("transactions", ContractField.array(true, ContractField.object(false, row)));
        for (String key : List.of("openingBalance", "totalCredits", "totalDebits", "closingBalance")) {
            contract.put(key, number);
        }
        contract.put("generatedAt", ContractField.scalar(FieldType.DATETIME, true));

        JsonNode mapping = Fixtures.json("mapping.json");
        return definition(contract, STATEMENT_FORMATS, mapping.get("transform"));
    }

    private static DocumentDefinition definition(Map<String, ContractField> contract, DateFormats formats,
                                                 JsonNode transform) {
        return new DocumentDefinition(UUID.randomUUID(), "statement", 1, "<html/>", contract, formats, transform);
    }

    // Gives the first instant, then the second for every later read
    private static Clock clockReading(Instant first, Instant later) {
        return new Clock() {
            private boolean read;

            @Override
            public ZoneId getZone() {
                return CLOCK.getZone();
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                Instant now = read ? later : first;
                read = true;
                return now;
            }
        };
    }

    // Sets or (when value is null) removes the node at a JSON pointer
    private static void patch(ObjectNode root, String pointer, String value) {
        int slash = pointer.lastIndexOf('/');
        ObjectNode parent = (ObjectNode) root.at(pointer.substring(0, slash));
        String key = pointer.substring(slash + 1);
        if (value == null) {
            parent.remove(key);
        } else {
            parent.set(key, json(value));
        }
    }

    // Single quotes keep the tables readable
    private static JsonNode json(String text) {
        return JSON.readTree(text.replace('\'', '"'));
    }
}
