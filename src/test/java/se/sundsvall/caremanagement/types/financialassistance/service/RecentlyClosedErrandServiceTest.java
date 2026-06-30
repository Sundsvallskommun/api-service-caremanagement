package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_CLOSED;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_UNDER_REVIEW;

@ExtendWith(MockitoExtension.class)
class RecentlyClosedErrandServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String APPLICANT = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String ERRAND_ID = "errand-1";
	private static final int WINDOW_DAYS = 30;

	@Mock
	private FinancialAssistanceRepository financialAssistanceRepositoryMock;

	@Mock
	private ErrandRepository errandRepositoryMock;

	private RecentlyClosedErrandService service() {
		return new RecentlyClosedErrandService(financialAssistanceRepositoryMock, errandRepositoryMock, WINDOW_DAYS);
	}

	private void errand(final String errandId, final String typeSlug, final String status, final OffsetDateTime touched) {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(errandId, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(errandId).withTypeSlug(typeSlug).withStatus(status).withTouched(touched)));
	}

	@Test
	void findsErrandClosedWithinWindow() {
		final var closedAt = OffsetDateTime.now().minusDays(5);
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of(ERRAND_ID));
		errand(ERRAND_ID, SLUG_RENEWAL, STATUS_CLOSED, closedAt);

		final var result = service().findRecentlyClosed(MUNICIPALITY_ID, NAMESPACE, List.of(APPLICANT));

		assertThat(result).isPresent();
		assertThat(result.get().errandId()).isEqualTo(ERRAND_ID);
		assertThat(result.get().closedAt()).isEqualTo(closedAt);
	}

	@Test
	void ignoresErrandClosedOutsideWindow() {
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of(ERRAND_ID));
		errand(ERRAND_ID, SLUG_RENEWAL, STATUS_CLOSED, OffsetDateTime.now().minusDays(WINDOW_DAYS + 10));

		assertThat(service().findRecentlyClosed(MUNICIPALITY_ID, NAMESPACE, List.of(APPLICANT))).isEmpty();
	}

	@Test
	void ignoresErrandThatIsNotClosed() {
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of(ERRAND_ID));
		errand(ERRAND_ID, SLUG_RENEWAL, STATUS_UNDER_REVIEW, OffsetDateTime.now().minusDays(2));

		assertThat(service().findRecentlyClosed(MUNICIPALITY_ID, NAMESPACE, List.of(APPLICANT))).isEmpty();
	}

	@Test
	void ignoresNonEbErrand() {
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of(ERRAND_ID));
		errand(ERRAND_ID, "some-other-type", STATUS_CLOSED, OffsetDateTime.now().minusDays(2));

		assertThat(service().findRecentlyClosed(MUNICIPALITY_ID, NAMESPACE, List.of(APPLICANT))).isEmpty();
	}

	@Test
	void closedErrandWithoutTimestampIsIgnored() {
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of(ERRAND_ID));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID).withTypeSlug(SLUG_RENEWAL).withStatus(STATUS_CLOSED)));

		assertThat(service().findRecentlyClosed(MUNICIPALITY_ID, NAMESPACE, List.of(APPLICANT))).isEmpty();
	}

	@Test
	void fallsBackToCreatedWhenNotTouched() {
		final var created = OffsetDateTime.now().minusDays(4);
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of(ERRAND_ID));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID).withTypeSlug(SLUG_RENEWAL).withStatus(STATUS_CLOSED).withCreated(created)));

		final var result = service().findRecentlyClosed(MUNICIPALITY_ID, NAMESPACE, List.of(APPLICANT));

		assertThat(result).map(RecentlyClosedErrandService.RecentlyClosed::closedAt).contains(created);
	}

	@Test
	void picksMostRecentlyClosedAcrossParties() {
		final var coApplicant = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of("errand-old"));
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(coApplicant)).thenReturn(List.of("errand-new"));
		errand("errand-old", SLUG_RENEWAL, STATUS_CLOSED, OffsetDateTime.now().minusDays(20));
		final var newer = OffsetDateTime.now().minusDays(3);
		errand("errand-new", SLUG_RENEWAL, STATUS_CLOSED, newer);

		final var result = service().findRecentlyClosed(MUNICIPALITY_ID, NAMESPACE, List.of(APPLICANT, coApplicant));

		assertThat(result).isPresent();
		assertThat(result.get().errandId()).isEqualTo("errand-new");
		assertThat(result.get().closedAt()).isEqualTo(newer);
	}

	@Test
	void blankPartiesYieldEmpty() {
		assertThat(service().findRecentlyClosed(MUNICIPALITY_ID, NAMESPACE, List.of(" "))).isEmpty();
		verifyNoInteractions(errandRepositoryMock);
	}

	@Test
	void deduplicatesSharedErrandIdAcrossParties() {
		final var coApplicant = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
		final var closedAt = OffsetDateTime.now().minusDays(2);
		// Both parties resolve the same shared errand id — it must be looked up once and still match.
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of(ERRAND_ID));
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(coApplicant)).thenReturn(List.of(ERRAND_ID));
		errand(ERRAND_ID, SLUG_RENEWAL, STATUS_CLOSED, closedAt);

		final var result = service().findRecentlyClosed(MUNICIPALITY_ID, NAMESPACE, List.of(APPLICANT, coApplicant));

		assertThat(result).map(RecentlyClosedErrandService.RecentlyClosed::errandId).contains(ERRAND_ID);
	}
}
