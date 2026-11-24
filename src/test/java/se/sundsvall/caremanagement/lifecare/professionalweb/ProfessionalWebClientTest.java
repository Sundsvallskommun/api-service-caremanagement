package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

	private ProfessionalWebClient client;

	@BeforeEach
	void setUp() {
		client = new ProfessionalWebClient(integrator);
	}

	@AfterEach
	void verifyNoUnexpectedCalls() {
		verifyNoMoreInteractions(integrator);
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

	// Job stimulus

	@Test
	void readJobStimulusReadsTheInsatsPeriods() {
		final var answer = json("{\"applicant\":[]}");
		when(integrator.readJobStimulus(MUNICIPALITY_ID, 24)).thenReturn(answer);

		assertThat(client.readJobStimulus(24)).isEqualTo(answer);
	}

	@Test
	void readJobStimulusEndDateAsksWithTheStartDate() {
		final var answer = json("\"2030-01-14\"");
		when(integrator.readJobStimulusEndDate(MUNICIPALITY_ID, "2028-01-15")).thenReturn(answer);

		assertThat(client.readJobStimulusEndDate("2028-01-15")).isEqualTo(answer);
	}

	@Test
	void saveJobStimulusPostsTheBodyAsItIs() {
		final var body = json("{\"applicant\":[]}");
		final var answer = json("{\"applicant\":[{\"jobStimulusId\":110}]}");
		when(integrator.saveJobStimulus(MUNICIPALITY_ID, body)).thenReturn(answer);

		assertThat(client.saveJobStimulus(body)).isEqualTo(answer);
	}

	// Decisions

	@Test
	void readDecisionProposalAsksWithTheAmountTypeAndTheCalculation() {
		final var answer = json("{\"decision\":{}}");
		when(integrator.readDecisionProposal(MUNICIPALITY_ID, 22, "TotalSum", 25)).thenReturn(answer);

		assertThat(client.readDecisionProposal(22, "TotalSum", 25)).isEqualTo(answer);
	}

	@Test
	void readDecisionReasonsReadsTheDecisionCodesReasons() {
		final var answer = json("[{\"id\":1}]");
		when(integrator.readDecisionReasons(MUNICIPALITY_ID, 153)).thenReturn(answer);

		assertThat(client.readDecisionReasons(153)).isEqualTo(answer);
	}

	@Test
	void readDecisionReadsTheDecision() {
		final var answer = json("{\"decisionId\":98}");
		when(integrator.readDecision(MUNICIPALITY_ID, 98)).thenReturn(answer);

		assertThat(client.readDecision(98)).isEqualTo(answer);
	}

	@Test
	void createDecisionPostsTheBodyOnTheInsats() {
		final var body = json("{\"decisionCode\":153}");
		final var answer = json("{\"decisionId\":98}");
		when(integrator.createDecision(MUNICIPALITY_ID, 2, body)).thenReturn(answer);

		assertThat(client.createDecision(2, body)).isEqualTo(answer);
	}

	@Test
	void updateDecisionPutsTheBodyOnTheDecision() {
		final var body = json("{\"decisionId\":98}");
		final var answer = json("{\"decisionId\":98,\"saved\":true}");
		when(integrator.updateDecision(MUNICIPALITY_ID, 98, body)).thenReturn(answer);

		assertThat(client.updateDecision(98, body)).isEqualTo(answer);
	}

	// Payments

	@Test
	void readPaymentProposalReadsTheInsatsUnderlag() {
		final var answer = json("{\"payment\":{}}");
		when(integrator.readPaymentProposal(MUNICIPALITY_ID, 24)).thenReturn(answer);

		assertThat(client.readPaymentProposal(24)).isEqualTo(answer);
	}

	@Test
	void readLatestPaymentsReadsTheInsatsPayments() {
		final var answer = json("[{\"paymentId\":4}]");
		when(integrator.readLatestPayments(MUNICIPALITY_ID, 24)).thenReturn(answer);

		assertThat(client.readLatestPayments(24)).isEqualTo(answer);
	}

	@Test
	void hasHouseholdAsksForTheInsatsOnTheDate() {
		when(integrator.hasHousehold(MUNICIPALITY_ID, 24, "2026-09-21")).thenReturn(json("true"));

		assertThat(client.hasHousehold(24, "2026-09-21")).isEqualTo(json("true"));
	}

	@Test
	void createPaymentPostsTheBodyOnTheInsats() {
		final var body = json("{\"amount\":1.0}");
		final var answer = json("{\"paymentId\":4}");
		when(integrator.createPayment(MUNICIPALITY_ID, 24, body)).thenReturn(answer);

		assertThat(client.createPayment(24, body)).isEqualTo(answer);
	}

	@Test
	void createPayeePostsTheBodyAsItIs() {
		final var body = json("{\"name\":\"Hyresvärden\"}");
		final var answer = json("{\"payeeId\":3}");
		when(integrator.createPayee(MUNICIPALITY_ID, body)).thenReturn(answer);

		assertThat(client.createPayee(body)).isEqualTo(answer);
	}

	// Calculations

	@Test
	void readCalculationProposalReadsTheInsatsUnderlag() {
		final var answer = json("{\"calculation\":{}}");
		when(integrator.readCalculationProposal(MUNICIPALITY_ID, 24)).thenReturn(answer);

		assertThat(client.readCalculationProposal(24)).isEqualTo(answer);
	}

	@Test
	void listLatestCalculationsReadsTheInsatsCalculations() {
		final var answer = json("[{\"calculationId\":31}]");
		when(integrator.listLatestCalculations(MUNICIPALITY_ID, 24)).thenReturn(answer);

		assertThat(client.listLatestCalculations(24)).isEqualTo(answer);
	}

	@Test
	void readCalculationReadsTheCalculation() {
		final var answer = json("{\"calculationId\":31}");
		when(integrator.readCalculation(MUNICIPALITY_ID, 31)).thenReturn(answer);

		assertThat(client.readCalculation(31)).isEqualTo(answer);
	}

	@Test
	void readCalculationForEditReadsTheCalculationForEdit() {
		final var answer = json("{\"calculation\":{\"calculationId\":31}}");
		when(integrator.readCalculationForEdit(MUNICIPALITY_ID, 31)).thenReturn(answer);

		assertThat(client.readCalculationForEdit(31)).isEqualTo(answer);
	}

	@Test
	void theComputationsPostTheirBodiesAsTheyAre() {
		final var body = json("{\"normId\":1}");
		when(integrator.placePersons(MUNICIPALITY_ID, body)).thenReturn(json("{\"placed\":1}"));
		when(integrator.readAmount(MUNICIPALITY_ID, body)).thenReturn(json("{\"amount\":1300}"));
		when(integrator.readSharedCost(MUNICIPALITY_ID, body)).thenReturn(json("2030"));
		when(integrator.readJobStimuliForCalculation(MUNICIPALITY_ID, body)).thenReturn(json("{\"marked\":1}"));

		assertThat(client.placePersons(body)).isEqualTo(json("{\"placed\":1}"));
		assertThat(client.readAmount(body)).isEqualTo(json("{\"amount\":1300}"));
		assertThat(client.readSharedCost(body)).isEqualTo(json("2030"));
		assertThat(client.readJobStimuliForCalculation(body)).isEqualTo(json("{\"marked\":1}"));
	}

	@Test
	void createCalculationPostsTheBodyOnTheInsats() {
		final var body = json("{\"calculation\":{}}");
		final var answer = json("{\"calculationId\":31}");
		when(integrator.createCalculation(MUNICIPALITY_ID, 24, body)).thenReturn(answer);

		assertThat(client.createCalculation(24, body)).isEqualTo(answer);
	}

	@Test
	void updateCalculationPutsTheBodyOnTheCalculation() {
		final var body = json("{\"calculation\":{}}");
		final var answer = json("{\"calculationId\":31}");
		when(integrator.updateCalculation(MUNICIPALITY_ID, 31, body)).thenReturn(answer);

		assertThat(client.updateCalculation(31, body)).isEqualTo(answer);
	}

	@Test
	void deleteCalculationDeletesTheCalculation() {
		client.deleteCalculation(31);

		verify(integrator).deleteCalculation(MUNICIPALITY_ID, 31);
	}

	// Journal notes and documents

	@Test
	void listDocumentsReadsThePersonsRecordByPartyId() {
		final var answer = json("{\"documentModels\":[]}");
		when(integrator.listDocuments(MUNICIPALITY_ID, "party-1")).thenReturn(answer);

		assertThat(client.listDocuments("party-1")).isEqualTo(answer);
	}

	@Test
	void theRecordsAreReadInViewModeOrForEdit() {
		when(integrator.readJournalNote(MUNICIPALITY_ID, 1)).thenReturn(json("{\"view\":\"note\"}"));
		when(integrator.readJournalNoteForEdit(MUNICIPALITY_ID, 1)).thenReturn(json("{\"edit\":\"note\"}"));
		when(integrator.readDocument(MUNICIPALITY_ID, 3)).thenReturn(json("{\"view\":\"document\"}"));
		when(integrator.readDocumentForEdit(MUNICIPALITY_ID, 3)).thenReturn(json("{\"edit\":\"document\"}"));

		assertThat(client.readJournalNote(1)).isEqualTo(json("{\"view\":\"note\"}"));
		assertThat(client.readJournalNoteForEdit(1)).isEqualTo(json("{\"edit\":\"note\"}"));
		assertThat(client.readDocument(3)).isEqualTo(json("{\"view\":\"document\"}"));
		assertThat(client.readDocumentForEdit(3)).isEqualTo(json("{\"edit\":\"document\"}"));
	}

	@Test
	void theProposalsAreReadOnTheInsats() {
		when(integrator.readJournalNoteProposal(MUNICIPALITY_ID, 2)).thenReturn(json("{\"documentJournalNote\":{}}"));
		when(integrator.readDocumentProposal(MUNICIPALITY_ID, 2)).thenReturn(json("{\"document\":{}}"));

		assertThat(client.readJournalNoteProposal(2)).isEqualTo(json("{\"documentJournalNote\":{}}"));
		assertThat(client.readDocumentProposal(2)).isEqualTo(json("{\"document\":{}}"));
	}

	@Test
	void theRecordsAreCreatedWithTheirBodiesAsTheyAre() {
		final var body = json("{\"title\":\"Anteckning\"}");
		when(integrator.createJournalNote(MUNICIPALITY_ID, body)).thenReturn(json("{\"id\":138}"));
		when(integrator.createDocument(MUNICIPALITY_ID, body)).thenReturn(json("{\"id\":139}"));

		assertThat(client.createJournalNote(body)).isEqualTo(json("{\"id\":138}"));
		assertThat(client.createDocument(body)).isEqualTo(json("{\"id\":139}"));
	}

	@Test
	void theRecordsAreUpdatedOnTheirIds() {
		final var body = json("{\"documentId\":1}");
		when(integrator.updateJournalNote(MUNICIPALITY_ID, 1, body)).thenReturn(json("{\"saved\":\"note\"}"));
		when(integrator.updateDocument(MUNICIPALITY_ID, 3, body)).thenReturn(json("{\"saved\":\"document\"}"));

		assertThat(client.updateJournalNote(1, body)).isEqualTo(json("{\"saved\":\"note\"}"));
		assertThat(client.updateDocument(3, body)).isEqualTo(json("{\"saved\":\"document\"}"));
	}

	// Prints

	@Test
	void thePrintsAreLifecaresPdfUnchanged() {
		final var decision = "%PDF-1.7 beslut".getBytes(StandardCharsets.UTF_8);
		final var document = "%PDF-1.7 dokument".getBytes(StandardCharsets.UTF_8);
		final var calculation = "%PDF-1.7 beräkning".getBytes(StandardCharsets.UTF_8);
		when(integrator.printDecision(MUNICIPALITY_ID, 98)).thenReturn(decision);
		when(integrator.printDocument(MUNICIPALITY_ID, 3)).thenReturn(document);
		when(integrator.printCalculation(MUNICIPALITY_ID, 31)).thenReturn(calculation);

		assertThat(client.printDecision(98)).isEqualTo(decision);
		assertThat(client.printDocument(3)).isEqualTo(document);
		assertThat(client.printCalculation(31)).isEqualTo(calculation);
	}

	@Test
	void anAnswerThatIsNotAPdfIsABadGateway() {
		when(integrator.printDecision(MUNICIPALITY_ID, 98)).thenReturn("<html>".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> client.printDecision(98))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY);
				assertThat(problem.getDetail()).isEqualTo("Lifecare did not answer with a PDF");
			});
	}

	@Test
	void anEmptyPrintIsABadGateway() {
		when(integrator.printDocument(MUNICIPALITY_ID, 3)).thenReturn(null);
		when(integrator.printCalculation(MUNICIPALITY_ID, 31)).thenReturn("%PD".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> client.printDocument(3))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY));
		assertThatThrownBy(() -> client.printCalculation(31))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY));
	}

	@Test
	void aPrintFromAnIntegratorThatCannotBeReachedIsABadGateway() {
		when(integrator.printCalculation(MUNICIPALITY_ID, 31)).thenThrow(new IllegalStateException("connection reset"));

		assertThatThrownBy(() -> client.printCalculation(31))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus()).isEqualTo(BAD_GATEWAY);
				assertThat(problem.getDetail()).isEqualTo("lifecare-integrator could not be reached (IllegalStateException)");
			});
	}

	@Test
	void aPrintTheIntegratorRefusesPassesItsProblemOn() {
		final var notFound = Problem.valueOf(NOT_FOUND, "Beslutet finns inte");
		when(integrator.printDecision(MUNICIPALITY_ID, 98)).thenThrow(notFound);

		assertThatThrownBy(() -> client.printDecision(98)).isSameAs(notFound);
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
