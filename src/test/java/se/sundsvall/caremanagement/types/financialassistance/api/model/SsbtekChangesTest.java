package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SsbtekChangesTest {

	@Test
	void testAccessors() {
		final var compared = OffsetDateTime.parse("2026-09-25T09:30:00+02:00");
		final var read = OffsetDateTime.parse("2026-09-25T03:00:00+02:00");
		final var change = new SsbtekChange("CHANGE", "CONFIRM", "EDITED", "APPLICANT", 20, "Lön", new BigDecimal("12400"), new BigDecimal("12000"));

		final var changes = new SsbtekChanges(4242, true, compared, read, List.of(change));

		assertThat(changes.calculationId()).isEqualTo(4242);
		assertThat(changes.isFinal()).isTrue();
		assertThat(changes.comparedAt()).isEqualTo(compared);
		assertThat(changes.ssbtekReadAt()).isEqualTo(read);
		assertThat(changes.changes()).containsExactly(change);
		assertThat(change.kind()).isEqualTo("CHANGE");
		assertThat(change.mode()).isEqualTo("CONFIRM");
		assertThat(change.reason()).isEqualTo("EDITED");
		assertThat(change.role()).isEqualTo("APPLICANT");
		assertThat(change.incomeTypeId()).isEqualTo(20);
		assertThat(change.incomeType()).isEqualTo("Lön");
		assertThat(change.ssbtekAmount()).isEqualByComparingTo("12400");
		assertThat(change.lifecareAmount()).isEqualByComparingTo("12000");
	}

	@Test
	void testAppliedAccessors() {
		final var change = new AppliedSsbtekChange("CO_APPLICANT", "Barnbidrag", null);
		final var applied = new AppliedSsbtekChanges(4242, List.of(change));

		assertThat(applied.calculationId()).isEqualTo(4242);
		assertThat(applied.applied()).containsExactly(change);
		assertThat(change.role()).isEqualTo("CO_APPLICANT");
		assertThat(change.incomeType()).isEqualTo("Barnbidrag");
		assertThat(change.amount()).isNull();
	}
}
