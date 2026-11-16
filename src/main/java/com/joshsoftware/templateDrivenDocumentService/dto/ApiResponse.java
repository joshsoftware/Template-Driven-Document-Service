package com.joshsoftware.templateDrivenDocumentService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// Response body for every successful API call.
@Getter
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private String message;

    private Integer statusCode;

    private T data;

    // Builds a 200 OK response.
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .message(message)
                .statusCode(HttpStatus.OK.value())
                .data(data)
                .build();
    }
}
