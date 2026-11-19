package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.ContractField;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DateFormats;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.FieldType;
import com.joshsoftware.templateDrivenDocumentService.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DefaultMappingEngineTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    // 2024-06-01 10:00 in India
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-06-01T04:30:00Z"), ZoneId.of("Asia/Kolkata"));
    private static final DateFormats STATEMENT_FORMATS = DateFormats.of("yyyyMMdd", "yyyy-MM-dd'T'HH:mm:ss");
    private static final ContractField TEXT = ContractField.scalar(FieldType.STRING, true);

    private final MappingEngine engine = new DefaultMappingEngine(CLOCK);

    // ---- the real statement config ----

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "samples/payload.json, samples/expected-mapped.json",
            "samples/payload-complete.json, samples/expected-mapped-complete.json"})
    void mapsBankPayloadToContractShape(String payload, String expected) {
        JsonNode input = Fixtures.json(payload);

        JsonNode mapped = engine.transform(statement(), input);

        assertThat(mapped).isEqualTo(Fixtures.json(expected));
        // The engine must leave the caller's payload alone
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

    // A template with no dates needs no dateFormats
    @Test
    void mapsWithoutDateFormatsWhenNoSystemCalls() {
        DocumentDefinition noDates = definition(Map.of("v", TEXT), DateFormats.NONE, json("{'v':'A'}"));

        assertThat(engine.transform(noDates, json("{'A':'x'}"))).isEqualTo(json("{'v':'x'}"));
    }

    // ---- helpers ----

    // The contract is not used by transform, so any valid one will do
    private static DocumentDefinition statement() {
        return definition(Map.of("v", TEXT), STATEMENT_FORMATS, Fixtures.json("mapping.json").get("transform"));
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

    // Single quotes keep the tables readable
    private static JsonNode json(String text) {
        return JSON.readTree(text.replace('\'', '"'));
    }
}
