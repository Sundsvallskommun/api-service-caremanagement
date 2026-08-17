package se.sundsvall.caremanagement.types.financialassistance.archive;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.attachments.service.SourceFile;
import se.sundsvall.caremanagement.conversation.spi.ConversationMessageView;
import se.sundsvall.caremanagement.conversation.spi.ConversationThreadQueryService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.service.DecisionService;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_CLOSED;

/**
 * Archives the conversation of every closed financial assistance errand into a single PDF — the rendered message pages
 * first, then each
 * conversation attachment appended in full, introduced by a numbered {@code Bilaga {n}} divider so two
 * identically-named
 * files can be told apart. The PDF is named
 * {@code {label}_{errandNumber}_{firstMessageDate}--{lastMessageDate}.pdf}, uploaded to the errand's Lifecare
 * actualisation, and stored back on the errand as a {@code MESSAGE_HISTORY} attachment — which doubles as the
 * archived-marker, so the job is idempotent: an already-archived errand is skipped on the next run.
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
	private static final String PDF_MIME_TYPE = "application/pdf";
	private static final String PDF_EXTENSION = ".pdf";
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

			if (!ThreadAttachments.hasApplicantMessage(thread)) {
				LOG.info("No applicant message in the conversation for errand {} - nothing to archive", errand.getErrandNumber());
				return;
			}

			final var actualisationId = resolveActualisationId(errand);
			if (actualisationId.isEmpty()) {
				LOG.warn("Skipping errand {} - no Lifecare actualisation id recorded, cannot upload the message history", errand.getErrandNumber());
				return;
			}

			final var pdf = assemble(errand.getErrandNumber(), thread);
			final var fileName = fileName(errand.getErrandNumber(), thread);
			final var title = fileName.substring(0, fileName.length() - PDF_EXTENSION.length());

			// Record the local message-history marker FIRST — it is the idempotency guard checked at the top of this
			// method. Ordering it before the (non-idempotent) Lifecare upload means: if the marker write fails, nothing
			// has been pushed to Lifecare yet and the next run retries cleanly; if the Lifecare upload fails, the marker
			// is already set so the job won't re-run and upload a duplicate Lifecare document. Nothing after the upload
			// throws, so a "Failed to archive" log can no longer coincide with a document actually created in Lifecare.
			attachmentService.createMessageHistoryAttachment(errand.getMunicipalityId(), errand.getNamespace(), errand.getId(), fileName, pdf);

			actualisationService.uploadAttachment(actualisationId.get(), fileName, pdf,
				properties.lifecareDocumentType(), properties.lifecareDocumentSenderType(), title, properties.lifecareSenderName());

			LOG.info("Archived message history for errand {} ({} message(s)) to Lifecare actualisation {}", errand.getErrandNumber(), thread.size(), actualisationId.get());
		} catch (final Exception e) {
			LOG.error("Failed to archive message history for errand {}: {}", errand.getErrandNumber(), e.getMessage(), e);
		}
	}

	/** Render the messages section, then append each numbered attachment behind a divider, into a single PDF. */
	private byte[] assemble(final String errandNumber, final List<ConversationMessageView> thread) {
		final var attachments = ThreadAttachments.flatten(thread);

		final var sources = new ArrayList<SourceFile>();
		sources.add(new SourceFile("meddelanden.pdf", PDF_MIME_TYPE, MessageHistoryPdfRenderer.renderMessages(errandNumber, thread, attachments)));
		attachments.forEach(attachment -> {
			sources.add(new SourceFile("bilaga-%d-rubrik.pdf".formatted(attachment.number()), PDF_MIME_TYPE, MessageHistoryPdfRenderer.renderSeparator(attachment)));
			sources.add(new SourceFile(attachment.fileName(), attachment.mimeType(), attachment.content()));
		});

		return attachmentService.combineToPdf(sources);
	}

	/** {@code {label}_{errandNumber}_{firstMessageDate}--{lastMessageDate}.pdf}. */
	private String fileName(final String errandNumber, final List<ConversationMessageView> thread) {
		final var first = date(thread.getFirst().created());
		final var last = date(thread.getLast().created());
		return "%s_%s_%s--%s%s".formatted(properties.documentLabel(), errandNumber, first, last, PDF_EXTENSION);
	}

	private static String date(final OffsetDateTime timestamp) {
		return Optional.ofNullable(timestamp).map(value -> value.atZoneSameInstant(ZoneId.systemDefault()).format(DATE)).orElse("");
	}

	/** The Lifecare actualisation id recorded on the errand, parsed from the most recent {@code ACTUALISATION} decision. */
	private Optional<Integer> resolveActualisationId(final Errand errand) {
		return decisionService.readAll(errand.getMunicipalityId(), errand.getNamespace(), errand.getId()).stream()
			.filter(decision -> ACTUALISATION_DECISION_TYPE.equals(decision.getDecisionType()))
			.map(Decision::getValue)
			.filter(StringUtils::hasText)
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
