package se.sundsvall.caremanagement.financialaid.integration;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

@ExtendWith(MockitoExtension.class)
class FinancialAidIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PERSONAL_NUMBER = "200001012384";
	private static final String FROM_DATE = "2026-04-01";
	private static final String TO_DATE = "2026-06-30";

	@Mock
	private FinancialAidClient clientMock;

	@InjectMocks
	private FinancialAidIntegration integration;

	@Test
	void getFinancialAidBasis() {
		final var response = Map.of("so", Map.<String, Object>of("amount", "1250.50"));
		when(clientMock.getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE)).thenReturn(response);

		final var result = integration.getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE);

		assertThat(result).isSameAs(response);
		verify(clientMock).getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getFinancialAidBasisTranslatesProblem() {
		when(clientMock.getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE))
			.thenThrow(Problem.valueOf(GATEWAY_TIMEOUT, "upstream timeout"));

		assertThatThrownBy(() -> integration.getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(clientMock).getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE);
	}

	@Test
	void getFinancialAidBasisTranslatesGenericFailure() {
		when(clientMock.getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE))
			.thenThrow(new RuntimeException("connection reset"));

		assertThatThrownBy(() -> integration.getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(clientMock).getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE);
	}

	@Test
	void transportFailureMessageIsNotLeakedIntoProblemDetail() {
		// A transport failure embeds the request URL (carrying personalNumber) in its message — must be dropped.
		final var leaky = "GET http://financial-aid/2281/financial-aid?personalNumber=200001012384&fromDate=2026-04-01 failed";
		when(clientMock.getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE))
			.thenThrow(new RuntimeException(leaky));

		assertThatThrownBy(() -> integration.getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, FROM_DATE, TO_DATE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY)
			.extracting(throwable -> ((ThrowableProblem) throwable).getDetail())
			.satisfies(detail -> {
				assertThat(detail).doesNotContain(leaky);
				assertThat(detail).doesNotContain(PERSONAL_NUMBER);
				assertThat(detail).isEqualTo("Error fetching financial-aid basis: RuntimeException");
			});
	}
}
