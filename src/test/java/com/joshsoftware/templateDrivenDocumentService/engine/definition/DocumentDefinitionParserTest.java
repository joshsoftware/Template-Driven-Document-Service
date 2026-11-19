package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import com.joshsoftware.templateDrivenDocumentService.support.Fixtures;
import com.joshsoftware.templateDrivenDocumentService.support.StatementDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DocumentDefinitionParserTest {

    private static final ContractField TEXT = ContractField.scalar(FieldType.STRING, true);
    private static final DocumentDefinitionParser PARSER = new DocumentDefinitionParser(JsonMapper.builder().build());
    private static final String CONTRACT = "{'fields':{'n':{'type':'string'}}}";
    private static final String MAPPING = "{'transform':{'n':'N'}}";

    @Test
    void readsTheStatementDefinition() {
        DocumentDefinition def = StatementDefinition.load();

        assertThat(def.templateVersionId()).isEqualTo(StatementDefinition.VERSION_ID);
        assertThat(def.templateId()).isEqualTo("statement");
        assertThat(def.version()).isEqualTo(1);
        assertThat(def.contract()).isEqualTo(statementContract());
        // File order is kept at every level
        assertThat(def.contract().keySet()).containsExactlyElementsOf(statementContract().keySet());
        assertThat(def.contract().get("transactions").items().fields().keySet())
                .containsExactly("transactionDate", "valueDate", "description", "amount", "drCr", "balance");
        assertThat(def.dateFormats().datePattern()).isEqualTo("yyyyMMdd");
        assertThat(def.dateFormats().datetimePattern()).isEqualTo("yyyy-MM-dd'T'HH:mm:ss");
        Map<String, MappingRule> mapping = MappingReader.read(Fixtures.json("mapping.json").get("transform"));
        assertThat(def.mapping()).isEqualTo(mapping);
        assertThat(def.mapping().keySet()).containsExactlyElementsOf(mapping.keySet());
    }

    // Absent required means optional; a template with no dates needs no dateFormats
    @Test
    void appliesDefaultsForOptionalKeys() {
        DocumentDefinition def = new DocumentDefinitionParser(JsonMapper.builder().build())
                .parse(UUID.randomUUID(), "certificate", 1, "<html/>",
                        "{\"fields\":{\"name\":{\"type\":\"string\"}}}", "{\"transform\":{\"name\":\"N\"}}");

        assertThat(def.contract().get("name").required()).isFalse();
        assertThat(def.dateFormats()).isEqualTo(DateFormats.NONE);
    }

    // Every broken file is a config error naming the file and field, never a raw Jackson or null error
    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenConfigs")
    void rejectsBrokenConfig(String scenario, String contract, String mapping, String message) {
        assertThatThrownBy(() -> PARSER.parse(UUID.randomUUID(), "t", 1, "<html/>", json(contract), json(mapping)))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage(message);
    }

    // Single quotes keep the table readable; null stays null
    private static String json(String text) {
        return text == null ? null : text.replace('\'', '"');
    }

    static Stream<Arguments> brokenConfigs() {
        return Stream.of(
                // whole files
                arguments("mapping not valid JSON", CONTRACT, "{'transform':", "mapping: is not valid JSON"),
                arguments("contract not valid JSON", "{'fields':", MAPPING, "contract: is not valid JSON"),
                arguments("mapping empty", CONTRACT, " ", "mapping: is empty"),
                arguments("contract null", null, MAPPING, "contract: is empty"),
                arguments("mapping is an array", CONTRACT, "[]", "mapping: must be a JSON object"),
                arguments("duplicate key", "{'fields':{'n':{'type':'string'},'n':{'type':'number'}}}", MAPPING,
                        "contract: is not valid JSON"),
                // mapping.json
                arguments("transform missing", CONTRACT, "{}", "mapping: transform is required"),
                arguments("unknown mapping key", CONTRACT, "{'dateformats':{},'transform':{'n':'N'}}",
                        "mapping: unknown key 'dateformats'"),
                arguments("dateFormats not an object", CONTRACT, "{'dateFormats':'yyyyMMdd','transform':{'n':'N'}}",
                        "mapping: dateFormats must be an object"),
                arguments("unknown dateFormats key", CONTRACT, "{'dateFormats':{'time':'HH'},'transform':{'n':'N'}}",
                        "mapping: dateFormats: unknown key 'time'"),
                arguments("date pattern a number", CONTRACT, "{'dateFormats':{'date':8},'transform':{'n':'N'}}",
                        "mapping: dateFormats.date must be a pattern string"),
                arguments("date pattern null", CONTRACT, "{'dateFormats':{'date':null},'transform':{'n':'N'}}",
                        "mapping: dateFormats.date must be a pattern string"),
                // contract.json
                arguments("unknown contract key", "{'fields':{'n':{'type':'string'}},'name':'x'}", MAPPING,
                        "contract: unknown key 'name'"),
                arguments("fields missing", "{}", MAPPING, "contract: fields must be an object"),
                arguments("typo in a field key", "{'fields':{'n':{'type':'string','requried':true}}}", MAPPING,
                        "contract field 'n': unknown key 'requried'"),
                arguments("required not a boolean", "{'fields':{'n':{'type':'string','required':'yes'}}}", MAPPING,
                        "contract field 'n': required must be true or false"),
                arguments("field not an object", "{'fields':{'n':'string'}}", MAPPING,
                        "contract field 'n': must be an object"),
                arguments("nested shape error names the path",
                        "{'fields':{'a':{'type':'object','fields':{'b':{'type':'enum'}}}}}", MAPPING,
                        "contract field 'a.b': ENUM needs values"),
                arguments("array item error names the path",
                        "{'fields':{'a':{'type':'array','items':{'type':'object'}}}}", MAPPING,
                        "contract field 'a[]': OBJECT needs fields"),
                arguments("values not strings", "{'fields':{'n':{'type':'enum','values':['D',1]}}}", MAPPING,
                        "contract field 'n': values must be an array of strings"),
                arguments("nested fields not an object", "{'fields':{'a':{'type':'object','fields':[]}}}", MAPPING,
                        "contract field 'a': fields must be an object"));
    }

    // statement/v1/contract.json written out by hand, to compare with what the parser builds
    private static Map<String, ContractField> statementContract() {
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
        return contract;
    }
}
