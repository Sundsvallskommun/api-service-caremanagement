package se.sundsvall.caremanagement.lifecare.integration;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.lifecare.integration.FamilyCareDates.startOfDay;

class FamilyCareDatesTest {

	/**
	 * FamilyCare rejects a bare {@code yyyy-MM-dd} — and {@code ISO_LOCAL_DATE_TIME} would render midnight as
	 * {@code 2026-06-01T00:00}, dropping the seconds field FamilyCare requires.
	 */
	@Test
	void startOfDayIsRenderedWithSeconds() {
		assertThat(startOfDay(LocalDate.of(2026, JUNE, 1))).isEqualTo("2026-06-01T00:00:00");
	}

	@Test
	void nullStaysNullSoAnOptionalParameterIsSimplyOmitted() {
		assertThat(startOfDay(null)).isNull();
	}
}
