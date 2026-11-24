package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.integrator.ProfessionalWebIntegratorClient;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Lifecare ProfessionalWeb through lifecare-integrator's resources. JSON trees, not typed objects: a write sends back
 * Lifecare's own object, and a typed round trip would drop the fields no type knows. Nothing is retried or logged.
 */
@Component
public class ProfessionalWebClient {

	private static final byte[] PDF_SIGNATURE = "%PDF".getBytes(StandardCharsets.US_ASCII);
	static final String ERROR_NOT_A_PDF = "Lifecare did not answer with a PDF";

	/** The integrator's path takes a municipality; there is one Lifecare, Sundsvall's. */
	private static final String MUNICIPALITY_ID = "2281";

	private final ProfessionalWebIntegratorClient integrator;

	public ProfessionalWebClient(final ProfessionalWebIntegratorClient integrator) {
		this.integrator = integrator;
	}

	public JsonNode listReminders(final int serviceId) {
		return callForJson(() -> integrator.listReminders(MUNICIPALITY_ID, serviceId));
	}

	public JsonNode readReminderProposal(final int serviceId) {
		return callForJson(() -> integrator.readReminderProposal(MUNICIPALITY_ID, serviceId));
	}

	public JsonNode readReminder(final int reminderId) {
		return callForJson(() -> integrator.readReminder(MUNICIPALITY_ID, reminderId));
	}

	/** Creates a bevakning. Not idempotent. */
	public JsonNode createReminder(final JsonNode body) {
		return callForJson(() -> integrator.createReminder(MUNICIPALITY_ID, body));
	}

	/** Saves a changed bevakning; the body's reminderId is the one given. */
	public void updateReminder(final int reminderId, final JsonNode body) {
		callWithoutAnswer(() -> integrator.updateReminder(MUNICIPALITY_ID, reminderId, body));
	}

	public void removeReminder(final int reminderId) {
		callWithoutAnswer(() -> integrator.removeReminder(MUNICIPALITY_ID, reminderId));
	}

	public JsonNode readJobStimulus(final int serviceId) {
		return callForJson(() -> integrator.readJobStimulus(MUNICIPALITY_ID, serviceId));
	}

	/** The end Lifecare gives a jobbstimulans period starting on the date, a bare JSON string. */
	public JsonNode readJobStimulusEndDate(final String fromDate) {
		return callForJson(() -> integrator.readJobStimulusEndDate(MUNICIPALITY_ID, fromDate));
	}

	/** Saves a person's jobbstimulans periods; a period left out is deleted. */
	public JsonNode saveJobStimulus(final JsonNode body) {
		return callForJson(() -> integrator.saveJobStimulus(MUNICIPALITY_ID, body));
	}

	/**
	 * The underlag for a new beslut on an insats; an empty amountType and calculation 0 for one made without a beräkning.
	 */
	public JsonNode readDecisionProposal(final int serviceId, final String amountType, final int calculationId) {
		return callForJson(() -> integrator.readDecisionProposal(MUNICIPALITY_ID, serviceId, amountType, calculationId));
	}

	public JsonNode readDecisionReasons(final int decisionCode) {
		return callForJson(() -> integrator.readDecisionReasons(MUNICIPALITY_ID, decisionCode));
	}

	/** A beslut, whole: the object an update takes back. */
	public JsonNode readDecision(final int decisionId) {
		return callForJson(() -> integrator.readDecision(MUNICIPALITY_ID, decisionId));
	}

	/** Registers a beslut on an insats. Not idempotent. */
	public JsonNode createDecision(final int serviceId, final JsonNode body) {
		return callForJson(() -> integrator.createDecision(MUNICIPALITY_ID, serviceId, body));
	}

	public JsonNode updateDecision(final int decisionId, final JsonNode body) {
		return callForJson(() -> integrator.updateDecision(MUNICIPALITY_ID, decisionId, body));
	}

	public JsonNode readPaymentProposal(final int serviceId) {
		return callForJson(() -> integrator.readPaymentProposal(MUNICIPALITY_ID, serviceId));
	}

	public JsonNode readLatestPayments(final int serviceId) {
		return callForJson(() -> integrator.readLatestPayments(MUNICIPALITY_ID, serviceId));
	}

	/** Whether the person of the insats has a hushåll on the date (ISO), as a JSON boolean. */
	public JsonNode hasHousehold(final int serviceId, final String date) {
		return callForJson(() -> integrator.hasHousehold(MUNICIPALITY_ID, serviceId, date));
	}

	/** Registers an utbetalning on an insats. Not idempotent and it moves money: never retried. */
	public JsonNode createPayment(final int serviceId, final JsonNode body) {
		return callForJson(() -> integrator.createPayment(MUNICIPALITY_ID, serviceId, body));
	}

	/** Creates a betalningsmottagare. Not idempotent. */
	public JsonNode createPayee(final JsonNode body) {
		return callForJson(() -> integrator.createPayee(MUNICIPALITY_ID, body));
	}

	public JsonNode readCalculationProposal(final int serviceId) {
		return callForJson(() -> integrator.readCalculationProposal(MUNICIPALITY_ID, serviceId));
	}

	/** Every beräkning on an insats, without rows, as an array. */
	public JsonNode listLatestCalculations(final int serviceId) {
		return callForJson(() -> integrator.listLatestCalculations(MUNICIPALITY_ID, serviceId));
	}

