package se.sundsvall.caremanagement.lifecare.professionalweb;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.lifecare.professionalweb.integrator.ProfessionalWebIntegratorClient;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.MissingNode;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Transport for Lifecare ProfessionalWeb's api2 endpoints.
 *
 * <p>
 * Callers deal in JSON trees and never in cookies. Trees rather than typed objects on purpose: every write to
 * ProfessionalWeb is the object Lifecare handed out (a proposal, or the current record) with a few fields changed, and
 * a typed round trip would silently drop every field the type does not know about.
 * </p>
 *
 * <p>
 * How the call reaches Lifecare, session escalation included, is the {@link ProfessionalWebTransport}'s business: it
 * goes through api-service-lifecare-integrator. Nothing is retried here.
 * </p>
 *
 * <p>
 * Nothing that crosses this client is logged beyond the path and the status: answers carry personnummer, and request
 * bodies carry whatever the caseworker wrote.
 * </p>
 */
@Component
public class ProfessionalWebClient {

	/** The one Lifecare module careM talks to. */
	public static final String MODULE = "WESE.FC.ProfessionalWeb";

	private static final Logger LOG = LoggerFactory.getLogger(ProfessionalWebClient.class);
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String DELETE = "DELETE";
	private static final byte[] PDF_SIGNATURE = "%PDF".getBytes(StandardCharsets.US_ASCII);
	static final String ERROR_NOT_A_PDF = "Lifecare did not answer with a PDF";
	static final String ERROR_ASKED_AGAIN = "Lifecare ställde en parameterfråga som careM inte kunde besvara";

	/** The integrator's path takes a municipality; there is one Lifecare, Sundsvall's. */
	private static final String MUNICIPALITY_ID = "2281";

	private final ProfessionalWebTransport transport;
	private final ProfessionalWebIntegratorClient integrator;
	private final JsonMapper json = JsonMapper.builder().build();

	public ProfessionalWebClient(final ProfessionalWebTransport transport, final ProfessionalWebIntegratorClient integrator) {
		this.transport = transport;
		this.integrator = integrator;
	}

	/** The bevakningar on an insats. */
	public JsonNode listReminders(final int serviceId) {
		return read(() -> integrator.listReminders(MUNICIPALITY_ID, serviceId));
	}

	/** The blank bevakning and the options for a new one on an insats. */
	public JsonNode readReminderProposal(final int serviceId) {
		return read(() -> integrator.readReminderProposal(MUNICIPALITY_ID, serviceId));
	}

	/** A bevakning, as Lifecare's editor reads it. */
	public JsonNode readReminder(final int reminderId) {
		return read(() -> integrator.readReminder(MUNICIPALITY_ID, reminderId));
	}

	/** Creates a bevakning. Not idempotent. */
	public JsonNode createReminder(final JsonNode body) {
		return read(() -> integrator.createReminder(MUNICIPALITY_ID, body));
	}

	/** Saves a changed bevakning; the body's reminderId is the one given. */
	public void updateReminder(final int reminderId, final JsonNode body) {
		write(() -> integrator.updateReminder(MUNICIPALITY_ID, reminderId, body));
	}

	/** Removes a bevakning. */
	public void removeReminder(final int reminderId) {
		write(() -> integrator.removeReminder(MUNICIPALITY_ID, reminderId));
	}

	/** The sökandes and medsökandes jobbstimulans periods on an insats. */
	public JsonNode readJobStimulus(final int serviceId) {
		return read(() -> integrator.readJobStimulus(MUNICIPALITY_ID, serviceId));
	}

	/** The end Lifecare gives a jobbstimulans period starting on the date, a bare JSON string. */
	public JsonNode readJobStimulusEndDate(final String fromDate) {
		return read(() -> integrator.readJobStimulusEndDate(MUNICIPALITY_ID, fromDate));
	}

	/** Saves a person's jobbstimulans periods; a period left out is deleted. */
	public JsonNode saveJobStimulus(final JsonNode body) {
		return read(() -> integrator.saveJobStimulus(MUNICIPALITY_ID, body));
	}

