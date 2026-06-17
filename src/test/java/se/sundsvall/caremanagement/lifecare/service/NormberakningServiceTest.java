package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationServiceDTO;
import generated.se.sundsvall.lifecarefc.PostCalculationBodyRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormberakningServiceTest {

	private static final String APPLICANT = "199001011234";
	private static final YearMonth MONTH = YearMonth.of(2026, 6);

	@Mock
	private LifecareFcIntegration lifecareFcIntegrationMock;

	@Mock
	private LifecareEbCaseService lifecareEbCaseServiceMock;

	@Mock
	private ObjectMapper objectMapperMock;

	@InjectMocks
	private NormberakningService service;

	private static ClassifiedIncome bostadsbidrag() {
		return new ClassifiedIncome(
			new SsbtekIncome("Bostadsbidrag", null, "Månad", new BigDecimal("1850"), LocalDate.of(2026, 5, 15), ApplicantRole.APPLICANT),
			"TA_MED_KVITTNING", "Bostadsbidrag", false, "Ta med kvittning");
	}

	private static PersonBasedCalculationProposalDTO proposal() {
		return new PersonBasedCalculationProposalDTO()
			.addServicesItem(new PersonBasedCalculationServiceDTO().id(5))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(20).name("Bostadsbidrag"));
	}

	@Test
	void buildAndPostFromClassifiedMapsCategoryToFcAndPosts() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareFcIntegrationMock.createCalculation(any(PostCalculationBodyRequest.class))).thenReturn(4712);
		when(lifecareEbCaseServiceMock.previousNormberakningIncomeTypes(APPLICANT, MONTH)).thenReturn(List.of("Bostadsbidrag"));

		final var result = service.buildAndPostFromClassified(APPLICANT, MONTH, "[json]");

		assertThat(result.calculationId()).isEqualTo(4712);
		assertThat(result.informationComplete()).isTrue();
		assertThat(result.missingIncomeTypes()).isEmpty();
		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFcIntegrationMock).createCalculation(captor.capture());
		assertThat(captor.getValue().getPersonId()).isEqualTo(APPLICANT);
		assertThat(captor.getValue().getServiceId()).isEqualTo(5);
		assertThat(captor.getValue().getCalculationIncomes()).hasSize(1);
		assertThat(captor.getValue().getCalculationIncomes().getFirst().getId()).isEqualTo(20);
	}

	@Test
	void reportsIncompleteWhenAPreviousIncomeTypeIsMissingThisMonth() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareFcIntegrationMock.createCalculation(any(PostCalculationBodyRequest.class))).thenReturn(4712);
		when(lifecareEbCaseServiceMock.previousNormberakningIncomeTypes(APPLICANT, MONTH)).thenReturn(List.of("Bostadsbidrag", "Dagersättning"));

		final var result = service.buildAndPostFromClassified(APPLICANT, MONTH, "[json]");

		assertThat(result.informationComplete()).isFalse();
		assertThat(result.missingIncomeTypes()).containsExactly("Dagersättning");
	}

	@Test
	void treatsCompletenessAsCompleteWhenPreviousLookupFails() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal());
		when(lifecareFcIntegrationMock.createCalculation(any(PostCalculationBodyRequest.class))).thenReturn(4712);
		when(lifecareEbCaseServiceMock.previousNormberakningIncomeTypes(APPLICANT, MONTH)).thenThrow(new RuntimeException("FC down"));

		final var result = service.buildAndPostFromClassified(APPLICANT, MONTH, "[json]");

		assertThat(result.calculationId()).isEqualTo(4712);
		assertThat(result.informationComplete()).isTrue();
		assertThat(result.missingIncomeTypes()).isEmpty();
	}
}
