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
	void readsTheUnderlagOfTheInsatsAsLifecaresWebAppAsksForIt() {
		final var answer = JsonNodeFactory.instance.objectNode();
		when(clientMock.readDecisionProposal(2, "", 0)).thenReturn(answer);

		assertThat(client.readProposal(2)).isSameAs(answer);
	}

	@Test
	void readsTheOrsakCatalogueOfABeslutstyp() {
		final var answer = JsonNodeFactory.instance.arrayNode();
		when(clientMock.readDecisionReasons(153)).thenReturn(answer);

		assertThat(client.readReasons(153)).isSameAs(answer);
	}

	@Test
	void readsAndUpdatesABeslutByItsId() {
		final var answer = JsonNodeFactory.instance.objectNode();
		final var body = JsonNodeFactory.instance.objectNode();
		when(clientMock.readDecision(98)).thenReturn(answer);
		when(clientMock.updateDecision(98, body)).thenReturn(answer);

		assertThat(client.readDecision(98)).isSameAs(answer);
		assertThat(client.update(98, body)).isSameAs(answer);
	}

	@Test
	void createsABeslutOnTheInsats() {
		final var answer = JsonNodeFactory.instance.objectNode();
		final var body = JsonNodeFactory.instance.objectNode();
		when(clientMock.createDecision(2, body)).thenReturn(answer);

		assertThat(client.create(2, body)).isSameAs(answer);
	}

	@Test
	void printsTheBeslut() {
		final var pdf = "%PDF-1.7".getBytes();
		when(clientMock.printDecision(98)).thenReturn(pdf);

		assertThat(client.printDecision(98)).isSameAs(pdf);
	}

	@Test
	void readsTheUnderlagFromANormberakningAsLifecaresCalculationViewAsksForIt() {
		final var answer = JsonNodeFactory.instance.objectNode();
		when(clientMock.readDecisionProposal(22, "TotalSum", 25)).thenReturn(answer);

		assertThat(client.readProposal(22, 25)).isSameAs(answer);
	}
}
