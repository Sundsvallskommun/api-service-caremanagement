package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseService;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseSummary;
import se.sundsvall.caremanagement.types.financialassistance.api.model.ApplicationSuggestion;
import se.sundsvall.caremanagement.types.financialassistance.api.model.EligibilityRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_CONSTELLATION_MISMATCH;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_DECISION_FOR_CURRENT_MONTH;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_NO_DECISION_FOR_CURRENT_MONTH;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_NO_OPEN_CASE;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_RECENT_APPLICATION;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String APPLICANT = "198001012389";
	private static final String CO_APPLICANT = "198202022397";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private FinancialAssistanceRepository financialAssistanceRepositoryMock;

	@Mock
	private LifecareEbCaseService lifecareEbCaseServiceMock;

	private EligibilityService service() {
		return new EligibilityService(errandRepositoryMock, financialAssistanceRepositoryMock, lifecareEbCaseServiceMock, 90);
	}

	private static EligibilityRequest alone() {
		return EligibilityRequest.create().withApplicant(APPLICANT);
	}

	private static ApplicationSuggestion recommended(final List<ApplicationSuggestion> suggestions) {
		return suggestions.stream().filter(ApplicationSuggestion::isRecommended).findFirst().orElseThrow();
	}

	private void noRecentApplications() {
		when(financialAssistanceRepositoryMock.findRecentErrandIdsByPerson(eq(APPLICANT), any(OffsetDateTime.class))).thenReturn(List.of());
	}

	// ---- Duplicate guard (our DB) ------------------------------------------------------------------------------------

	@Test
	void recentApplicationAloneSuggestsSupplementary() {
		when(financialAssistanceRepositoryMock.findRecentErrandIdsByPerson(eq(APPLICANT), any(OffsetDateTime.class))).thenReturn(List.of(ERRAND_ID));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID).withTypeSlug(SLUG_NEW).withCreated(OffsetDateTime.now())));
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)
				.withPersons(List.of(FaPerson.create().withRole(ROLE_APPLICANT).withPersonalNumber(APPLICANT)))));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_RECENT_APPLICATION);
		assertThat(response.isHasRecentApplication()).isTrue();
		assertThat(response.isRequiresCaseworker()).isFalse();
		assertThat(response.getConstellationMatchesPrevious()).isTrue();
		assertThat(response.getWindowDays()).isEqualTo(90);
		assertThat(response.getSuggestions()).singleElement()
			.satisfies(s -> assertThat(s.getTypeSlug()).isEqualTo(SLUG_SUPPLEMENTARY))
			.satisfies(s -> assertThat(s.isRecommended()).isTrue());
		verifyNoInteractions(lifecareEbCaseServiceMock);
	}

	@Test
	void recentApplicationWithDifferentConstellationRequiresCaseworker() {
		final var request = EligibilityRequest.create().withApplicant(APPLICANT).withCoApplicant(CO_APPLICANT);
		when(financialAssistanceRepositoryMock.findRecentErrandIdsByPerson(eq(APPLICANT), any(OffsetDateTime.class))).thenReturn(List.of(ERRAND_ID));
		when(financialAssistanceRepositoryMock.findRecentErrandIdsByPerson(eq(CO_APPLICANT), any(OffsetDateTime.class))).thenReturn(List.of());
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID).withTypeSlug(SLUG_RENEWAL).withCreated(OffsetDateTime.now())));
		// Previous application was for a single applicant — now applying together => mismatch.
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)
				.withPersons(List.of(FaPerson.create().withRole(ROLE_APPLICANT).withPersonalNumber(APPLICANT)))));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getReasonCode()).isEqualTo(REASON_CONSTELLATION_MISMATCH);
		assertThat(response.isRequiresCaseworker()).isTrue();
		assertThat(response.getConstellationMatchesPrevious()).isFalse();
		assertThat(response.isHasCoApplicant()).isTrue();
	}

	@Test
	void recentErrandOfOtherTypeIsIgnored() {
		// The person appears in a non-EB errand only — it must not count as a recent EB application.
		when(financialAssistanceRepositoryMock.findRecentErrandIdsByPerson(eq(APPLICANT), any(OffsetDateTime.class))).thenReturn(List.of(ERRAND_ID));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID).withTypeSlug("some-other-type").withCreated(OffsetDateTime.now())));
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT), any())).thenReturn(LifecareEbCaseSummary.none());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.isHasRecentApplication()).isFalse();
		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_OPEN_CASE);
	}

	// ---- Lifecare-driven routing -------------------------------------------------------------------------------------

	@Test
	void noOpenCaseSuggestsNewApplication() {
		final var request = alone().withWithinDays(30);
		when(financialAssistanceRepositoryMock.findRecentErrandIdsByPerson(eq(APPLICANT), any(OffsetDateTime.class))).thenReturn(List.of());
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT), any())).thenReturn(LifecareEbCaseSummary.none());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_OPEN_CASE);
		assertThat(response.isLifecareChecked()).isTrue();
		assertThat(response.isHasOpenCase()).isFalse();
		assertThat(response.getWindowDays()).isEqualTo(30);
		assertThat(response.getSuggestions()).singleElement()
			.satisfies(s -> assertThat(s.getTypeSlug()).isEqualTo(SLUG_NEW))
			.satisfies(s -> assertThat(s.getPeriodMonth()).isNull());
	}

	@Test
	void openCaseWithDecisionForCurrentMonthSuggestsRenewalNextOrSupplementary() {
		final var current = YearMonth.now();
		noRecentApplications();
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT), any()))
			.thenReturn(new LifecareEbCaseSummary(true, true, current, true, Set.of()));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_DECISION_FOR_CURRENT_MONTH);
		assertThat(response.isHasOpenCase()).isTrue();
		assertThat(response.isHasDecisionForCurrentMonth()).isTrue();
		assertThat(response.isHasPreviousCalculation()).isTrue();
		assertThat(response.getLatestDecisionPeriodMonth()).isEqualTo(current.getMonthValue());
		assertThat(response.getLatestDecisionPeriodYear()).isEqualTo(current.getYear());
		assertThat(response.getSuggestions()).hasSize(2);
		final var primary = recommended(response.getSuggestions());
		assertThat(primary.getTypeSlug()).isEqualTo(SLUG_RENEWAL);
		assertThat(primary.getPeriodMonth()).isEqualTo(current.plusMonths(1).getMonthValue());
		assertThat(primary.getPeriodYear()).isEqualTo(current.plusMonths(1).getYear());
		assertThat(response.getSuggestions().get(1).getTypeSlug()).isEqualTo(SLUG_SUPPLEMENTARY);
	}

	@Test
	void openCaseWithoutDecisionForCurrentMonthSuggestsRenewalThisOrNextOrSupplementary() {
		final var current = YearMonth.now();
		noRecentApplications();
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT), any()))
			.thenReturn(new LifecareEbCaseSummary(true, false, current.minusMonths(2), false, Set.of()));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_DECISION_FOR_CURRENT_MONTH);
		assertThat(response.isRequiresCaseworker()).isFalse();
		assertThat(response.getSuggestions()).hasSize(3);
		final var primary = recommended(response.getSuggestions());
		assertThat(primary.getTypeSlug()).isEqualTo(SLUG_RENEWAL);
		assertThat(primary.getPeriodMonth()).isEqualTo(current.getMonthValue());
	}

	@Test
	void openCaseWithoutAnyDecisionLeavesConstellationUnknown() {
		// Open case via an aktualisering only — no decision to compare the constellation against.
		noRecentApplications();
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT), any()))
			.thenReturn(new LifecareEbCaseSummary(true, false, null, false, Set.of()));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_DECISION_FOR_CURRENT_MONTH);
		assertThat(response.isRequiresCaseworker()).isFalse();
		assertThat(response.getConstellationMatchesPrevious()).isNull();
		assertThat(response.getLatestDecisionPeriodMonth()).isNull();
	}

	@Test
	void openCaseWithConstellationMismatchRequiresCaseworker() {
		final var request = EligibilityRequest.create().withApplicant(APPLICANT).withCoApplicant(CO_APPLICANT);
		final var current = YearMonth.now();
		when(financialAssistanceRepositoryMock.findRecentErrandIdsByPerson(eq(APPLICANT), any(OffsetDateTime.class))).thenReturn(List.of());
		when(financialAssistanceRepositoryMock.findRecentErrandIdsByPerson(eq(CO_APPLICANT), any(OffsetDateTime.class))).thenReturn(List.of());
		// Latest decision lists a different co-applicant than the one now applying.
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT), any()))
			.thenReturn(new LifecareEbCaseSummary(true, false, current, false, Set.of("197001010000")));
		when(lifecareEbCaseServiceMock.summarize(eq(CO_APPLICANT), any()))
			.thenReturn(new LifecareEbCaseSummary(true, false, null, false, Set.of()));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getReasonCode()).isEqualTo(REASON_CONSTELLATION_MISMATCH);
		assertThat(response.isRequiresCaseworker()).isTrue();
		assertThat(response.getConstellationMatchesPrevious()).isFalse();
	}

	@Test
	void coApplicantWithoutOpenCaseForBothSuggestsNewApplication() {
		final var request = EligibilityRequest.create().withApplicant(APPLICANT).withCoApplicant(CO_APPLICANT);
		when(financialAssistanceRepositoryMock.findRecentErrandIdsByPerson(eq(APPLICANT), any(OffsetDateTime.class))).thenReturn(List.of());
		when(financialAssistanceRepositoryMock.findRecentErrandIdsByPerson(eq(CO_APPLICANT), any(OffsetDateTime.class))).thenReturn(List.of());
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT), any()))
			.thenReturn(new LifecareEbCaseSummary(true, true, YearMonth.now(), true, Set.of()));
		when(lifecareEbCaseServiceMock.summarize(eq(CO_APPLICANT), any())).thenReturn(LifecareEbCaseSummary.none());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.isHasOpenCase()).isFalse();
		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_OPEN_CASE);
		assertThat(response.getSuggestions()).singleElement().satisfies(s -> assertThat(s.getTypeSlug()).isEqualTo(SLUG_NEW));
	}

	@Test
	void lifecareUnavailableDegradesToNewApplication() {
		noRecentApplications();
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT), any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "FC down"));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.isLifecareChecked()).isFalse();
		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_OPEN_CASE);
		assertThat(response.getMessage()).contains("Lifecare kunde inte nås");
		assertThat(response.getSuggestions()).singleElement().satisfies(s -> assertThat(s.getTypeSlug()).isEqualTo(SLUG_NEW));
		verify(errandRepositoryMock, never()).findByIdAndNamespaceAndMunicipalityId(any(), any(), any());
	}
}
