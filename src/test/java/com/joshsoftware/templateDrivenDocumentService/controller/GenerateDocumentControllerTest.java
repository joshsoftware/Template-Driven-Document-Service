package com.joshsoftware.templateDrivenDocumentService.controller;

import com.joshsoftware.templateDrivenDocumentService.constants.HeaderConstants;
import com.joshsoftware.templateDrivenDocumentService.exception.handler.GlobalExceptionHandler;
import com.joshsoftware.templateDrivenDocumentService.util.filter.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs the controller with the real RequestIdFilter and GlobalExceptionHandler.
@WebMvcTest(GenerateDocumentController.class)
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class GenerateDocumentControllerTest {

    private static final String URL = "/api/v1/generate-statement";
    private static final String VALID_BODY = """
            {"templateId":"123dbjhbfjh","channel":"MOBILE",
             "payload":{"userId":"2026","templateId":"generate-statement"}}""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validRequestReturnsApiResponse() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.X_REQUEST_ID, "req-1")
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(header().string(HeaderConstants.X_REQUEST_ID, "req-1"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Document generation request accepted"));
    }

    @Test
    void missingFieldsGiveInvalidRequestWithViolations() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.X_REQUEST_ID, "req-2")
                        .content("{\"templateId\":\"Bad Id!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.requestId").value("req-2"))
                .andExpect(jsonPath("$.violations.length()").value(3))
                .andExpect(jsonPath("$.violations[?(@.field=='channel')].rule").value("NotNull"))
                .andExpect(jsonPath("$.violations[?(@.field=='payload')].rule").value("NotEmpty"))
                .andExpect(jsonPath("$.violations[?(@.field=='templateId')].rule").value("Pattern"));
    }

    @Test
    void malformedJsonGivesInvalidRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void nonObjectPayloadGivesInvalidRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":\"123dbjhbfjh\",\"channel\":\"MOBILE\",\"payload\":\"text\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void emptyPayloadGivesInvalidRequest() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":\"123dbjhbfjh\",\"channel\":\"MOBILE\",\"payload\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.violations[0].field").value("payload"))
                .andExpect(jsonPath("$.violations[0].rule").value("NotEmpty"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MOBILE", "NET_BANKING", "WHATSAPP"})
    void supportedChannelsAreAccepted(String channel) throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":\"123dbjhbfjh\",\"channel\":\"" + channel + "\",\"payload\":{\"a\":1}}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FAX", "API", "mobile", ""})
    void unsupportedChannelGivesInvalidRequest(String channel) throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":\"123dbjhbfjh\",\"channel\":\"" + channel + "\",\"payload\":{\"a\":1}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc-123", "abc_123", "abc 123", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void invalidTemplateIdGivesInvalidRequest(String templateId) throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":\"" + templateId + "\",\"channel\":\"MOBILE\",\"payload\":{\"a\":1}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.violations[0].field").value("templateId"))
                .andExpect(jsonPath("$.violations[0].rule").value("Pattern"));
    }

    @Test
    void wrongContentTypeGives415() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void wrongMethodGives405() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void invalidRequestIdHeaderGoesThroughGlobalHandler() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .header(HeaderConstants.X_REQUEST_ID, "bad id;")
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.requestId").exists());
    }
}
