package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_CIVILSTAND_CHANGED;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_EXISTING_CASE;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_NO_EXISTING_CASE;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String APPLICANT = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String CO_APPLICANT = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
	private static final String APPLICANT_PNR = "198001012389";
	private static final String CO_APPLICANT_PNR = "198202022397";
	private static final String ERRAND_ID = "errand-1";

	private static final YearMonth CURRENT = YearMonth.now();
	private static final YearMonth NEXT = CURRENT.plusMonths(1);

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private FinancialAssistanceRepository financialAssistanceRepositoryMock;

	@Mock
	private LifecareEbCaseService lifecareEbCaseServiceMock;

	@Mock
	private CitizenService citizenServiceMock;

	private EligibilityService service() {
		return new EligibilityService(errandRepositoryMock, financialAssistanceRepositoryMock, lifecareEbCaseServiceMock, citizenServiceMock, 90, false);
	}

	@BeforeEach
	void citizenResolvesPartyIds() {
		lenient().when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT)).thenReturn(Optional.of(APPLICANT_PNR));
		lenient().when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, CO_APPLICANT)).thenReturn(Optional.of(CO_APPLICANT_PNR));
	}

	private static EligibilityRequest alone() {
		return EligibilityRequest.create().withApplicant(APPLICANT);
	}

	private static EligibilityRequest together() {
		return EligibilityRequest.create().withApplicant(APPLICANT).withCoApplicant(CO_APPLICANT);
	}

	private static ApplicationSuggestion recommended(final List<ApplicationSuggestion> suggestions) {
		return suggestions.stream().filter(ApplicationSuggestion::isRecommended).findFirst().orElseThrow();
	}

	/** Wire a CM EB errand owned by the given persons, with an optional period, created now. */
	private void cmErrand(final FaPerson... persons) {
		cmErrandWithPeriod(null, null, persons);
	}

	private void cmErrandWithPeriod(final YearMonth period, final OffsetDateTime created, final FaPerson... persons) {
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of(ERRAND_ID));
		lenient().when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(CO_APPLICANT)).thenReturn(List.of(ERRAND_ID));
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(ErrandEntity.create().withId(ERRAND_ID).withTypeSlug(SLUG_RENEWAL)
				.withCreated(created != null ? created : OffsetDateTime.now())));
		final var fa = FinancialAssistanceEntity.create().withErrandId(ERRAND_ID).withPersons(List.of(persons));
		Optional.ofNullable(period).ifPresent(p -> fa.withPeriodMonth(p.getMonthValue()).withPeriodYear(p.getYear()));
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(fa));
	}

	private static FaPerson person(final String role, final String partyId) {
		return FaPerson.create().withRole(role).withPartyId(partyId);
	}

	private void noCmErrands() {
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of());
		lenient().when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(CO_APPLICANT)).thenReturn(List.of());
	}

	// ---- 1) Existence gate -------------------------------------------------------------------------------------------

	@Test
	void noExistenceAnywhereSuggestsNew() {
		noCmErrands();
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareEbCaseSummary.none());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_EXISTING_CASE);
		assertThat(response.isExistsInCm()).isFalse();
		assertThat(response.isExistsInLc()).isFalse();
		assertThat(response.getSuggestions()).singleElement()
			.satisfies(s -> assertThat(s.getTypeSlug()).isEqualTo(SLUG_NEW))
			.satisfies(s -> assertThat(s.getPeriodMonth()).isNull())
			.satisfies(s -> assertThat(s.isRecommended()).isTrue());
	}

	@Test
	void existsInLifecareOnlyPassesExistence() {
		noCmErrands();
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareEbCaseSummary(true, Set.of(), null, false, false));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.isExistsInLc()).isTrue();
		assertThat(response.getReasonCode()).isEqualTo(REASON_EXISTING_CASE);
	}

	@Test
	void coApplicantMissingEverywhereSuggestsNew() {
		// Applicant exists in CM, co-applicant exists nowhere → not "för båda" → NY.
		cmErrand(person(ROLE_APPLICANT, APPLICANT), person(ROLE_CO_APPLICANT, CO_APPLICANT));
		// Re-stub co lookup to empty so the co-applicant is absent from CM, and absent from LC.
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(CO_APPLICANT)).thenReturn(List.of());
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareEbCaseSummary(true, Set.of(), null, false, true));
		when(lifecareEbCaseServiceMock.summarize(eq(CO_APPLICANT_PNR), any())).thenReturn(LifecareEbCaseSummary.none());

		// errand only lists the applicant so co-applicant isn't found in CM either
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)
				.withPersons(List.of(person(ROLE_APPLICANT, APPLICANT)))));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, together());

		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_EXISTING_CASE);
	}

	// ---- 2) Civilstånd gate ------------------------------------------------------------------------------------------

	@Test
	void civilstandChangedSuggestsNew() {
		// Previous CM application was solo; now applying together → civilstånd changed → NY.
		cmErrand(person(ROLE_APPLICANT, APPLICANT));
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareEbCaseSummary.none());
		when(lifecareEbCaseServiceMock.summarize(eq(CO_APPLICANT_PNR), any()))
			.thenReturn(new LifecareEbCaseSummary(true, Set.of(), null, false, false)); // co exists in LC

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, together());

		assertThat(response.getReasonCode()).isEqualTo(REASON_CIVILSTAND_CHANGED);
		assertThat(response.getCivilstandMatches()).isFalse();
		assertThat(response.isHasCoApplicant()).isTrue();
		assertThat(response.getSuggestions()).singleElement().satisfies(s -> assertThat(s.getTypeSlug()).isEqualTo(SLUG_NEW));
	}

	@Test
	void sameCivilstandTogetherPasses() {
		// Previous CM application also had a co-applicant → same civilstånd → continue to month logic.
		cmErrand(person(ROLE_APPLICANT, APPLICANT), person(ROLE_CO_APPLICANT, CO_APPLICANT));
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareEbCaseSummary.none());
		when(lifecareEbCaseServiceMock.summarize(eq(CO_APPLICANT_PNR), any())).thenReturn(LifecareEbCaseSummary.none());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, together());

		assertThat(response.getCivilstandMatches()).isTrue();
		assertThat(response.getReasonCode()).isEqualTo(REASON_EXISTING_CASE);
	}

	// ---- 3) Per-month logic ------------------------------------------------------------------------------------------

	@Test
	void existingCaseNoDecisionThisMonthRecommendsRenewalThisMonth() {
		cmErrand(person(ROLE_APPLICANT, APPLICANT)); // no period → no application for this/next month
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareEbCaseSummary(true, Set.of(), null, false, false));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_EXISTING_CASE);
		assertThat(response.isCurrentMonthDecided()).isFalse();
		assertThat(response.getSuggestions()).hasSize(2);
		final var primary = recommended(response.getSuggestions());
		assertThat(primary.getTypeSlug()).isEqualTo(SLUG_RENEWAL);
		assertThat(primary.getPeriodMonth()).isEqualTo(CURRENT.getMonthValue());
		assertThat(response.getSuggestions().get(1).getPeriodMonth()).isEqualTo(NEXT.getMonthValue());
	}

	@Test
	void decisionForCurrentMonthRecommendsNextMonthAndSupplementThis() {
		cmErrand(person(ROLE_APPLICANT, APPLICANT));
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareEbCaseSummary(true, Set.of(CURRENT), CURRENT, true, false));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_EXISTING_CASE);
		assertThat(response.isCurrentMonthDecided()).isTrue();
		assertThat(response.isApplicationExistsThisMonth()).isTrue();
		assertThat(response.isHasPreviousCalculation()).isTrue();
		final var primary = recommended(response.getSuggestions());
		assertThat(primary.getTypeSlug()).isEqualTo(SLUG_RENEWAL);
		assertThat(primary.getPeriodMonth()).isEqualTo(NEXT.getMonthValue());
		// the non-recommended option is a tilläggsansökan for the current month
		assertThat(response.getSuggestions()).anySatisfy(s -> {
			assertThat(s.getTypeSlug()).isEqualTo(SLUG_SUPPLEMENTARY);
			assertThat(s.getPeriodMonth()).isEqualTo(CURRENT.getMonthValue());
		});
	}

	@Test
	void cmApplicationForThisMonthYieldsSupplement() {
		// A CM application already exists for the current month within the window → tilläggsansökan this month.
		cmErrandWithPeriod(CURRENT, OffsetDateTime.now(), person(ROLE_APPLICANT, APPLICANT));
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareEbCaseSummary(true, Set.of(), null, false, false));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.isApplicationExistsThisMonth()).isTrue();
		final var primary = recommended(response.getSuggestions());
		assertThat(primary.getTypeSlug()).isEqualTo(SLUG_SUPPLEMENTARY);
		assertThat(primary.getPeriodMonth()).isEqualTo(CURRENT.getMonthValue());
	}

	@Test
	void cmApplicationOutsideWindowDoesNotCount() {
		// Same-month CM application but created long ago → outside 90-day window → still återansökan.
		cmErrandWithPeriod(CURRENT, OffsetDateTime.now().minusDays(200), person(ROLE_APPLICANT, APPLICANT));
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareEbCaseSummary(true, Set.of(), null, false, false));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.isApplicationExistsThisMonth()).isFalse();
		assertThat(recommended(response.getSuggestions()).getTypeSlug()).isEqualTo(SLUG_RENEWAL);
	}

	// ---- Degradation -------------------------------------------------------------------------------------------------

	@Test
	void lifecareUnavailableButExistsInCmStillRoutes() {
		cmErrand(person(ROLE_APPLICANT, APPLICANT));
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "FC down"));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.isLifecareChecked()).isFalse();
		assertThat(response.isExistsInCm()).isTrue();
		assertThat(response.getReasonCode()).isEqualTo(REASON_EXISTING_CASE);
		assertThat(recommended(response.getSuggestions()).getTypeSlug()).isEqualTo(SLUG_RENEWAL);
	}

	@Test
	void configuredWindowIsReflected() {
		noCmErrands();
		when(lifecareEbCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareEbCaseSummary.none());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getWindowDays()).isEqualTo(90); // from the service config, not the request
	}
}