	/**
	 * The underlag for a new beslut on an insats; an empty amountType and calculation 0 for one made without a beräkning.
	 */
	public JsonNode readDecisionProposal(final int serviceId, final String amountType, final int calculationId) {
		return read(() -> integrator.readDecisionProposal(MUNICIPALITY_ID, serviceId, amountType, calculationId));
	}

	/** The orsak catalogue of a beslutstyp. */
	public JsonNode readDecisionReasons(final int decisionCode) {
		return read(() -> integrator.readDecisionReasons(MUNICIPALITY_ID, decisionCode));
	}

	/** A beslut, whole: the object an update takes back. */
	public JsonNode readDecision(final int decisionId) {
		return read(() -> integrator.readDecision(MUNICIPALITY_ID, decisionId));
	}

	/** Registers a beslut on an insats. Not idempotent. */
	public JsonNode createDecision(final int serviceId, final JsonNode body) {
		return read(() -> integrator.createDecision(MUNICIPALITY_ID, serviceId, body));
	}

	/** Saves a changed beslut. */
	public JsonNode updateDecision(final int decisionId, final JsonNode body) {
		return read(() -> integrator.updateDecision(MUNICIPALITY_ID, decisionId, body));
	}

	/** The underlag for a new utbetalning on an insats: blank payment, payees, betalsätt, saldon and months. */
	public JsonNode readPaymentProposal(final int serviceId) {
		return read(() -> integrator.readPaymentProposal(MUNICIPALITY_ID, serviceId));
	}

	/** The latest utbetalningar registered on an insats. */
	public JsonNode readLatestPayments(final int serviceId) {
		return read(() -> integrator.readLatestPayments(MUNICIPALITY_ID, serviceId));
	}

	/** Whether the person of the insats has a hushåll on the date (ISO), as a JSON boolean. */
	public JsonNode hasHousehold(final int serviceId, final String date) {
		return read(() -> integrator.hasHousehold(MUNICIPALITY_ID, serviceId, date));
	}

	/** Registers an utbetalning on an insats. Not idempotent and it moves money: never retried. */
	public JsonNode createPayment(final int serviceId, final JsonNode body) {
		return read(() -> integrator.createPayment(MUNICIPALITY_ID, serviceId, body));
	}

	/** Creates a betalningsmottagare. Not idempotent. */
	public JsonNode createPayee(final JsonNode body) {
		return read(() -> integrator.createPayee(MUNICIPALITY_ID, body));
	}

	/** The underlag for a new beräkning on an insats: a blank one with the household, and the catalogues. */
	public JsonNode readCalculationProposal(final int serviceId) {
		return read(() -> integrator.readCalculationProposal(MUNICIPALITY_ID, serviceId));
	}

	/** Every beräkning on an insats, without rows, as an array. */
	public JsonNode listLatestCalculations(final int serviceId) {
		return read(() -> integrator.listLatestCalculations(MUNICIPALITY_ID, serviceId));
	}

	/** A saved beräkning as Lifecare shows it. */
	public JsonNode readCalculation(final int calculationId) {
		return read(() -> integrator.readCalculation(MUNICIPALITY_ID, calculationId));
	}

	/** A saved beräkning whole, what an update takes back, with the catalogues. */
	public JsonNode readCalculationForEdit(final int calculationId) {
		return read(() -> integrator.readCalculationForEdit(MUNICIPALITY_ID, calculationId));
	}

	/** Places the members on the norm's rows. Changes nothing in Lifecare. */
	public JsonNode placePersons(final JsonNode body) {
		return read(() -> integrator.placePersons(MUNICIPALITY_ID, body));
	}

	/** A member's amount for a period. Changes nothing in Lifecare. */
	public JsonNode readAmount(final JsonNode body) {
		return read(() -> integrator.readAmount(MUNICIPALITY_ID, body));
	}

