package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.tree;

@ExtendWith(MockitoExtension.class)
class LifecareCalculationClientTest {

	@Mock
	private ProfessionalWebClient professionalWeb;

	@Captor
	private ArgumentCaptor<JsonNode> bodyCaptor;

	private LifecareCalculationClient client;

	@BeforeEach
	void setUp() {
		client = new LifecareCalculationClient(professionalWeb);
	}

	@Test
	void deletesABeräkning() {
		client.delete(53);

		verify(professionalWeb).deleteCalculation(53);
	}

	@Test
	void readsTheUnderlagForANewBeräkningOnTheInsats() {
		final var answer = json("{\"calculation\":{}}");
		when(professionalWeb.readCalculationProposal(24)).thenReturn(answer);

		assertThat(client.readProposal(24)).isSameAs(answer);
	}

	@Test
	void listsTheInsatssBeräkningar() {
		final var answer = tree("[{\"calculationId\":31}]");
		when(professionalWeb.listLatestCalculations(24)).thenReturn(answer);

		assertThat(client.listForService(24)).isSameAs(answer);
	}

	@Test
	void readsASavedBeräkning() {
		final var answer = json("{\"calculationId\":31}");
		when(professionalWeb.readCalculation(31)).thenReturn(answer);

		assertThat(client.read(31)).isSameAs(answer);
	}

	@Test
	void readsASavedBeräkningForEdit() {
		final var answer = json("{\"calculation\":{\"calculationId\":31}}");
		when(professionalWeb.readCalculationForEdit(31)).thenReturn(answer);

		assertThat(client.readForEdit(31)).isSameAs(answer);
	}

	@Test
	void readsTheInsatssJobbstimulans() {
		final var answer = json("{\"applicant\":null}");
		when(professionalWeb.readJobStimulus(24)).thenReturn(answer);

		assertThat(client.readJobStimulus(24)).isSameAs(answer);
	}

	@Test
	void placesPersons() {
		final var body = json("{\"normId\":1}");
		final var answer = json("{\"calculationPersons\":[]}");
		when(professionalWeb.placePersons(body)).thenReturn(answer);

		assertThat(client.placePersons(body)).isSameAs(answer);
	}

	@Test
	void countsAMembersAmount() {
		when(professionalWeb.readAmount(bodyCaptor.capture())).thenReturn(json("{\"amount\":1300}"));

		final var amount = client.amountFor(json("{\"personId\":\"a\"}"), json("{\"rowId\":2}"), tree("\"2026-09-01\""), tree("\"2026-09-30\""));

		assertThat(amount.intValue()).isEqualTo(1300);
		assertThat(bodyCaptor.getValue()).isEqualTo(json("""
			{"person":{"personId":"a"},"normRow":{"rowId":2},"startDate":"2026-09-01","endDate":"2026-09-30"}"""));
	}

	@Test
	void countsTheGemensammaKostnader() {
		when(professionalWeb.readSharedCost(bodyCaptor.capture())).thenReturn(tree("2030"));

		assertThat(client.sharedCost(tree("\"2026-09-01\""), tree("\"2026-09-30\""), json("{\"noOfMembers\":4}")).intValue()).isEqualTo(2030);
		assertThat(bodyCaptor.getValue()).isEqualTo(json("{\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-30\",\"normShared\":{\"noOfMembers\":4}}"));
	}

	@Test
	void marksJobbstimulans() {
		final var answer = json("{\"calculationId\":1,\"hasApplicantJobStimuli\":true}");
		when(professionalWeb.readJobStimuliForCalculation(bodyCaptor.capture())).thenReturn(answer);

		assertThat(client.withJobStimuli(json("{\"calculationId\":1}"), json("{\"applicant\":null}"))).isSameAs(answer);
		assertThat(bodyCaptor.getValue()).isEqualTo(json("{\"calculation\":{\"calculationId\":1},\"jobStimulus\":{\"applicant\":null}}"));
	}

	@Test
	void createsOnTheInsats() {
		final ObjectNode body = json("{}");
		final var answer = json("{\"calculationId\":31}");
		when(professionalWeb.createCalculation(24, body)).thenReturn(answer);

		assertThat(client.create(24, body)).isSameAs(answer);
	}

	@Test
	void updatesTheBeräkning() {
		final ObjectNode body = json("{}");
		final var answer = json("{\"calculationId\":31}");
		when(professionalWeb.updateCalculation(31, body)).thenReturn(answer);

		assertThat(client.update(31, body)).isSameAs(answer);
	}

	@Test
	void printsTheBeräkning() {
		final var pdf = new byte[] {
			1
		};
		when(professionalWeb.printCalculation(31)).thenReturn(pdf);

		assertThat(client.print(31)).isSameAs(pdf);
	}
}
