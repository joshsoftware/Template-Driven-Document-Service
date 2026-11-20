package com.joshsoftware.templateDrivenDocumentService.enums;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.MappingException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    // Broken template config is our bug, not the caller's: 500 MAPPING_ERROR
    @Test
    void brokenTemplateConfigIsReportedAsMappingError() {
        assertThat(ErrorCode.from(new InvalidDefinitionException("contract field 'ifsc': unknown type")))
                .isEqualTo(ErrorCode.MAPPING_ERROR);
        assertThat(ErrorCode.from(new MappingException("bad path"))).isEqualTo(ErrorCode.MAPPING_ERROR);
        assertThat(ErrorCode.MAPPING_ERROR.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
