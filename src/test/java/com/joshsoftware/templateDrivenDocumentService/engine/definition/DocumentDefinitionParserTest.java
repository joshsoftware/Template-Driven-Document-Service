package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.support.Fixtures;
import com.joshsoftware.templateDrivenDocumentService.support.StatementDefinition;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentDefinitionParserTest {

    private static final ContractField TEXT = ContractField.scalar(FieldType.STRING, true);

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
