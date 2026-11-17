package com.joshsoftware.templateDrivenDocumentService.controller;

import com.joshsoftware.templateDrivenDocumentService.constants.MdcConstants;
import com.joshsoftware.templateDrivenDocumentService.dto.ApiResponse;
import com.joshsoftware.templateDrivenDocumentService.dto.request.GenerateDocumentRequestDTO;
import jakarta.validation.Valid;
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
public class GenerateDocumentController {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> generateDocument(@Valid @RequestBody GenerateDocumentRequestDTO request) {
        long start = System.currentTimeMillis();
        // Never log the payload: it holds customer data.
        log.info("[GenerateDocumentController#generateDocument] start templateId={} channel={}",
                request.getTemplateId(), request.getChannel());

        String requestId = MDC.get(MdcConstants.REQUEST_ID);
        // TODO: call DocumentGenerationService.generate(request, requestId) and return
        //  GenerateDocumentResponseDTO(jobId, status) once the service exists.

        log.info("[GenerateDocumentController#generateDocument] end templateId={} requestId={} durationMs={}",
                request.getTemplateId(), requestId, System.currentTimeMillis() - start);
        return ResponseEntity.ok(ApiResponse.success("Document generation request accepted", null));
    }
}