	/** The gemensamma kostnader for a period, a bare number. Changes nothing in Lifecare. */
	public JsonNode readSharedCost(final JsonNode body) {
		return read(() -> integrator.readSharedCost(MUNICIPALITY_ID, body));
	}

	/** Marks which members have jobbstimulans during a beräkning's period. Changes nothing in Lifecare. */
	public JsonNode readJobStimuliForCalculation(final JsonNode body) {
		return read(() -> integrator.readJobStimuliForCalculation(MUNICIPALITY_ID, body));
	}

	/** Creates a beräkning on an insats. Not idempotent. */
	public JsonNode createCalculation(final int serviceId, final JsonNode body) {
		return read(() -> integrator.createCalculation(MUNICIPALITY_ID, serviceId, body));
	}

	/** Saves a changed beräkning; Lifecare answers with it recounted. */
	public JsonNode updateCalculation(final int calculationId, final JsonNode body) {
		return read(() -> integrator.updateCalculation(MUNICIPALITY_ID, calculationId, body));
	}

	/** Removes a beräkning. */
	public void deleteCalculation(final int calculationId) {
		write(() -> integrator.deleteCalculation(MUNICIPALITY_ID, calculationId));
	}

	/** The journalanteckningar and documents in a person's record. */
	public JsonNode listDocuments(final String partyId) {
		return read(() -> integrator.listDocuments(MUNICIPALITY_ID, partyId));
	}

	/** A journalanteckning with its content, in view mode. */
	public JsonNode readJournalNote(final int id) {
		return read(() -> integrator.readJournalNote(MUNICIPALITY_ID, id));
	}

	/** A journalanteckning as Lifecare's editor reads it, the object an update takes back. */
	public JsonNode readJournalNoteForEdit(final int id) {
		return read(() -> integrator.readJournalNoteForEdit(MUNICIPALITY_ID, id));
	}

	/** A document with its content, in view mode. */
	public JsonNode readDocument(final int id) {
		return read(() -> integrator.readDocument(MUNICIPALITY_ID, id));
	}

	/** A document as Lifecare's editor reads it, the object an update takes back. */
	public JsonNode readDocumentForEdit(final int id) {
		return read(() -> integrator.readDocumentForEdit(MUNICIPALITY_ID, id));
	}

	/** The blank journalanteckning and its note types on an insats. */
	public JsonNode readJournalNoteProposal(final int serviceId) {
		return read(() -> integrator.readJournalNoteProposal(MUNICIPALITY_ID, serviceId));
	}

	/** The blank document and its document types on an insats. */
	public JsonNode readDocumentProposal(final int serviceId) {
		return read(() -> integrator.readDocumentProposal(MUNICIPALITY_ID, serviceId));
	}

	/** Writes a new journalanteckning. Not idempotent. */
	public JsonNode createJournalNote(final JsonNode body) {
		return read(() -> integrator.createJournalNote(MUNICIPALITY_ID, body));
	}

	/** Writes a new document. Not idempotent. */
	public JsonNode createDocument(final JsonNode body) {
		return read(() -> integrator.createDocument(MUNICIPALITY_ID, body));
	}

	/** Saves a changed journalanteckning. */
	public JsonNode updateJournalNote(final int id, final JsonNode body) {
		return read(() -> integrator.updateJournalNote(MUNICIPALITY_ID, id, body));
	}

	/** Saves a changed document. */
	public JsonNode updateDocument(final int id, final JsonNode body) {
		return read(() -> integrator.updateDocument(MUNICIPALITY_ID, id, body));
	}

	/** An answer from the integrator; a missing node when it answered without a body. */
	private static JsonNode read(final Supplier<JsonNode> call) {
		return Optional.ofNullable(ProfessionalWebErrors.call(call)).orElse(MissingNode.getInstance());
	}

	private static void write(final Runnable call) {
		ProfessionalWebErrors.call(() -> {
			call.run();
			return null;
		});
	}

