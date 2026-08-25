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
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseSummary;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_ALL_TYPES_TEST;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_EXISTING_CASE;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_MARITAL_STATUS_CHANGED;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_NO_EXISTING_CASE;
import static se.sundsvall.caremanagement.types.financialassistance.service.EligibilityService.REASON_RECENTLY_CLOSED;

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
	private ErrandQueryService errandQueryServiceMock;

	@Mock
	private FinancialAssistanceRepository financialAssistanceRepositoryMock;

	@Mock
	private LifecareCaseService lifecareCaseServiceMock;

	@Mock
	private CitizenService citizenServiceMock;

	@Mock
	private RecentlyClosedErrandService recentlyClosedErrandServiceMock;

	private EligibilityService service() {
		return new EligibilityService(errandQueryServiceMock, financialAssistanceRepositoryMock, lifecareCaseServiceMock,
			citizenServiceMock, recentlyClosedErrandServiceMock, 90, false);
	}

	/** The service with the return-all-types test override on — off in every other test, as in production. */
	private EligibilityService serviceReturningAllTypes() {
		return new EligibilityService(errandQueryServiceMock, financialAssistanceRepositoryMock, lifecareCaseServiceMock,
			citizenServiceMock, recentlyClosedErrandServiceMock, 90, true);
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

	/** Wire a CM financial assistance errand owned by the given persons, with an optional period, created now. */
	private void cmErrand(final FaPerson... persons) {
		cmErrandWithPeriod(null, null, persons);
	}

	private void cmErrandWithPeriod(final YearMonth period, final OffsetDateTime created, final FaPerson... persons) {
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of(ERRAND_ID));
		lenient().when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(CO_APPLICANT)).thenReturn(List.of(ERRAND_ID));
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_RENEWAL)
				.withCreated(Optional.ofNullable(created).orElseGet(OffsetDateTime::now))));
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

	// ---- 0) Protected identity gate -----------------------------------------------------------------------------------

	@Test
	void protectedApplicantViaCitizenYieldsEmptySuggestions() {
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(true);

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		// No application offered, and the protected status must not leak — no reasonCode, no message.
		assertThat(response.getSuggestions()).isEmpty();
		assertThat(response.getReasonCode()).isNull();
		assertThat(response.getMessage()).isNull();
	}

	@Test
	void protectedApplicantViaLifecareYieldsEmptySuggestions() {
		// Citizen says not protected, but Lifecare flags the person → still no suggestions.
		when(lifecareCaseServiceMock.hasProtectedIdentity(APPLICANT_PNR)).thenReturn(true);

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getSuggestions()).isEmpty();
		assertThat(response.getReasonCode()).isNull();
	}

	@Test
	void protectedCoApplicantYieldsEmptySuggestions() {
		// Applicant is fine, but the co-applicant has protected identity → the joint application is blocked too.
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenReturn(false);
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, CO_APPLICANT)).thenReturn(true);

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, together());

		assertThat(response.getSuggestions()).isEmpty();
		assertThat(response.getReasonCode()).isNull();
	}

	@Test
	void protectedCheckIsBestEffortAndDoesNotBlockOnFailure() {
		// Both protection sources fail → treated as not protected → routing proceeds normally.
		noCmErrands();
		when(citizenServiceMock.hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT)).thenThrow(Problem.valueOf(BAD_GATEWAY, "citizen down"));
		when(lifecareCaseServiceMock.hasProtectedIdentity(APPLICANT_PNR)).thenThrow(Problem.valueOf(BAD_GATEWAY, "FamilyCare down"));
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareCaseSummary.none());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_EXISTING_CASE);
		assertThat(response.getSuggestions()).isNotEmpty();
	}

	// ---- 1) Existence gate -------------------------------------------------------------------------------------------

	@Test
	void noExistenceAnywhereSuggestsNew() {
		noCmErrands();
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareCaseSummary.none());

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
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareCaseSummary(true, Set.of(), null, false, false));

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
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareCaseSummary(true, Set.of(), null, false, true));
		when(lifecareCaseServiceMock.summarize(eq(CO_APPLICANT_PNR), any())).thenReturn(LifecareCaseSummary.none());

		// errand only lists the applicant so co-applicant isn't found in CM either
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)
				.withPersons(List.of(person(ROLE_APPLICANT, APPLICANT)))));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, together());

		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_EXISTING_CASE);
	}

	// ---- 2) Marital status gate
	// ------------------------------------------------------------------------------------------

	@Test
	void civilstandChangedSuggestsNew() {
		// Previous CM application was solo; now applying together → marital status changed → NY.
		cmErrand(person(ROLE_APPLICANT, APPLICANT));
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareCaseSummary.none());
		when(lifecareCaseServiceMock.summarize(eq(CO_APPLICANT_PNR), any()))
			.thenReturn(new LifecareCaseSummary(true, Set.of(), null, false, false)); // co exists in LC

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, together());

		assertThat(response.getReasonCode()).isEqualTo(REASON_MARITAL_STATUS_CHANGED);
		assertThat(response.getMaritalStatusMatches()).isFalse();
		assertThat(response.isHasCoApplicant()).isTrue();
		assertThat(response.getSuggestions()).singleElement().satisfies(s -> assertThat(s.getTypeSlug()).isEqualTo(SLUG_NEW));
	}

	@Test
	void sameCivilstandTogetherPasses() {
		// Previous CM application also had a co-applicant → same marital status → continue to month logic.
		cmErrand(person(ROLE_APPLICANT, APPLICANT), person(ROLE_CO_APPLICANT, CO_APPLICANT));
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareCaseSummary.none());
		when(lifecareCaseServiceMock.summarize(eq(CO_APPLICANT_PNR), any())).thenReturn(LifecareCaseSummary.none());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, together());

		assertThat(response.getMaritalStatusMatches()).isTrue();
		assertThat(response.getReasonCode()).isEqualTo(REASON_EXISTING_CASE);
	}

	@Test
	void differentCoApplicantSuggestsNew() {
		final var otherCo = "b2c3d4e5-f6a7-8901-bcde-f12345678901";
		final var otherCoPnr = "199003033394";
		// Previous CM case: applicant + partner A (CO_APPLICANT). Now applying with a different partner (otherCo) who also
		// exists (Lifecare footprint) → same household size, new person → new constellation → NY.
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(APPLICANT)).thenReturn(List.of(ERRAND_ID));
		when(financialAssistanceRepositoryMock.findErrandIdsByPartyId(otherCo)).thenReturn(List.of());
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withId(ERRAND_ID).withTypeSlug(SLUG_RENEWAL).withCreated(OffsetDateTime.now())));
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(
			FinancialAssistanceEntity.create().withErrandId(ERRAND_ID)
				.withPersons(List.of(person(ROLE_APPLICANT, APPLICANT), person(ROLE_CO_APPLICANT, CO_APPLICANT)))));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, otherCo)).thenReturn(Optional.of(otherCoPnr));
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareCaseSummary.none());
		when(lifecareCaseServiceMock.summarize(eq(otherCoPnr), any()))
			.thenReturn(new LifecareCaseSummary(true, Set.of(), null, false, false));
		final var request = EligibilityRequest.create().withApplicant(APPLICANT).withCoApplicant(otherCo);

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, request);

		assertThat(response.getReasonCode()).isEqualTo(REASON_MARITAL_STATUS_CHANGED);
		assertThat(response.getMaritalStatusMatches()).isFalse();
		assertThat(response.getSuggestions()).singleElement().satisfies(s -> assertThat(s.getTypeSlug()).isEqualTo(SLUG_NEW));
	}

	// ---- 2.5) Recently-closed gate -----------------------------------------------------------------------------------

	@Test
	void recentlyClosedSuggestsRenewalAndSurfacesErrand() {
		final var closedAt = OffsetDateTime.now().minusDays(5);
		cmErrand(person(ROLE_APPLICANT, APPLICANT)); // applicant exists → passes existence + marital (alone)
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareCaseSummary(true, Set.of(), null, false, false));
		when(recentlyClosedErrandServiceMock.findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any()))
			.thenReturn(Optional.of(new RecentlyClosedErrandService.RecentlyClosed(ERRAND_ID, closedAt)));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_RECENTLY_CLOSED);
		assertThat(response.getReopenableErrandId()).isEqualTo(ERRAND_ID);
		assertThat(response.getClosedAt()).isEqualTo(closedAt);
		assertThat(response.getSuggestions()).singleElement()
			.satisfies(s -> assertThat(s.getTypeSlug()).isEqualTo(SLUG_RENEWAL))
			.satisfies(s -> assertThat(s.isRecommended()).isTrue())
			.satisfies(s -> assertThat(s.getPeriodMonth()).isEqualTo(CURRENT.getMonthValue()));
	}

	@Test
	void noRecentlyClosedFallsThroughToPerMonth() {
		// Recently-closed service finds nothing → routing continues to the normal per-month logic.
		cmErrand(person(ROLE_APPLICANT, APPLICANT));
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareCaseSummary(true, Set.of(), null, false, false));
		when(recentlyClosedErrandServiceMock.findRecentlyClosed(eq(MUNICIPALITY_ID), eq(NAMESPACE), any())).thenReturn(Optional.empty());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_EXISTING_CASE);
		assertThat(response.getReopenableErrandId()).isNull();
	}

	// ---- 3) Per-month logic ------------------------------------------------------------------------------------------

	@Test
	void existingCaseNoDecisionThisMonthRecommendsRenewalThisMonth() {
		cmErrand(person(ROLE_APPLICANT, APPLICANT)); // no period → no application for this/next month
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareCaseSummary(true, Set.of(), null, false, false));

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
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareCaseSummary(true, Set.of(CURRENT), CURRENT, true, false));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_EXISTING_CASE);
		assertThat(response.isCurrentMonthDecided()).isTrue();
		assertThat(response.isApplicationExistsThisMonth()).isTrue();
		assertThat(response.isHasPreviousCalculation()).isTrue();
		final var primary = recommended(response.getSuggestions());
		assertThat(primary.getTypeSlug()).isEqualTo(SLUG_RENEWAL);
		assertThat(primary.getPeriodMonth()).isEqualTo(NEXT.getMonthValue());
		// the non-recommended option is a supplementary application for the current month
		assertThat(response.getSuggestions()).anySatisfy(s -> {
			assertThat(s.getTypeSlug()).isEqualTo(SLUG_SUPPLEMENTARY);
			assertThat(s.getPeriodMonth()).isEqualTo(CURRENT.getMonthValue());
		});
	}

	@Test
	void cmApplicationForThisMonthYieldsSupplement() {
		// A CM application already exists for the current month within the window → supplementary application this month.
		cmErrandWithPeriod(CURRENT, OffsetDateTime.now(), person(ROLE_APPLICANT, APPLICANT));
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareCaseSummary(true, Set.of(), null, false, false));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.isApplicationExistsThisMonth()).isTrue();
		final var primary = recommended(response.getSuggestions());
		assertThat(primary.getTypeSlug()).isEqualTo(SLUG_SUPPLEMENTARY);
		assertThat(primary.getPeriodMonth()).isEqualTo(CURRENT.getMonthValue());
	}

	@Test
	void cmApplicationOutsideWindowDoesNotCount() {
		// Same-month CM application but created long ago → outside 90-day window → still renewal.
		cmErrandWithPeriod(CURRENT, OffsetDateTime.now().minusDays(200), person(ROLE_APPLICANT, APPLICANT));
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any()))
			.thenReturn(new LifecareCaseSummary(true, Set.of(), null, false, false));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.isApplicationExistsThisMonth()).isFalse();
		assertThat(recommended(response.getSuggestions()).getTypeSlug()).isEqualTo(SLUG_RENEWAL);
	}

	// ---- Degradation -------------------------------------------------------------------------------------------------

	@Test
	void lifecareUnavailableButExistsInCmStillRoutes() {
		cmErrand(person(ROLE_APPLICANT, APPLICANT));
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenThrow(Problem.valueOf(BAD_GATEWAY, "FamilyCare down"));

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.isLifecareChecked()).isFalse();
		assertThat(response.isExistsInCm()).isTrue();
		assertThat(response.getReasonCode()).isEqualTo(REASON_EXISTING_CASE);
		assertThat(recommended(response.getSuggestions()).getTypeSlug()).isEqualTo(SLUG_RENEWAL);
	}

	@Test
	void configuredWindowIsReflected() {
		noCmErrands();
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareCaseSummary.none());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getWindowDays()).isEqualTo(90); // from the service config, not the request
	}

	// ---- The return-all-types test override --------------------------------------------------------------------------

	@Test
	void returnAllTypesOffersEveryApplicationTypeAndConsultsNothing() {
		final var response = serviceReturningAllTypes().evaluate(MUNICIPALITY_ID, NAMESPACE, together());

		assertThat(response.getReasonCode()).isEqualTo(REASON_ALL_TYPES_TEST);
		assertThat(response.isHasCoApplicant()).isTrue();
		assertThat(response.getSuggestions()).extracting(ApplicationSuggestion::getTypeSlug)
			.containsExactly(SLUG_NEW, SLUG_RENEWAL, SLUG_SUPPLEMENTARY);
		assertThat(recommended(response.getSuggestions()).getTypeSlug()).isEqualTo(SLUG_NEW);

		// The override short-circuits ahead of every gate — nothing is read, not even the protected-identity check.
		verifyNoInteractions(errandQueryServiceMock, financialAssistanceRepositoryMock, lifecareCaseServiceMock, citizenServiceMock,
			recentlyClosedErrandServiceMock);
	}

	@Test
	void routingIsOnByDefaultWhenTheOverrideIsUnset() {
		noCmErrands();
		when(lifecareCaseServiceMock.summarize(eq(APPLICANT_PNR), any())).thenReturn(LifecareCaseSummary.none());

		final var response = service().evaluate(MUNICIPALITY_ID, NAMESPACE, alone());

		assertThat(response.getReasonCode()).isEqualTo(REASON_NO_EXISTING_CASE).isNotEqualTo(REASON_ALL_TYPES_TEST);
	}
}
