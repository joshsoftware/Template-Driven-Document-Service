package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ContractFieldTest {

    private static final ContractField TEXT = ContractField.scalar(FieldType.STRING, true);

    // The statement's transactions rule: an array whose items are objects
    @Test
    void describesStatementTransactionsAsArrayOfObjects() {
        Map<String, ContractField> row = new LinkedHashMap<>();
        row.put("transactionDate", ContractField.scalar(FieldType.DATE, true));
        row.put("description", TEXT);
        row.put("amount", ContractField.scalar(FieldType.NUMBER, true));
        row.put("drCr", ContractField.enumOf(true, List.of("D", "C")));

        ContractField transactions = ContractField.array(true, ContractField.object(false, row));

        assertThat(transactions.type()).isEqualTo(FieldType.ARRAY);
        assertThat(transactions.items().type()).isEqualTo(FieldType.OBJECT);
        assertThat(transactions.items().fields().keySet())
                .containsExactly("transactionDate", "description", "amount", "drCr");
        assertThat(transactions.items().fields().get("drCr").values()).containsExactly("D", "C");
    }

    // A loaded contract must not change if the caller later edits its own map or list
    @Test
    void isNotAffectedByLaterChangesToCallerCollections() {
        Map<String, ContractField> fields = new LinkedHashMap<>(Map.of("city", TEXT));
        List<String> values = new ArrayList<>(List.of("D", "C"));

        ContractField address = ContractField.object(true, fields);
        ContractField drCr = ContractField.enumOf(true, values);
        fields.put("added", TEXT);
        values.add("X");

        assertThat(address.fields()).containsOnlyKeys("city");
        assertThat(drCr.values()).containsExactly("D", "C");
        assertThatThrownBy(() -> address.fields().put("x", TEXT)).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rulesThatCannotBeChecked")
    void rejectsRulesThatCannotBeChecked(Executable build, String detail) {
        assertThatThrownBy(build::execute)
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage("contract field: " + detail);
    }

    static Stream<Arguments> rulesThatCannotBeChecked() {
        return Stream.of(
                arguments(named("object without fields", (Executable) () -> ContractField.object(true, Map.of())),
                        "OBJECT needs fields"),
                arguments(named("array without items", (Executable) () -> ContractField.array(true, null)),
                        "ARRAY needs items"),
                arguments(named("enum without values", (Executable) () -> ContractField.enumOf(true, List.of())),
                        "ENUM needs values"),
                arguments(named("enum with duplicate values", (Executable) () -> ContractField.enumOf(true, List.of("D", "D"))),
                        "ENUM values must be distinct"),
                arguments(named("string with enum values",
                                (Executable) () -> new ContractField(FieldType.STRING, true, null, null, List.of("A"))),
                        "STRING allows no fields, items or values"),
                arguments(named("object with items",
                                (Executable) () -> new ContractField(FieldType.OBJECT, true, Map.of("a", TEXT), TEXT, null)),
                        "OBJECT allows only fields"),
                arguments(named("enum with items",
                                (Executable) () -> new ContractField(FieldType.ENUM, true, null, TEXT, List.of("A"))),
                        "ENUM allows only values"));
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({"string, STRING", "number, NUMBER", "date, DATE", "datetime, DATETIME",
            "enum, ENUM", "array, ARRAY", "object, OBJECT"})
    void readsTypeNamesFromContractJson(String name, FieldType expected) {
        assertThat(FieldType.fromJson(name, "field")).isEqualTo(expected);
    }

    // The message names the field so the config author knows where to look
    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"strng", "STRING", "boolean"})
    void rejectsUnknownTypeNamingTheField(String name) {
        assertThatThrownBy(() -> FieldType.fromJson(name, "address.city"))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage("contract field 'address.city': unknown type");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsMissingType(String name) {
        assertThatThrownBy(() -> FieldType.fromJson(name, "ifsc"))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage("contract field 'ifsc': type is required");
    }
}
