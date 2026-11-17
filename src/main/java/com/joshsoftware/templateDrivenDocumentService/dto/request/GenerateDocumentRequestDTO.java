package com.joshsoftware.templateDrivenDocumentService.dto.request;

import com.joshsoftware.templateDrivenDocumentService.annotation.NonEmptyJsonObject;
import com.joshsoftware.templateDrivenDocumentService.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

// Request body for POST /api/v1/generate-statement.
// The envelope is the same for every document type; only payload differs.
// Failed checks are turned into a 400 INVALID_REQUEST by GlobalExceptionHandler.
@Getter
@Setter
@NoArgsConstructor
public class GenerateDocumentRequestDTO {

    // Id of the template to generate the document from. Letters and digits only, 1 to 50 characters.
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{1,50}$")
    private String templateId;

    // One of MOBILE (mobile banking), NET_BANKING (net banking), WHATSAPP (WhatsApp banking).
    // An unknown value cannot be read into the enum and gives 400 INVALID_REQUEST.
    @NotNull
    private Channel channel;

    // Document data. Must be a non-empty JSON object; null, {} or a non-object give 400 INVALID_REQUEST.
    // Its fields are dynamic and checked later against the data contract, not here.
    // Kept as JsonNode (not Map) so decimals are read as BigDecimal and keep their exact value and scale
    // (see spring.jackson.deserialization.use-big-decimal-for-floats), and the engine can use it as is.
    // Holds customer data: never log it.
    @NonEmptyJsonObject
    private JsonNode payload;
}
