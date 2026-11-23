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
	private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 6, 15);

	@Nested
	@SpringBootTest(classes = Application.class)
	@ActiveProfiles("junit")
	class WithNothingSet {

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

			verify(familyCare).getActualisations(MUNICIPALITY_ID, PARTY_ID, LocalDate.of(2025, 5, 15), REFERENCE_DATE);
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

			verify(familyCare).getServices(MUNICIPALITY_ID, PARTY_ID, LocalDate.of(2023, 6, 15), REFERENCE_DATE);
			verify(familyCare).getUsers(MUNICIPALITY_ID, 1000, null, null, null);
		}

		@Test
		void professionalWebKeepsItsDefaults() {
			assertThat(professionalWebProperties.decisionPrintTemplateId()).isEqualTo("885bfb68-c97b-47c0-921d-ef00caaa2423");
			assertThat(professionalWebProperties.calculationPrintTemplateId()).isEqualTo("7936e9ec-e32a-40cf-9dea-d95b16e021f0");
			assertThat(professionalWebProperties.documentPrintTemplateId()).isEqualTo("b9f20b49-c8ba-4c44-ad33-b2bf8f0b688a");
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
		"integration.lifecare-professionalweb.test-decision-maker=Test Beslutsfattare",
		"integration.lifecare-professionalweb.decision-print-template-id=decision-template",
		"integration.lifecare-professionalweb.calculation-print-template-id=calculation-template",
		"integration.lifecare-professionalweb.document-print-template-id=document-template"
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

			verify(familyCare).getActualisations(MUNICIPALITY_ID, PARTY_ID, LocalDate.of(2026, 4, 15), REFERENCE_DATE);
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

			verify(familyCare).getServices(MUNICIPALITY_ID, PARTY_ID, LocalDate.of(2026, 1, 15), REFERENCE_DATE);
			verify(familyCare).getUsers(eq(MUNICIPALITY_ID), eq(25), any(), any(), any());
		}

		@Test
		void professionalWebUsesTheSetValues() {
			assertThat(professionalWebProperties.decisionPrintTemplateId()).isEqualTo("decision-template");
			assertThat(professionalWebProperties.calculationPrintTemplateId()).isEqualTo("calculation-template");
			assertThat(professionalWebProperties.documentPrintTemplateId()).isEqualTo("document-template");
			assertThat(professionalWebProperties.testDecisionMaker()).isEqualTo("Test Beslutsfattare");
		}
	}
}
