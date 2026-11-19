package com.joshsoftware.templateDrivenDocumentService.engine.contract;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.ContractField;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DateFormats;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.FieldType;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.DataContractViolationException;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import com.joshsoftware.templateDrivenDocumentService.support.Fixtures;
import com.joshsoftware.templateDrivenDocumentService.support.StatementDefinition;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DefaultContractValidatorTest {

    // Same setting the API uses, so JSON decimals arrive as exact BigDecimal
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();
    private static final DateFormats FORMATS = DateFormats.of("yyyyMMdd", "yyyy-MM-dd'T'HH:mm:ss");
    private static final DocumentDefinition STATEMENT = StatementDefinition.load();

    private static final ContractField TEXT = ContractField.scalar(FieldType.STRING, true);
    private static final ContractField OPTIONAL_TEXT = ContractField.scalar(FieldType.STRING, false);
    private static final ContractField NUMBER = ContractField.scalar(FieldType.NUMBER, true);
    private static final ContractField DATE = ContractField.scalar(FieldType.DATE, true);
    private static final ContractField DATETIME = ContractField.scalar(FieldType.DATETIME, true);
    private static final ContractField DR_CR = ContractField.enumOf(true, List.of("D", "C"));

    private final ContractValidator validator = new DefaultContractValidator();

    // ---- the real statement ----

    // payload.json has no customer details or summary: violations follow contract order, children not listed
    @Test
    void reportsEveryMissingStatementField() {
        assertViolations(STATEMENT, Fixtures.json("samples/expected-mapped.json"), Stream.of(
                        "customerName", "address", "accountType", "branchDetails", "customerId", "ifsc", "micrCode",
                        "openingBalance", "totalCredits", "totalDebits", "closingBalance")
                .map(field -> tuple(field, "REQUIRED")).toArray(Tuple[]::new));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsTheTypedStatementModel() {
        Map<String, Object> model = validator.validate(STATEMENT, Fixtures.json("samples/expected-mapped-complete.json"));

        assertThat(model.keySet()).containsExactlyElementsOf(STATEMENT.contract().keySet());
        assertThat(model)
                .containsEntry("customerName", "TEST CUSTOMER")
                .containsEntry("statementDate", LocalDate.of(2024, 6, 1))
                .containsEntry("generatedAt", LocalDateTime.of(2024, 6, 1, 10, 0))
                .containsEntry("openingBalance", new BigDecimal("0.00"))
                .containsEntry("closingBalance", new BigDecimal("14200.00"));
        Map<String, Object> address = (Map<String, Object>) model.get("address");
        assertThat(address)
                .containsEntry("lines", List.of("12 TEST ROAD", "TEST AREA", "TEST LANDMARK"))
                .containsEntry("country", "IN");
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) model.get("transactions");
        assertThat(transactions).hasSize(2);
        // BigDecimal.equals compares scale too, so this proves 14400.00 kept its two decimals
        assertThat(transactions.getFirst())
                .containsEntry("transactionDate", LocalDate.of(2024, 4, 10))
                .containsEntry("amount", new BigDecimal("14400"))
                .containsEntry("balance", new BigDecimal("14400.00"))
                .containsEntry("drCr", "C");
        // The template gets a read-only model
        assertThatThrownBy(() -> model.put("x", 1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> transactions.add(Map.of())).isInstanceOf(UnsupportedOperationException.class);
    }

    // ---- one row per rule ----

    @ParameterizedTest(name = "{0}")
    @MethodSource("ruleScenarios")
    void appliesEachRule(String scenario, ContractField field, String raw, Object expected) {
        DocumentDefinition def = definition(Map.of("v", field), FORMATS);
        JsonNode mapped = raw == null ? json("{}") : json("{'v':" + raw + "}");

        if (expected instanceof ViolationRule rule) {
            assertViolations(def, mapped, tuple("v", rule.name()));
        } else {
            assertThat(validator.validate(def, mapped).get("v")).isEqualTo(expected);
        }
    }

    static Stream<Arguments> ruleScenarios() {
        ContractField address = ContractField.object(true, Map.of("city", TEXT));
        ContractField lines = ContractField.array(true, TEXT);
        return Stream.of(
                // missing and blank
                arguments("required, missing", TEXT, null, ViolationRule.REQUIRED),
                arguments("required, null", TEXT, "null", ViolationRule.REQUIRED),
                arguments("required, empty", TEXT, "''", ViolationRule.REQUIRED),
                arguments("required, spaces only", TEXT, "'   '", ViolationRule.REQUIRED),
                arguments("required object, blank", address, "''", ViolationRule.REQUIRED),
                arguments("optional, missing", OPTIONAL_TEXT, null, null),
                arguments("optional, empty", OPTIONAL_TEXT, "''", null),
                // string
                arguments("text", TEXT, "'TEST'", "TEST"),
                arguments("text keeps its spaces", TEXT, "' A B '", " A B "),
                arguments("JSON number for text", TEXT, "10000000000001", ViolationRule.WRONG_TYPE),
                arguments("object for text", TEXT, "{}", ViolationRule.WRONG_TYPE),
                // number
                arguments("number text keeps scale", NUMBER, "'14400.00'", new BigDecimal("14400.00")),
                arguments("JSON decimal keeps scale", NUMBER, "14400.10", new BigDecimal("14400.10")),
                arguments("zero is not blank", NUMBER, "'0.00'", new BigDecimal("0.00")),
                arguments("negative", NUMBER, "'-200.50'", new BigDecimal("-200.50")),
                arguments("letters", NUMBER, "'abc'", ViolationRule.NOT_A_NUMBER),
                arguments("thousands separator", NUMBER, "'1,000.00'", ViolationRule.NOT_A_NUMBER),
                arguments("padded number", NUMBER, "' 14400'", ViolationRule.NOT_A_NUMBER),
                arguments("boolean", NUMBER, "true", ViolationRule.NOT_A_NUMBER),
                // date and datetime
                arguments("date", DATE, "'20240410'", LocalDate.of(2024, 4, 10)),
                arguments("31 February", DATE, "'20240231'", ViolationRule.BAD_DATE),
                arguments("date in another format", DATE, "'2024-04-10'", ViolationRule.BAD_DATE),
                arguments("date as JSON number", DATE, "20240410", ViolationRule.BAD_DATE),
                arguments("datetime", DATETIME, "'2024-06-01T10:00:00'", LocalDateTime.of(2024, 6, 1, 10, 0)),
                arguments("datetime with a space", DATETIME, "'2024-06-01 10:00:00'", ViolationRule.BAD_DATETIME),
                // enum
                arguments("enum value", DR_CR, "'C'", "C"),
                arguments("enum, other value", DR_CR, "'X'", ViolationRule.NOT_ALLOWED),
                arguments("enum, wrong case", DR_CR, "'c'", ViolationRule.NOT_ALLOWED),
                // shape
                arguments("text for object", address, "'text'", ViolationRule.WRONG_TYPE),
                arguments("object for list", lines, "{}", ViolationRule.WRONG_TYPE));
    }

    // ---- nesting, privacy, config ----

    // A missing parent is one violation; items and nested fields carry their full path
    @Test
    void reportsNestedFieldsByPath() {
        Map<String, ContractField> contract = new LinkedHashMap<>();
        contract.put("address", ContractField.object(true, Map.of("city", TEXT, "pinCode", TEXT)));
        contract.put("transactions", ContractField.array(true,
                ContractField.object(false, Map.of("amount", NUMBER, "drCr", DR_CR))));
        contract.put("lines", ContractField.array(true, TEXT));
        JsonNode mapped = json("""
                {'address':null,
                 'transactions':[{'amount':'1','drCr':'C'},{'amount':'x','drCr':'X'}],
                 'lines':['a',5,'']}""");

        assertThatThrownBy(() -> validator.validate(definition(contract, FORMATS), mapped))
                .isInstanceOfSatisfying(DataContractViolationException.class, e ->
                        assertThat(e.getViolations()).extracting(Violation::getField, Violation::getRule)
                                .containsExactlyInAnyOrder(
                                        tuple("address", "REQUIRED"),
                                        tuple("transactions[1].amount", "NOT_A_NUMBER"),
                                        tuple("transactions[1].drCr", "NOT_ALLOWED"),
                                        tuple("lines[1]", "WRONG_TYPE"),
                                        tuple("lines[2]", "REQUIRED")));
    }

    // Violations go back to the bank and into logs, so they must never echo customer data
    @Test
    void neverEchoesPayloadValues() {
        DocumentDefinition def = definition(Map.of("amount", NUMBER, "drCr", DR_CR), FORMATS);

        assertThatThrownBy(() -> validator.validate(def, json("{'amount':'SECRET-123','drCr':'SECRET-X'}")))
                .isInstanceOfSatisfying(DataContractViolationException.class, e -> {
                    assertThat(e.getMessage()).doesNotContain("SECRET");
                    assertThat(e.getViolations()).allSatisfy(v -> {
                        assertThat(v.getField()).doesNotContain("SECRET");
                        assertThat(v.getRule()).doesNotContain("SECRET");
                    });
                });
    }

    // A date field without its pattern is our config bug, caught at load, before any payload arrives
    @ParameterizedTest(name = "{0}")
    @MethodSource("missingDateFormats")
    void rejectsDateFieldWithoutPatternAtLoad(String scenario, Map<String, ContractField> contract, DateFormats formats,
                                              String message) {
        assertThatThrownBy(() -> definition(contract, formats))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage(message);
    }

    static Stream<Arguments> missingDateFormats() {
        ContractField optionalDate = ContractField.scalar(FieldType.DATE, false);
        return Stream.of(
                arguments("date", Map.of("statementDate", DATE), DateFormats.NONE,
                        "contract field 'statementDate': DATE needs dateFormats.date"),
                arguments("optional date still needs it", Map.of("periodFrom", optionalDate), DateFormats.NONE,
                        "contract field 'periodFrom': DATE needs dateFormats.date"),
                arguments("datetime", Map.of("generatedAt", DATETIME), DateFormats.of("yyyyMMdd", null),
                        "contract field 'generatedAt': DATETIME needs dateFormats.datetime"),
                arguments("inside an object", Map.of("a", ContractField.object(true, Map.of("d", DATE))),
                        DateFormats.NONE, "contract field 'a.d': DATE needs dateFormats.date"),
                arguments("inside list rows", Map.of("rows", ContractField.array(true,
                                ContractField.object(false, Map.of("d", DATE)))),
                        DateFormats.NONE, "contract field 'rows[].d': DATE needs dateFormats.date"),
                arguments("list of dates", Map.of("days", ContractField.array(true, DATE)),
                        DateFormats.NONE, "contract field 'days[]': DATE needs dateFormats.date"));
    }

    // ---- helpers ----

    private void assertViolations(DocumentDefinition def, JsonNode mapped, Tuple... expected) {
        assertThatThrownBy(() -> validator.validate(def, mapped))
                .isInstanceOfSatisfying(DataContractViolationException.class, e ->
                        assertThat(e.getViolations()).extracting(Violation::getField, Violation::getRule)
                                .containsExactly(expected));
    }

    private static DocumentDefinition definition(Map<String, ContractField> contract, DateFormats formats) {
        return new DocumentDefinition(UUID.randomUUID(), "statement", 1, "<html/>", contract, formats,
                JSON.createObjectNode());
    }

    // Single quotes keep the tables readable
    private static JsonNode json(String text) {
        return JSON.readTree(text.replace('\'', '"'));
    }
}
