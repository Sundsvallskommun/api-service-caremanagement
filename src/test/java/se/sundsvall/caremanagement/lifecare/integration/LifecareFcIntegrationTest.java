package se.sundsvall.caremanagement.lifecare.integration;

import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefc.PostCalculationBodyRequest;
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
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class LifecareFcIntegrationTest {

	private static final String PERSON_ID = "200001012384";

	@Mock
	private LifecareFcClient clientMock;

	@InjectMocks
	private LifecareFcIntegration integration;

	@Test
	void getActualisationProposal() {
		final var response = new PersonBasedAktualiseringProposalDTO();
		when(clientMock.getActualisationProposal(PERSON_ID)).thenReturn(response);

		final var result = integration.getActualisationProposal(PERSON_ID);

		assertThat(result).isSameAs(response);
		verify(clientMock).getActualisationProposal(PERSON_ID);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getActualisationProposalFailure() {
		when(clientMock.getActualisationProposal(PERSON_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "boom"));

		assertThatThrownBy(() -> integration.getActualisationProposal(PERSON_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(clientMock).getActualisationProposal(PERSON_ID);
	}

	@Test
	void createActualisation() {
		final var body = new PostAktualiseringsBodyRequest();
		when(clientMock.createActualisation(body)).thenReturn(4711);

		final var result = integration.createActualisation(body);

		assertThat(result).isEqualTo(4711);
		verify(clientMock).createActualisation(body);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void createActualisationFailure() {
		final var body = new PostAktualiseringsBodyRequest();
		when(clientMock.createActualisation(body)).thenThrow(new RuntimeException("connection reset"));

		assertThatThrownBy(() -> integration.createActualisation(body))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(clientMock).createActualisation(body);
	}

	@Test
	void getCalculationProposal() {
		final var response = new PersonBasedCalculationProposalDTO();
		when(clientMock.getCalculationProposal(PERSON_ID)).thenReturn(response);

		final var result = integration.getCalculationProposal(PERSON_ID);

		assertThat(result).isSameAs(response);
		verify(clientMock).getCalculationProposal(PERSON_ID);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void getCalculationProposalFailure() {
		when(clientMock.getCalculationProposal(PERSON_ID)).thenThrow(new RuntimeException("timeout"));

		assertThatThrownBy(() -> integration.getCalculationProposal(PERSON_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(clientMock).getCalculationProposal(PERSON_ID);
	}

	@Test
	void createCalculation() {
		final var body = new PostCalculationBodyRequest();
		when(clientMock.createCalculation(body)).thenReturn(99);

		final var result = integration.createCalculation(body);

		assertThat(result).isEqualTo(99);
		verify(clientMock).createCalculation(body);
		verifyNoMoreInteractions(clientMock);
	}

	@Test
	void createCalculationFailure() {
		final var body = new PostCalculationBodyRequest();
		when(clientMock.createCalculation(body)).thenThrow(Problem.valueOf(BAD_GATEWAY, "upstream down"));

		assertThatThrownBy(() -> integration.createCalculation(body))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_GATEWAY);

		verify(clientMock).createCalculation(body);
	}
}
