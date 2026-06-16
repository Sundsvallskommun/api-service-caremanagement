package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationIncomePostDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationServiceDTO;
import generated.se.sundsvall.lifecarefc.PostCalculationBodyRequest;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.PreparedSsbtekIncomes;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekChangeWarning;
import se.sundsvall.caremanagement.lifecare.service.model.UnhandledIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.lifecare.service.model.UnhandledReason.NOT_ON_WHITELIST;

@ExtendWith(MockitoExtension.class)
class NormberakningServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String APPLICANT = "199001011234";
	private static final String CO_APPLICANT = "199202022345";
	private static final YearMonth MONTH = YearMonth.of(2026, 6);

	@Mock
	private SsbtekIncomeFetchService ssbtekIncomeFetchServiceMock;

	@Mock
	private LifecareFcIntegration lifecareFcIntegrationMock;

	@InjectMocks
	private NormberakningService service;

	@Test
	void buildAndPostFetchesAssemblesAndPosts() {
		final var incomeRow = new PersonBasedCalculationIncomePostDTO().id(10);
		final var unhandled = new UnhandledIncome("Bostadstillägg", null, null, NOT_ON_WHITELIST);
		final var warning = new SsbtekChangeWarning("Bostadsbidrag", new BigDecimal("2400"), new BigDecimal("1850"), new BigDecimal("-23"));
		final var prepared = new PreparedSsbtekIncomes(List.of(incomeRow), List.of(unhandled), List.of(warning));
		final var proposal = new PersonBasedCalculationProposalDTO().addServicesItem(new PersonBasedCalculationServiceDTO().id(5));

		when(ssbtekIncomeFetchServiceMock.fetchAndPrepare(MUNICIPALITY_ID, APPLICANT, CO_APPLICANT, MONTH)).thenReturn(prepared);
		when(lifecareFcIntegrationMock.getCalculationProposal(APPLICANT)).thenReturn(proposal);
		when(lifecareFcIntegrationMock.createCalculation(any(PostCalculationBodyRequest.class))).thenReturn(4711);

		final var result = service.buildAndPost(MUNICIPALITY_ID, APPLICANT, CO_APPLICANT, MONTH);

		assertThat(result.calculationId()).isEqualTo(4711);
		assertThat(result.unhandledIncomes()).containsExactly(unhandled);
		assertThat(result.changeWarnings()).containsExactly(warning);

		final ArgumentCaptor<PostCalculationBodyRequest> captor = ArgumentCaptor.forClass(PostCalculationBodyRequest.class);
		verify(lifecareFcIntegrationMock).createCalculation(captor.capture());
		assertThat(captor.getValue().getPersonId()).isEqualTo(APPLICANT);
		assertThat(captor.getValue().getServiceId()).isEqualTo(5);
		assertThat(captor.getValue().getCalculationIncomes()).containsExactly(incomeRow);
	}
}
