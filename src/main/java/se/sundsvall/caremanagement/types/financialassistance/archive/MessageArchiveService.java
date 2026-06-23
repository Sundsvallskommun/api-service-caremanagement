package se.sundsvall.caremanagement.types.financialassistance.archive;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.conversation.spi.ConversationThreadQueryService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_CLOSED;

/**
 * Archives the conversation of every closed EB errand. For each errand that has been in {@code CLOSED} (and otherwise
 * untouched) for at least {@code daysAfterClose} days, the thread is rendered into a
 * {@code {errandNumber}_meddelandehistorik.pdf}, uploaded to the errand's Lifecare actualisation, and stored back on
 * the
 * errand as a {@code MESSAGE_HISTORY} attachment — which doubles as the archived-marker, so the job is idempotent: an
 * already-archived errand is skipped on the next run.
 *
 * <p>
 * Each errand is processed in isolation: a failure on one (e.g. a Lifecare hiccup) is logged and the batch continues.
 * Nothing about message content is logged — only errand numbers, counts and ids — since the bodies are citizen
 * correspondence. The Lifecare upload is the irreversible step and runs first; the local marker is written only after
 * it
 * succeeds, so a failed upload is retried on the next run rather than silently skipped.
 */
@Service
public class MessageArchiveService {

	private static final Logger LOG = LoggerFactory.getLogger(MessageArchiveService.class);

	/** {@code Decision} type carrying the Lifecare actualisation id, written when the actualisation is created. */
	private static final String ACTUALISATION_DECISION_TYPE = "ACTUALISATION";
	private static final String PDF_FILE_SUFFIX = "_meddelandehistorik.pdf";

	private final ErrandService errandService;
	private final ConversationThreadQueryService conversationThreadQueryService;
	private final DecisionService decisionService;
	private final AttachmentService attachmentService;
	private final ActualisationService actualisationService;
	private final MessageArchiveProperties properties;

	MessageArchiveService(final ErrandService errandService, final ConversationThreadQueryService conversationThreadQueryService,
		final DecisionService decisionService, final AttachmentService attachmentService, final ActualisationService actualisationService,
		final MessageArchiveProperties properties) {
		this.errandService = errandService;
		this.conversationThreadQueryService = conversationThreadQueryService;
		this.decisionService = decisionService;
		this.attachmentService = attachmentService;
		this.actualisationService = actualisationService;
		this.properties = properties;
	}

	/**
	 * Find every errand that has been closed long enough and archive its conversation. Safe to run repeatedly — already
	 * archived errands are skipped.
	 */
	public void archiveClosedErrands() {
		final var cutoff = now(systemDefault()).minusDays(properties.daysAfterClose());
		final var candidates = errandService.findByStatusTouchedBefore(properties.municipalityId(), properties.namespace(), STATUS_CLOSED, cutoff);

		LOG.info("Message archive: {} closed errand(s) eligible for archiving (closed on or before {})", candidates.size(), cutoff);
		candidates.forEach(this::archiveOne);
	}

	private void archiveOne(final Errand errand) {
		try {
			if (attachmentService.messageHistoryExists(errand.getId())) {
				return;
			}

			final var thread = conversationThreadQueryService.threadForErrand(errand.getId());
			if (thread.isEmpty()) {
				LOG.info("No conversation to archive for errand {}", errand.getErrandNumber());
				return;
			}

			final var actualisationId = resolveActualisationId(errand);
			if (actualisationId.isEmpty()) {
				LOG.warn("Skipping errand {} - no Lifecare actualisation id recorded, cannot upload the meddelandehistorik", errand.getErrandNumber());
				return;
			}

			final var pdf = MeddelandehistorikPdfRenderer.render(errand.getErrandNumber(), thread);
			actualisationService.uploadAttachment(actualisationId.get(), errand.getErrandNumber() + PDF_FILE_SUFFIX, pdf,
				properties.lifecareDocumentType(), properties.lifecareDocumentSenderType(), properties.lifecareTitle(), properties.lifecareSenderName());

			attachmentService.createMessageHistoryAttachment(errand.getMunicipalityId(), errand.getNamespace(), errand.getId(), pdf);

			LOG.info("Archived meddelandehistorik for errand {} ({} message(s)) to Lifecare actualisation {}", errand.getErrandNumber(), thread.size(), actualisationId.get());
		} catch (final Exception e) {
			LOG.error("Failed to archive meddelandehistorik for errand {}: {}", errand.getErrandNumber(), e.getMessage(), e);
		}
	}

	/** The Lifecare actualisation id recorded on the errand, parsed from the most recent {@code ACTUALISATION} decision. */
	private Optional<Integer> resolveActualisationId(final Errand errand) {
		return decisionService.readAll(errand.getMunicipalityId(), errand.getNamespace(), errand.getId()).stream()
			.filter(decision -> ACTUALISATION_DECISION_TYPE.equals(decision.getDecisionType()))
			.map(Decision::getValue)
			.filter(value -> hasText(value))
			.map(MessageArchiveService::parseId)
			.flatMap(Optional::stream)
			.findFirst();
	}

	private static Optional<Integer> parseId(final String value) {
		try {
			return Optional.of(Integer.valueOf(value.trim()));
		} catch (final NumberFormatException e) {
			return Optional.empty();
		}
	}
}
