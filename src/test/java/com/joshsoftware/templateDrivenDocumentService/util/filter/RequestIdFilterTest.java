package com.joshsoftware.templateDrivenDocumentService.util.filter;

import com.joshsoftware.templateDrivenDocumentService.constants.HeaderConstants;
import com.joshsoftware.templateDrivenDocumentService.constants.MdcConstants;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import com.joshsoftware.templateDrivenDocumentService.exception.InvalidRequestException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RequestIdFilterTest {

    private final HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
    private final RequestIdFilter filter = new RequestIdFilter(resolver);
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/documents/generate");
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final AtomicReference<String> mdcInChain = new AtomicReference<>();
    private final FilterChain chain = (req, res) -> mdcInChain.set(MDC.get(MdcConstants.REQUEST_ID));

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesCallerRequestId() throws Exception {
        request.addHeader(HeaderConstants.X_REQUEST_ID, "abc-123_x.y");

        filter.doFilter(request, response, chain);

        assertThat(mdcInChain.get()).isEqualTo("abc-123_x.y");
        assertThat(response.getHeader(HeaderConstants.X_REQUEST_ID)).isEqualTo("abc-123_x.y");
        verifyNoInteractions(resolver);
    }

    @Test
    void createsUuidWhenHeaderMissing() throws Exception {
        filter.doFilter(request, response, chain);

        String id = response.getHeader(HeaderConstants.X_REQUEST_ID);
        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(mdcInChain.get()).isEqualTo(id);
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        FilterChain chainSettingJobId = (req, res) -> MDC.put(MdcConstants.JOB_ID, UUID.randomUUID().toString());

        filter.doFilter(request, response, chainSettingJobId);

        assertThat(MDC.get(MdcConstants.REQUEST_ID)).isNull();
        assertThat(MDC.get(MdcConstants.JOB_ID)).isNull();
    }

    @Test
    void clearsMdcWhenChainThrows() {
        FilterChain failing = (req, res) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilter(request, response, failing);
        } catch (Exception ignored) {
            // expected
        }

        assertThat(MDC.get(MdcConstants.REQUEST_ID)).isNull();
    }

    @Test
    void rejectsTooLongHeader() throws Exception {
        assertRejected("a".repeat(65));
    }

    @Test
    void rejectsHeaderWithBadCharacters() throws Exception {
        assertRejected("1234 5678;drop");
    }

    @Test
    void rejectsEmptyHeader() throws Exception {
        assertRejected("");
    }

    private void assertRejected(String badId) throws Exception {
        request.addHeader(HeaderConstants.X_REQUEST_ID, badId);

        filter.doFilter(request, response, chain);

        assertThat(mdcInChain.get()).as("chain must not run").isNull();
        String id = response.getHeader(HeaderConstants.X_REQUEST_ID);
        assertThat(id).isNotEqualTo(badId);
        assertThat(UUID.fromString(id)).isNotNull();
        verify(resolver).resolveException(eq(request), eq(response), isNull(),
                argThat(ex -> ex instanceof InvalidRequestException ire
                        && ire.getErrorCode() == ErrorCode.INVALID_REQUEST));
        assertThat(MDC.get(MdcConstants.REQUEST_ID)).isNull();
    }
}
