package se.sundsvall.caremanagement.lifecare;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedServiceDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import se.sundsvall.caremanagement.Application;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCare;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebProperties;
import se.sundsvall.caremanagement.lifecare.service.CaseworkerResolver;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;

import static java.time.Month.APRIL;
import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Lifecare settings that are not about how Lifecare is reached keep their names and defaults. Only FamilyCare
 * itself (the integrator route) is mocked; the services read the settings as they are bound.
 */
class LifecareSettingsBindingTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PARTY_ID = "6a5c3d18-1f2b-4e77-9c0a-2b3d4e5f6a7b";
	private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, JUNE, 15);

	@Nested
	@SpringBootTest(classes = Application.class)
	@ActiveProfiles("junit")
	class WithTheShippedDefaults {

		@MockitoBean
		private LifecareFamilyCare familyCare;

		@Autowired
		private LifecareCaseService lifecareCaseService;

		@Autowired
		private CaseworkerResolver caseworkerResolver;

		@Autowired
		private ProfessionalWebProperties professionalWebProperties;

		@Test
		void lookbackIsThirteenMonths() {
			lifecareCaseService.summarize(MUNICIPALITY_ID, PARTY_ID, REFERENCE_DATE);

			verify(familyCare).getActualisations(MUNICIPALITY_ID, PARTY_ID, LocalDate.of(2025, MAY, 15), REFERENCE_DATE);
		}

		@Test
		void aktuellIsAnOpenActualisation() {
			whenActualisationStatusIs("aktuell");

			assertThat(lifecareCaseService.summarize(MUNICIPALITY_ID, PARTY_ID, REFERENCE_DATE).hasOpenCase()).isTrue();
		}

		@Test
		void anyOtherStatusIsNotAnOpenActualisation() {
			whenActualisationStatusIs("Avslutad");

			assertThat(lifecareCaseService.summarize(MUNICIPALITY_ID, PARTY_ID, REFERENCE_DATE).hasOpenCase()).isFalse();
		}

		@Test
		void caseworkerLookbackIsThirtySixMonthsAndUsersLimitIsAThousand() {
			whenAnOpenServiceHasACaseworker();

			caseworkerResolver.resolve(MUNICIPALITY_ID, PARTY_ID, REFERENCE_DATE);

			verify(familyCare).getServices(MUNICIPALITY_ID, PARTY_ID, LocalDate.of(2023, JUNE, 15), REFERENCE_DATE);
			verify(familyCare).getUsers(MUNICIPALITY_ID, 1000, null, null, null);
		}

		@Test
		void professionalWebKeepsItsDefaults() {
			assertThat(professionalWebProperties.testDecisionMaker()).isNullOrEmpty();
		}

		private void whenActualisationStatusIs(final String status) {
			when(familyCare.getActualisations(eq(MUNICIPALITY_ID), eq(PARTY_ID), any(), any()))
				.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO().addResultItem(new PersonBasedAktualiseringDTO().status(status)));
		}

		private void whenAnOpenServiceHasACaseworker() {
			when(familyCare.getServices(eq(MUNICIPALITY_ID), eq(PARTY_ID), any(), any()))
				.thenReturn(new ApiPaginationCompositePersonBasedServiceDTO().addResultItem(new PersonBasedServiceDTO().startDate("2026-01-01").caseworker("Anna Andersson")));
		}
	}

	@Nested
	@SpringBootTest(classes = Application.class, properties = {
		"integration.lifecare-familycare.lookback-months=2",
		"integration.lifecare-familycare.open-actualisation-statuses=Pågående,Vilande",
		"integration.lifecare-familycare.caseworker-lookback-months=5",
		"integration.lifecare-familycare.users-limit=25",
		"integration.lifecare-professionalweb.test-decision-maker=Test Beslutsfattare"
	})
	@ActiveProfiles("junit")
	class WithEverythingSet {

		@MockitoBean
		private LifecareFamilyCare familyCare;

		@Autowired
		private LifecareCaseService lifecareCaseService;

		@Autowired
		private CaseworkerResolver caseworkerResolver;

		@Autowired
		private ProfessionalWebProperties professionalWebProperties;

		@Test
		void theSetLookbackIsUsed() {
			lifecareCaseService.summarize(MUNICIPALITY_ID, PARTY_ID, REFERENCE_DATE);

			verify(familyCare).getActualisations(MUNICIPALITY_ID, PARTY_ID, LocalDate.of(2026, APRIL, 15), REFERENCE_DATE);
		}

		@Test
		void theSetOpenStatusesAreUsed() {
			when(familyCare.getActualisations(eq(MUNICIPALITY_ID), eq(PARTY_ID), any(), any()))
				.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO().addResultItem(new PersonBasedAktualiseringDTO().status("Vilande")));

			assertThat(lifecareCaseService.summarize(MUNICIPALITY_ID, PARTY_ID, REFERENCE_DATE).hasOpenCase()).isTrue();
		}

		@Test
		void aktuellIsNoLongerOpenWhenTheStatusesAreSet() {
			when(familyCare.getActualisations(eq(MUNICIPALITY_ID), eq(PARTY_ID), any(), any()))
				.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO().addResultItem(new PersonBasedAktualiseringDTO().status("Aktuell")));

			assertThat(lifecareCaseService.summarize(MUNICIPALITY_ID, PARTY_ID, REFERENCE_DATE).hasOpenCase()).isFalse();
		}

		@Test
		void theSetCaseworkerLookbackAndUsersLimitAreUsed() {
			when(familyCare.getServices(eq(MUNICIPALITY_ID), eq(PARTY_ID), any(), any()))
				.thenReturn(new ApiPaginationCompositePersonBasedServiceDTO().addResultItem(new PersonBasedServiceDTO().startDate("2026-01-01").caseworker("Anna Andersson")));

			caseworkerResolver.resolve(MUNICIPALITY_ID, PARTY_ID, REFERENCE_DATE);

			verify(familyCare).getServices(MUNICIPALITY_ID, PARTY_ID, LocalDate.of(2026, JANUARY, 15), REFERENCE_DATE);
			verify(familyCare).getUsers(eq(MUNICIPALITY_ID), eq(25), any(), any(), any());
		}

		@Test
		void professionalWebUsesTheSetValues() {
			assertThat(professionalWebProperties.testDecisionMaker()).isEqualTo("Test Beslutsfattare");
		}
	}
}
