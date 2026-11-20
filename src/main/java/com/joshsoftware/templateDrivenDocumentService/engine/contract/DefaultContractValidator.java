package com.joshsoftware.templateDrivenDocumentService.engine.contract;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.DataContractViolationException;
import tools.jackson.databind.JsonNode;

import java.util.Map;

// Validates and converts in one walk, so what passes is exactly what the template gets
public final class DefaultContractValidator implements ContractValidator {

    @Override
    public Map<String, Object> validate(DocumentDefinition def, JsonNode mapped) {
        ContractWalker walker = new ContractWalker(def.dateFormats());
        Map<String, Object> model = walker.readObject(def.contract(), mapped, "");
        // Every violation at once, so the bank can fix the payload in one round
        if (!walker.violations().isEmpty()) {
            throw new DataContractViolationException(walker.violations());
        }
        return model;
    }
}
