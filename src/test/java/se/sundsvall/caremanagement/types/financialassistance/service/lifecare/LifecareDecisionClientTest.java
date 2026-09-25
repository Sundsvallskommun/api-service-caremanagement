package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebProperties;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecareDecisionClientTest {

	@Mock
	private ProfessionalWebClient clientMock;

	@Mock
	private ProfessionalWebProperties propertiesMock;

	@InjectMocks
	private LifecareDecisionClient client;

	@Test
	void readsTheUnderlagOfTheInsatsAsLifecaresWebAppAsksForIt() {
		final var answer = JsonNodeFactory.instance.objectNode();
		when(clientMock.get("api2/Decision/GetProposalForService",
			Map.of("businessType", "8", "businessId", "2", "amountType", "", "calculationId", "0", "proposalId", "0"))).thenReturn(answer);

		assertThat(client.readProposal(2)).isSameAs(answer);
	}

	@Test
	void readsTheOrsakCatalogueOfABeslutstyp() {
		final var answer = JsonNodeFactory.instance.arrayNode();
		when(clientMock.get("api2/Decision/GetMappedDecisionReasons", Map.of("id", "153"))).thenReturn(answer);

		assertThat(client.readReasons(153)).isSameAs(answer);
	}

	@Test
	void readsAndUpdatesABeslutByItsId() {
		final var answer = JsonNodeFactory.instance.objectNode();
		final var body = JsonNodeFactory.instance.objectNode();
		when(clientMock.get("api2/Decision/GetDecision", Map.of("businessType", "4", "businessId", "98"))).thenReturn(answer);
		when(clientMock.post("api2/Decision/Update", Map.of("businessType", "4", "businessId", "98"), body)).thenReturn(answer);

		assertThat(client.readDecision(98)).isSameAs(answer);
		assertThat(client.update(98, body)).isSameAs(answer);
	}

	@Test
	void createsABeslutOnTheInsats() {
		final var answer = JsonNodeFactory.instance.objectNode();
		final var body = JsonNodeFactory.instance.objectNode();
		when(clientMock.post("api2/Decision/Create", Map.of("businessType", "8", "businessId", "2"), body)).thenReturn(answer);

		assertThat(client.create(2, body)).isSameAs(answer);
	}

	@Test
	void printsTheBeslutWithTheConfiguredTemplate() {
		final var pdf = "%PDF-1.7".getBytes();
		when(propertiesMock.decisionPrintTemplateId()).thenReturn("template-1");
		when(clientMock.getPdf("RenderPdf/PrintDecision", Map.of("templateId", "template-1", "decisionId", "98", "hideRevisions", "true"))).thenReturn(pdf);

		assertThat(client.printDecision(98)).isSameAs(pdf);
		verify(propertiesMock).decisionPrintTemplateId();
	}
}