	/**
	 * Reads from api2.
	 *
	 * @param  path   the path below the module, e.g. {@code api2/Calculation/GetCalculation}
	 * @param  params query parameters, in order
	 * @return        the answer, a missing node when Lifecare answered with an empty body
	 */
	public JsonNode get(final String path, final Map<String, String> params) {
		return toJson(exchange(GET, path, params, null));
	}

	/**
	 * Writes to (or asks a computation of) api2 with a JSON body.
	 *
	 * @param  path   the path below the module
	 * @param  params query parameters, in order
	 * @param  body   the request body, serialised as JSON
	 * @return        the answer, a missing node when Lifecare answered with an empty body
	 */
	public JsonNode post(final String path, final Map<String, String> params, final Object body) {
		return toJson(exchange(POST, path, params, body));
	}

	/**
	 * Removes something. Lifecare's own client sends the id in a JSON body rather than the URL.
	 *
	 * @param  path the path below the module
	 * @param  body the request body
	 * @return      the answer, a missing node when Lifecare answered with an empty body
	 */
	public JsonNode delete(final String path, final Object body) {
		return delete(path, Map.of(), body);
	}

	public JsonNode delete(final String path, final Map<String, String> params, final Object body) {
		return toJson(exchange(DELETE, path, params, body));
	}

	/**
	 * Reads a file Lifecare renders, such as a decision as PDF. When the template asks parameter questions first, as
	 * "Beslut" does for a beslut with a medsökande, the questions are answered with Lifecare's own defaults, as a
	 * caseworker pressing Klar would.
	 *
	 * @param  path   the path below the module
	 * @param  params query parameters, in order
	 * @return        the PDF bytes
	 */
	public byte[] getPdf(final String path, final Map<String, String> params) {
		final var answer = exchange(GET, path, params, null);
		if (isPdf(answer)) {
			return answer.body();
		}
		// Some templates ask parameter questions first (Beslut: "Visa medsökande"); the form is posted back as it came.
		final var fields = ParameterQueryForm.fields(answer).orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, ERROR_NOT_A_PDF));
		LOG.info("Lifecare asked {} parameter field(s) before rendering {} - answering with its defaults", fields.size(), path);
		final var rendered = transport.submitForm(path, params, fields);
		if (!rendered.isSuccess()) {
			LOG.info("Lifecare refused the parameters for {} ({})", path, rendered.describe());
			throw ProfessionalWebErrors.toProblem(rendered);
		}
		if (isPdf(rendered)) {
			return rendered.body();
		}
		if (ParameterQueryForm.isParameterQuery(rendered)) {
			throw Problem.valueOf(BAD_GATEWAY, ERROR_ASKED_AGAIN);
		}
		throw Problem.valueOf(BAD_GATEWAY, ERROR_NOT_A_PDF);
	}

	private static boolean isPdf(final ProfessionalWebResponse response) {
		return startsWith(response.body(), PDF_SIGNATURE);
	}

	private ProfessionalWebResponse exchange(final String method, final String path, final Map<String, String> params, final Object body) {
		byte[] bytes = null;
		if (!GET.equals(method)) {
			bytes = serialise(body);
		}
		final var response = transport.exchange(method, path, params, bytes);
		if (!response.isSuccess()) {
			LOG.info("Lifecare refused {} {} ({})", method, path, response.describe());
			throw ProfessionalWebErrors.toProblem(response);
		}
		return response;
	}

	private byte[] serialise(final Object body) {
		if (body == null) {
			return new byte[0];
		}
		return json.writeValueAsBytes(body);
	}

	private JsonNode toJson(final ProfessionalWebResponse response) {
		if (response.body().length == 0) {
			return MissingNode.getInstance();
		}
		try {
			return json.readTree(response.body());
		} catch (final JacksonException _) {
			throw Problem.valueOf(BAD_GATEWAY, "Lifecare answered with something that is not JSON (" + response.describe() + ")");
		}
	}

	private static boolean startsWith(final byte[] body, final byte[] prefix) {
		return body.length >= prefix.length && Arrays.equals(body, 0, prefix.length, prefix, 0, prefix.length);
	}
}
