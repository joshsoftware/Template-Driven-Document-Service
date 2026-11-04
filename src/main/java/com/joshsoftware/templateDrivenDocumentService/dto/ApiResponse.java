package com.joshsoftware.templateDrivenDocumentService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Standard success envelope returned by every controller:
 * {@code { message, statusCode, data }}.
 * Errors use {@link ErrorResponse}, which shares {@code message} and {@code statusCode}.
 **/
@Getter
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private String message;

    private Integer statusCode;

    private T data;

    /** {@code 200 OK} with a body. */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .message(message)
                .statusCode(HttpStatus.OK.value())
                .data(data)
                .build();
    }
}
