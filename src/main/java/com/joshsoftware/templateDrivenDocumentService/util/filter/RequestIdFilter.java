package com.joshsoftware.templateDrivenDocumentService.util.filter;

import com.joshsoftware.templateDrivenDocumentService.constants.HeaderConstants;
import com.joshsoftware.templateDrivenDocumentService.constants.MdcConstants;
import com.joshsoftware.templateDrivenDocumentService.exception.InvalidRequestException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

// Runs first on every request. Puts the request id in MDC so every log line carries it,
// and returns it in the X-Request-Id response header.
// Uses the caller's X-Request-Id if valid, else creates a UUID.
// An invalid X-Request-Id gets a 400 INVALID_REQUEST. The raw value is never logged or put in MDC.
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final HandlerExceptionResolver exceptionResolver;

    // Exceptions thrown in a filter do not reach @RestControllerAdvice, so they are passed to
    // this resolver, which calls GlobalExceptionHandler.
    public RequestIdFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HeaderConstants.X_REQUEST_ID);
        boolean valid = header == null || VALID_REQUEST_ID.matcher(header).matches();
        // A bad header still gets a new id, so the error response and logs can be traced.
        String requestId = header != null && valid ? header : UUID.randomUUID().toString();

        MDC.put(MdcConstants.REQUEST_ID, requestId);
        // Set before the chain runs so error responses carry it too.
        response.setHeader(HeaderConstants.X_REQUEST_ID, requestId);
        try {
            if (!valid) {
                log.warn("[RequestIdFilter#doFilterInternal] invalid {} header, length={}",
                        HeaderConstants.X_REQUEST_ID, header.length());
                exceptionResolver.resolveException(request, response, null,
                        new InvalidRequestException("Invalid " + HeaderConstants.X_REQUEST_ID + " header"));
                return;
            }
            chain.doFilter(request, response);
        } finally {
            // Also removes jobId, so nothing leaks to the next request on this thread.
            MDC.clear();
        }
    }
}
