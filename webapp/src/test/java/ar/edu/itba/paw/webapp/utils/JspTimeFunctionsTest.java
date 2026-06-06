package ar.edu.itba.paw.webapp.utils;

import java.time.OffsetDateTime;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

class JspTimeFunctionsTest {

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void dateFormatsUsingTheCarriedOffsetWithoutShiftingTheWallClock() {
        // 1. Arrange
        final OffsetDateTime value = OffsetDateTime.parse("2026-05-25T23:30:00-03:00");

        // 2. Exercise
        final String result = JspTimeFunctions.date(value);

        // 3. Assert
        Assertions.assertEquals("May 25, 2026", result);
    }

    @Test
    void dateTimeIncludesTheLocalTimeOfTheCarriedOffset() {
        // 1. Arrange
        final OffsetDateTime value = OffsetDateTime.parse("2026-05-25T23:30:00-03:00");

        // 2. Exercise
        final String result = JspTimeFunctions.dateTime(value);

        // 3. Assert
        Assertions.assertTrue(
                result.contains("2026") && result.contains("11:30"),
                "expected formatted date-time to contain the date and local time, was: " + result);
    }

    @Test
    void dateReturnsEmptyStringForNullValue() {
        // 1. Arrange

        // 2. Exercise
        final String result = JspTimeFunctions.date(null);

        // 3. Assert
        Assertions.assertEquals("", result);
    }
}
