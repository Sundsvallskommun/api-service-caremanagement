package se.sundsvall.caremanagement.lifecare.integration.integrator;

import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;

class IntegratorWriteMapperTest {

	private static final String APPLICANT_PARTY_ID = "6a5c3d18-1f2b-4e77-9c0a-2b3d4e5f6a7b";

	/** FamilyCare renders {@code yyyy-MM-dd'T'HH:mm:ss}; a bare date has to keep working too. */
	@ParameterizedTest
	@ValueSource(strings = {
		"2026-06-01T00:00:00", "2026-06-01", "2026-06-01T23:59:59", "2026-06-01T00:00:00+02:00"
	})
	void bothRenderedAndBareDatesParse(final String rendered) {
		final var body = new PostAktualiseringsBodyRequest().date(rendered);

		assertThat(IntegratorWriteMapper.toActualisation(body, APPLICANT_PARTY_ID).getDate())
			.isEqualTo(LocalDate.of(2026, JUNE, 1));
	}

	/**
	 * A date that will not parse becomes null rather than an exception, so the caller's required-field check reports
	 * it by name instead of the failure surfacing as a stack trace from inside the mapper.
	 */
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
		"not-a-date", "2026-13-45T00:00:00", "26-06-01"
	})
	void anUnparseableDateBecomesNull(final String rendered) {
		final var body = new PostAktualiseringsBodyRequest().date(rendered);

		assertThat(IntegratorWriteMapper.toActualisation(body, APPLICANT_PARTY_ID).getDate()).isNull();
	}

	@Test
	void theActualisationBodyIsMapped() {
		final var body = new PostAktualiseringsBodyRequest()
			.personId("200001012384")
			.date("2026-06-12T00:00:00")
			.type(1)
			.fromWho(20)
			.reason(10)
			.organisationId(4)
			.organisationUnitId("IFO-EB")
			.caseworkerId("kaka01")
			.specifies(2)
			.serviceId(42)
			.investigationId(11)
			.workingStatus(3);

		final var request = IntegratorWriteMapper.toActualisation(body, APPLICANT_PARTY_ID);

		assertThat(request.getPartyId()).isEqualTo(APPLICANT_PARTY_ID);
		assertThat(request.getDate()).isEqualTo(LocalDate.of(2026, JUNE, 12));
		assertThat(request.getTypeId()).isEqualTo(1);
		assertThat(request.getFromWhoId()).isEqualTo(20);
		assertThat(request.getReasonId()).isEqualTo(10);
		assertThat(request.getOrganisationId()).isEqualTo(4);
		assertThat(request.getOrganisationUnitId()).isEqualTo("IFO-EB");
		assertThat(request.getCaseworkerId()).isEqualTo("kaka01");
		assertThat(request.getSpecifiesId()).isEqualTo(2);
		assertThat(request.getServiceId()).isEqualTo(42);
		assertThat(request.getInvestigationId()).isEqualTo(11);
		assertThat(request.getWorkingStatusId()).isEqualTo(3);
	}

	/**
	 * FamilyCare's field names lose the {@code Id} suffix the integrator uses ({@code type} vs {@code typeId},
	 * {@code specifies} vs {@code specifiesId}), so the pairing is worth pinning: a mismatched pair would post a valid
	 * actualisation with the wrong reason or working status on it.
	 */
	@Test
	void theActualisationIdFieldsAreNotTransposed() {
		final var request = IntegratorWriteMapper.toActualisation(
			new PostAktualiseringsBodyRequest().type(1).fromWho(2).reason(3).specifies(4).workingStatus(5), APPLICANT_PARTY_ID);

		assertThat(request.getTypeId()).isEqualTo(1);
		assertThat(request.getFromWhoId()).isEqualTo(2);
		assertThat(request.getReasonId()).isEqualTo(3);
		assertThat(request.getSpecifiesId()).isEqualTo(4);
		assertThat(request.getWorkingStatusId()).isEqualTo(5);
	}
}
