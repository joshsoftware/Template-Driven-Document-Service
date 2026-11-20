package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateFormatsTest {

    // The statement mapping's dateFormats
    private static final DateFormats STATEMENT = DateFormats.of("yyyyMMdd", "yyyy-MM-dd'T'HH:mm:ss");

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "20240410, 2024-04-10",
            "20240229, 2024-02-29",
            "20240231, REJECTED",
            "20230229, REJECTED",
            "2024-04-10, REJECTED"})
    void readsBankDatesStrictly(String bankValue, String expected) {
        if ("REJECTED".equals(expected)) {
            assertThatThrownBy(() -> LocalDate.parse(bankValue, STATEMENT.date()))
                    .isInstanceOf(DateTimeParseException.class);
        } else {
            assertThat(LocalDate.parse(bankValue, STATEMENT.date())).isEqualTo(LocalDate.parse(expected));
        }
    }

    @Test
    void readsBankDateTimes() {
        assertThat(LocalDateTime.parse("2024-06-01T10:00:00", STATEMENT.datetime()))
                .isEqualTo(LocalDateTime.of(2024, 6, 1, 10, 0));
        assertThatThrownBy(() -> LocalDateTime.parse("2024-06-01 10:00:00", STATEMENT.datetime()))
                .isInstanceOf(DateTimeParseException.class);
    }

    // Config authors may write either; strict mode must work for both
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"yyyyMMdd", "uuuuMMdd"})
    void treatsYyyyAndUuuuAlike(String pattern) {
        DateFormats formats = DateFormats.of(pattern, null);

        assertThat(LocalDate.parse("20240410", formats.date())).isEqualTo(LocalDate.of(2024, 4, 10));
        assertThatThrownBy(() -> LocalDate.parse("20240231", formats.date()))
                .isInstanceOf(DateTimeParseException.class);
    }

    // A template with no dates, or only dates, needs only what it uses
    @Test
    void supportsTemplatesThatUseOnlySomeFormats() {
        DateFormats dateOnly = DateFormats.of("dd-MM-yyyy", null);

        assertThat(LocalDate.parse("10-04-2024", dateOnly.date())).isEqualTo(LocalDate.of(2024, 4, 10));
        assertThat(dateOnly.datetime()).isNull();
        assertThat(DateFormats.NONE.date()).isNull();
        assertThat(DateFormats.NONE.datetime()).isNull();
    }

    @Test
    void reportsBrokenPatternsAsConfigErrors() {
        assertThatThrownBy(() -> DateFormats.of(" ", null))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage("mapping: dateFormats.date is blank");
        assertThatThrownBy(() -> DateFormats.of("yyyyMMdd", "yyyy{"))
                .isInstanceOf(InvalidDefinitionException.class)
                .hasMessage("mapping: dateFormats.datetime is not a valid pattern");
    }
}