	public JsonNode readCalculation(final int calculationId) {
		return callForJson(() -> integrator.readCalculation(MUNICIPALITY_ID, calculationId));
	}

	/** A saved beräkning whole, what an update takes back, with the catalogues. */
	public JsonNode readCalculationForEdit(final int calculationId) {
		return callForJson(() -> integrator.readCalculationForEdit(MUNICIPALITY_ID, calculationId));
	}

	/** Places the members on the norm's rows. Changes nothing in Lifecare. */
	public JsonNode placePersons(final JsonNode body) {
		return callForJson(() -> integrator.placePersons(MUNICIPALITY_ID, body));
	}

	/** A member's amount for a period. Changes nothing in Lifecare. */
	public JsonNode readAmount(final JsonNode body) {
		return callForJson(() -> integrator.readAmount(MUNICIPALITY_ID, body));
	}

	/** The gemensamma kostnader for a period, a bare number. Changes nothing in Lifecare. */
	public JsonNode readSharedCost(final JsonNode body) {
		return callForJson(() -> integrator.readSharedCost(MUNICIPALITY_ID, body));
	}

	/** Marks which members have jobbstimulans during a beräkning's period. Changes nothing in Lifecare. */
	public JsonNode readJobStimuliForCalculation(final JsonNode body) {
		return callForJson(() -> integrator.readJobStimuliForCalculation(MUNICIPALITY_ID, body));
	}

	/** Creates a beräkning on an insats. Not idempotent. */
	public JsonNode createCalculation(final int serviceId, final JsonNode body) {
		return callForJson(() -> integrator.createCalculation(MUNICIPALITY_ID, serviceId, body));
	}

	/** Saves a changed beräkning; Lifecare answers with it recounted. */
	public JsonNode updateCalculation(final int calculationId, final JsonNode body) {
		return callForJson(() -> integrator.updateCalculation(MUNICIPALITY_ID, calculationId, body));
	}

	public void deleteCalculation(final int calculationId) {
		callWithoutAnswer(() -> integrator.deleteCalculation(MUNICIPALITY_ID, calculationId));
	}

	public JsonNode listDocuments(final String partyId) {
		return callForJson(() -> integrator.listDocuments(MUNICIPALITY_ID, partyId));
	}

	/** A journalanteckning with its content, in view mode. */
	public JsonNode readJournalNote(final int id) {
		return callForJson(() -> integrator.readJournalNote(MUNICIPALITY_ID, id));
	}

	/** A journalanteckning as Lifecare's editor reads it, the object an update takes back. */
	public JsonNode readJournalNoteForEdit(final int id) {
		return callForJson(() -> integrator.readJournalNoteForEdit(MUNICIPALITY_ID, id));
	}

	/** A document with its content, in view mode. */
	public JsonNode readDocument(final int id) {
		return callForJson(() -> integrator.readDocument(MUNICIPALITY_ID, id));
	}

	/** A document as Lifecare's editor reads it, the object an update takes back. */
	public JsonNode readDocumentForEdit(final int id) {
		return callForJson(() -> integrator.readDocumentForEdit(MUNICIPALITY_ID, id));
	}

	public JsonNode readJournalNoteProposal(final int serviceId) {
		return callForJson(() -> integrator.readJournalNoteProposal(MUNICIPALITY_ID, serviceId));
	}

	public JsonNode readDocumentProposal(final int serviceId) {
		return callForJson(() -> integrator.readDocumentProposal(MUNICIPALITY_ID, serviceId));
	}

	/** Writes a new journalanteckning. Not idempotent. */
	public JsonNode createJournalNote(final JsonNode body) {
		return callForJson(() -> integrator.createJournalNote(MUNICIPALITY_ID, body));
	}

	/** Writes a new document. Not idempotent. */
	public JsonNode createDocument(final JsonNode body) {
		return callForJson(() -> integrator.createDocument(MUNICIPALITY_ID, body));
	}

	public JsonNode updateJournalNote(final int id, final JsonNode body) {
		return callForJson(() -> integrator.updateJournalNote(MUNICIPALITY_ID, id, body));
	}

	public JsonNode updateDocument(final int id, final JsonNode body) {
		return callForJson(() -> integrator.updateDocument(MUNICIPALITY_ID, id, body));
	}

	public byte[] printDecision(final int decisionId) {
		return callForPdf(() -> integrator.printDecision(MUNICIPALITY_ID, decisionId));
	}

	public byte[] printDocument(final int id) {
		return callForPdf(() -> integrator.printDocument(MUNICIPALITY_ID, id));
	}

	public byte[] printCalculation(final int calculationId) {
		return callForPdf(() -> integrator.printCalculation(MUNICIPALITY_ID, calculationId));
	}

	private static byte[] callForPdf(final Supplier<byte[]> call) {
		return Optional.ofNullable(ProfessionalWebErrors.call(call))
			.filter(body -> startsWith(body, PDF_SIGNATURE))
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_NOT_A_PDF));
	}

	private static JsonNode callForJson(final Supplier<JsonNode> call) {
		return Optional.ofNullable(ProfessionalWebErrors.call(call)).orElse(MissingNode.getInstance());
	}

	private static void callWithoutAnswer(final Runnable call) {
		ProfessionalWebErrors.call(() -> {
			call.run();
			return null;
		});
	}

	private static boolean startsWith(final byte[] body, final byte[] prefix) {
		return body.length >= prefix.length && Arrays.equals(body, 0, prefix.length, prefix, 0, prefix.length);
	}
}
