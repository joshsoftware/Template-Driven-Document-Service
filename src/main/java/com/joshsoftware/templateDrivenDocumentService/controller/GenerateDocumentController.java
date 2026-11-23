package com.joshsoftware.templateDrivenDocumentService.controller;

import com.joshsoftware.templateDrivenDocumentService.constants.MdcConstants;
import com.joshsoftware.templateDrivenDocumentService.dto.ApiResponse;
import com.joshsoftware.templateDrivenDocumentService.dto.request.GenerateDocumentRequestDTO;
import com.joshsoftware.templateDrivenDocumentService.dto.response.GenerateDocumentResponseDTO;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Generate document API.
// No try/catch here: every exception goes to GlobalExceptionHandler, which returns an ErrorResponse
// with the request id. Bad JSON, a failed @Valid check, a wrong method or a wrong content type
// are all handled there.
@Slf4j
@RestController
@RequestMapping("/api/v1/generate-document")
@RequiredArgsConstructor
public class GenerateDocumentController {

    private static final String DOCUMENT_GENERATED_MESSAGE = "Document generated";

    private final DocumentGenerationService documentGenerationService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<GenerateDocumentResponseDTO>> generateDocument(@Valid @RequestBody GenerateDocumentRequestDTO request) {
        long start = System.currentTimeMillis();
        // Never log the payload: it holds customer data.
        log.info("[GenerateDocumentController#generateDocument] start templateId={} channel={}",
                request.getTemplateId(), request.getChannel());

        String requestId = MDC.get(MdcConstants.REQUEST_ID);
        GenerateDocumentResponseDTO response = documentGenerationService.generate(request, requestId);

        log.info("[GenerateDocumentController#generateDocument] end templateId={} requestId={} durationMs={}",
                request.getTemplateId(), requestId, System.currentTimeMillis() - start);
        return ResponseEntity.ok(ApiResponse.success(DOCUMENT_GENERATED_MESSAGE, response));
    }
}
