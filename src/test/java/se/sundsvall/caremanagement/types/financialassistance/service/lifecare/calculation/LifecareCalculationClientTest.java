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

import static org.assertj.core.api.Assertions.assertThat;
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
}
