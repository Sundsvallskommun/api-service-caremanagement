package se.sundsvall.caremanagement.lifecare.professionalweb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.professionalweb.integrator.ProfessionalWebIntegratorClient;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

/**
 * The ProfessionalWeb client against lifecare-integrator's resources. Only the Feign client is mocked.
 */
@ExtendWith(MockitoExtension.class)
class ProfessionalWebClientTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private ProfessionalWebIntegratorClient integrator;
	@Mock
	private ProfessionalWebTransport transport;

	private ProfessionalWebClient client;

	@BeforeEach
	void setUp() {
		client = new ProfessionalWebClient(transport, integrator);
	}

	private static JsonNode json(final String text) {
		return JSON.readTree(text);
	}

	// Reminders

	@Test
	void listRemindersReadsTheInsatsReminders() {
		final var answer = json("{\"reminders\":[{\"reminderId\":40}]}");
		when(integrator.listReminders(MUNICIPALITY_ID, 53)).thenReturn(answer);

		assertThat(client.listReminders(53)).isEqualTo(answer);
	}

	@Test
	void readReminderProposalReadsTheInsatsProposal() {
		final var answer = json("{\"reminderForAdd\":{}}");
		when(integrator.readReminderProposal(MUNICIPALITY_ID, 53)).thenReturn(answer);

		assertThat(client.readReminderProposal(53)).isEqualTo(answer);
	}

	@Test
	void readReminderReadsTheReminder() {
		final var answer = json("{\"reminder\":{\"reminderId\":40}}");
		when(integrator.readReminder(MUNICIPALITY_ID, 40)).thenReturn(answer);

		assertThat(client.readReminder(40)).isEqualTo(answer);
	}

	@Test
	void createReminderPostsTheBodyAsItIs() {
		final var body = json("{\"text\":\"Hej\"}");
		final var answer = json("{\"reminderId\":41}");
		when(integrator.createReminder(MUNICIPALITY_ID, body)).thenReturn(answer);

		assertThat(client.createReminder(body)).isEqualTo(answer);
	}

	@Test
	void updateReminderPutsTheBodyOnTheReminder() {
		final var body = json("{\"reminderId\":40}");

		client.updateReminder(40, body);

		verify(integrator).updateReminder(MUNICIPALITY_ID, 40, body);
	}

	@Test
	void removeReminderRemovesTheReminder() {
		client.removeReminder(40);

		verify(integrator).removeReminder(MUNICIPALITY_ID, 40);
	}

	// Answers and errors

	@Test
	void anEmptyAnswerIsAMissingNode() {
		when(integrator.createReminder(MUNICIPALITY_ID, json("{}"))).thenReturn(null);

		assertThat(client.createReminder(json("{}")).isMissingNode()).isTrue();
	}

	@Test
	void aRefusalFromTheIntegratorPassesUnchanged() {
		final var refusal = Problem.valueOf(UNPROCESSABLE_CONTENT, "Datum i framtiden");
		when(integrator.createReminder(MUNICIPALITY_ID, json("{}"))).thenThrow(refusal);

		assertThatThrownBy(() -> client.createReminder(json("{}"))).isSameAs(refusal);
	}

	@Test
	void aNotFoundFromTheIntegratorPassesUnchanged() {
		final var notFound = Problem.valueOf(NOT_FOUND, "Bevakningen finns inte");
		doThrow(notFound).when(integrator).removeReminder(MUNICIPALITY_ID, 40);

		assertThatThrownBy(() -> client.removeReminder(40)).isSameAs(notFound);
	}

	@Test
	void anIntegratorThatCannotBeReachedIsABadGateway() {
		when(integrator.listReminders(MUNICIPALITY_ID, 53)).thenThrow(new IllegalStateException("connection reset"));

		assertThatThrownBy(() -> client.listReminders(53))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY));
	}
}
