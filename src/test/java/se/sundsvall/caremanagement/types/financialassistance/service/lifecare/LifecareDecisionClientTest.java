package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecareDecisionClientTest {

	@Mock
	private ProfessionalWebClient clientMock;

	@InjectMocks
	private LifecareDecisionClient client;

	@Test
	void readsTheUnderlagFromANormberakningAsLifecaresCalculationViewAsksForIt() {
		final var answer = JsonNodeFactory.instance.objectNode();
		when(clientMock.readDecisionProposal(22, "TotalSum", 25)).thenReturn(answer);

		assertThat(client.readProposal(22, 25)).isSameAs(answer);
	}
}
