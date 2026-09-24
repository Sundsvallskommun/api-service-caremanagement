package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareCalculation;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.LifecareDocument;
import se.sundsvall.caremanagement.types.financialassistance.service.mapper.LifecareHistoryMapper;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * The applicant's Lifecare case-history reads — calculations, decisions and documents (metadata + content) served
 * straight from Lifecare. caremanagement only forwards; the applicant is identified by partyId, which the Lifecare
 * route takes as-is. Document content is gated on ownership so a caller cannot read another
 * applicant's document by its Lifecare-global id.
 */
@Service
@Transactional(readOnly = true)
public class FinancialAssistanceLifecareService {

	/** How far back the listing reaches when the caller gives no explicit {@code from} date. */
	private static final int ACTUALISATION_LOOKBACK_MONTHS = 24;
	private static final String DOCUMENT_NOT_FOUND_MESSAGE = "No Lifecare document '%s' found for the given applicant";

	private final LifecareCaseHistoryService lifecareCaseHistoryService;
	private final LifecareDecisionFilter lifecareDecisionFilter;

	FinancialAssistanceLifecareService(final LifecareCaseHistoryService lifecareCaseHistoryService,
		final LifecareDecisionFilter lifecareDecisionFilter) {
		this.lifecareCaseHistoryService = lifecareCaseHistoryService;
		this.lifecareDecisionFilter = lifecareDecisionFilter;
	}

	/**
	 * List the applicant's Lifecare calculations — the full case-history read the frontend renders
	 * straight from Lifecare. The applicant is identified by partyId. The period defaults to the last
	 * {@value #ACTUALISATION_LOOKBACK_MONTHS} months up to today when
	 * {@code from}/{@code to} are omitted.
	 */
	public List<LifecareCalculation> listCalculations(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
		final var toDate = ofNullable(to).orElseGet(LocalDate::now);
		final var fromDate = ofNullable(from).orElseGet(() -> toDate.minusMonths(ACTUALISATION_LOOKBACK_MONTHS));

		return lifecareCaseHistoryService.listCalculations(municipalityId, partyId, fromDate, toDate).stream()
			.map(LifecareHistoryMapper::toCalculation)
			.toList();
	}

	/**
	 * List the applicant's ekonomiskt bistånd decisions in Lifecare — served straight from Lifecare, less the decisions
	 * from other IFO areas (Vux, BoU, LVM …) that FamilyCare returns alongside them (see
	 * {@link LifecareDecisionFilter#isFinancialAssistance}). The EB utredningsbeslut stay in. The applicant is
	 * identified by partyId. The period defaults
	 * to the last {@value #ACTUALISATION_LOOKBACK_MONTHS} months up to today when {@code from}/{@code to} are omitted.
	 */
	public List<LifecareDecision> listDecisions(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
		final var toDate = ofNullable(to).orElseGet(LocalDate::now);
		final var fromDate = ofNullable(from).orElseGet(() -> toDate.minusMonths(ACTUALISATION_LOOKBACK_MONTHS));

		return lifecareCaseHistoryService.listDecisions(municipalityId, partyId, fromDate, toDate).stream()
			.filter(lifecareDecisionFilter::isFinancialAssistance)
			.map(LifecareHistoryMapper::toDecision)
			.toList();
	}

	/**
	 * List the applicant's Lifecare documents (metadata) — served straight from Lifecare. The applicant is identified by
	 * partyId. The period defaults to the last
	 * {@value #ACTUALISATION_LOOKBACK_MONTHS} months up to today when {@code from}/{@code to} are omitted. The content of a
	 * single document is fetched via {@link #readDocumentContent(String, String, String, LocalDate, LocalDate)}.
	 */
	public List<LifecareDocument> listDocuments(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
		return documentsFor(municipalityId, partyId, from, to);
	}

	/**
	 * The applicant's Lifecare documents — a non-proxied worker so the in-class ownership check
	 * ({@link #readDocumentContent}) can reuse it without a self-call to the {@code @Transactional} public method (which
	 * would bypass the Spring proxy — Sonar S6809).
	 */
	private List<LifecareDocument> documentsFor(final String municipalityId, final String partyId, final LocalDate from, final LocalDate to) {
		final var toDate = ofNullable(to).orElseGet(LocalDate::now);
		final var fromDate = ofNullable(from).orElseGet(() -> toDate.minusMonths(ACTUALISATION_LOOKBACK_MONTHS));

		return lifecareCaseHistoryService.listDocuments(municipalityId, partyId, fromDate, toDate).stream()
			.map(LifecareHistoryMapper::toDocument)
			.toList();
	}

	/**
	 * Read a single Lifecare document's content (the generated PDF) — the bytes are streamed straight from Lifecare,
	 * caremanagement only forwards them. Gated on ownership: the document id must belong to the given applicant (the same
	 * partyId scope as {@link #listDocuments}), so a caller cannot read another applicant's document by guessing its
	 * Lifecare-global id. A document id not in the applicant's list for the period yields 404 before any bytes are read.
	 */
	public byte[] readDocumentContent(final String municipalityId, final String partyId, final String documentId, final LocalDate from, final LocalDate to) {
		final var owned = documentsFor(municipalityId, partyId, from, to).stream()
			.anyMatch(document -> documentId.equals(document.getId()));
		if (!owned) {
			throw Problem.valueOf(NOT_FOUND, DOCUMENT_NOT_FOUND_MESSAGE.formatted(documentId));
		}
		return lifecareCaseHistoryService.documentContent(municipalityId, documentId);
	}
}
