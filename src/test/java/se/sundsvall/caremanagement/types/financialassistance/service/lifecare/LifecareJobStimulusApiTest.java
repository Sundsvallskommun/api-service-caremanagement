package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.professionalweb.ProfessionalWebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.json;

@ExtendWith(MockitoExtension.class)
class LifecareJobStimulusApiTest {

	@Mock
	private ProfessionalWebClient client;

	@InjectMocks
	private LifecareJobStimulusApi api;

	@Test
	void readForService() {
		final var answer = json("{}");
		when(client.readJobStimulus(24)).thenReturn(answer);

		assertThat(api.readForService(24)).isSameAs(answer);
	}

	@Test
	void readToDate() {
		final var answer = json("\"2030-01-14\"");
		when(client.readJobStimulusEndDate("2028-01-15")).thenReturn(answer);

		assertThat(api.readToDate("2028-01-15")).isSameAs(answer);
	}

	@Test
	void save() {
		final var body = json("{}");
		final var answer = json("{ \"applicant\": null }");
		when(client.saveJobStimulus(body)).thenReturn(answer);

		assertThat(api.save(body)).isSameAs(answer);
	}
}
