package se.sundsvall.caremanagement.attachments.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.api.model.Attachment;
import se.sundsvall.caremanagement.attachments.integration.db.AttachmentRepository;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.attachments.service.mapper.AttachmentMapper;
import se.sundsvall.caremanagement.conversation.spi.ConversationAttachmentQueryService;
import se.sundsvall.caremanagement.conversation.spi.Direction;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.nullsLast;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.caremanagement.attachments.service.mapper.AttachmentMapper.toAttachment;
import static se.sundsvall.caremanagement.attachments.service.mapper.AttachmentMapper.toAttachmentEntity;
import static se.sundsvall.caremanagement.attachments.service.mapper.AttachmentMapper.toAttachmentList;

@Service
@Transactional
public class AttachmentService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String ATTACHMENT_NOT_FOUND_MESSAGE = "No attachment with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";
	private static final String CASE_DATA_ALREADY_EXISTS_MESSAGE = "A case-data (ärendeuppgifter) attachment already exists on errand '%s' in namespace '%s' for municipality id '%s'";
	private static final String STREAM_ERROR_MESSAGE = "%s occurred when copying file with attachment id '%s' to response: %s";
	private static final String READ_ERROR_MESSAGE = "Could not read input stream: %s";

	/** Filename of the create-time combined PDF that merges the citizen's application files. */
	private static final String COMBINED_PDF_FILE_NAME = "sammanstallning.pdf";
	/** Filename of the living consolidation of the client's conversation attachments (regenerated in place). */
	private static final String CLIENT_PDF_FILE_NAME = "klientbilagor.pdf";

	/** Attachment documentType facet — see the {@code Attachment} schema. */
	private static final String DOCUMENT_TYPE_APPLICATION = "APPLICATION";
	private static final String DOCUMENT_TYPE_CONVERSATION = "CONVERSATION";
	private static final String DOCUMENT_TYPE_GENERATED = "GENERATED";
	private static final String DOCUMENT_TYPE_ERRAND = "ERRAND";
	private static final String DOCUMENT_TYPE_CASE_DATA = "CASE_DATA";
	private static final String DOCUMENT_TYPE_MESSAGE_HISTORY = "MESSAGE_HISTORY";
	/** Extension forced on the case-data attachment, whose file is renamed to {errandNumber}.pdf. */
	private static final String CASE_DATA_FILE_EXTENSION = ".pdf";
	private static final String MESSAGE_HISTORY_ALREADY_EXISTS_MESSAGE = "A message-history attachment already exists on errand '%s' in namespace '%s' for municipality id '%s'";
	/** Sender-role facet — the application files and the consolidated client PDF are the applicant's. */
	private static final String SENDER_CLIENT = Direction.INBOUND.role();

	private final ErrandRepository errandRepository;
	private final AttachmentRepository attachmentRepository;
	private final ConversationAttachmentQueryService conversationAttachmentQueryService;

	AttachmentService(final ErrandRepository errandRepository, final AttachmentRepository attachmentRepository,
		final ConversationAttachmentQueryService conversationAttachmentQueryService) {
		this.errandRepository = errandRepository;
		this.attachmentRepository = attachmentRepository;
		this.conversationAttachmentQueryService = conversationAttachmentQueryService;
	}

	public String createAttachment(final String municipalityId, final String namespace, final String errandId, final String documentType, final MultipartFile file) {
		final var errand = getErrand(municipalityId, namespace, errandId);
		final var resolvedDocumentType = ofNullable(documentType).orElse(DOCUMENT_TYPE_ERRAND);

		// A case-data attachment is unique per errand and is renamed to {errandNumber}.pdf. Lock the
		// errand row first so two concurrent CASE_DATA uploads serialize instead of both passing the existence check.
		if (DOCUMENT_TYPE_CASE_DATA.equals(resolvedDocumentType)) {
			lockErrand(municipalityId, namespace, errandId);
			if (attachmentRepository.existsByErrandIdAndDocumentType(errandId, DOCUMENT_TYPE_CASE_DATA)) {
				throw Problem.valueOf(BAD_REQUEST, CASE_DATA_ALREADY_EXISTS_MESSAGE.formatted(errandId, namespace, municipalityId));
			}
		}

		final var entity = toAttachmentEntity(errandId, namespace, municipalityId, resolvedDocumentType, null, file);
		if (DOCUMENT_TYPE_CASE_DATA.equals(resolvedDocumentType)) {
			entity.setFileName(errand.getErrandNumber() + CASE_DATA_FILE_EXTENSION);
		}

		return attachmentRepository.save(entity).getId();
	}

	/**
	 * Store the case-data snapshot for the errand — a convenience over {@link #createAttachment} that
	 * pins documentType {@code CASE_DATA}, so the file is renamed to {@code {errandNumber}.pdf} and the one-per-errand rule
	 * applies. Used by the financial assistance create bundle so the whole errand is created in a single call.
	 */
	public String createCaseDataAttachment(final String municipalityId, final String namespace, final String errandId, final MultipartFile file) {
		return createAttachment(municipalityId, namespace, errandId, DOCUMENT_TYPE_CASE_DATA, file);
	}

	/**
	 * Whether the errand already carries its message-history archive. Used by the archiving job as
	 * its idempotency guard — an errand whose conversation has already been archived is skipped.
	 */
	@Transactional(readOnly = true)
	public boolean messageHistoryExists(final String errandId) {
		return attachmentRepository.existsByErrandIdAndDocumentType(errandId, DOCUMENT_TYPE_MESSAGE_HISTORY);
	}

	/**
	 * Store the platform-generated message-history PDF for the errand as a {@code MESSAGE_HISTORY}
	 * attachment under the given file name. Unique per errand — a second one is rejected — so the row doubles as the
	 * archived-marker for the archiving job.
	 *
	 * @param  fileName the file name to store the PDF under
	 * @param  content  the rendered PDF bytes
	 * @return          the id of the created attachment
	 */
	public String createMessageHistoryAttachment(final String municipalityId, final String namespace, final String errandId, final String fileName, final byte[] content) {
		// Lock the errand row so the one-per-errand check-then-insert can't race a concurrent archiving run.
		lockErrand(municipalityId, namespace, errandId);
		if (attachmentRepository.existsByErrandIdAndDocumentType(errandId, DOCUMENT_TYPE_MESSAGE_HISTORY)) {
			throw Problem.valueOf(BAD_REQUEST, MESSAGE_HISTORY_ALREADY_EXISTS_MESSAGE.formatted(errandId, namespace, municipalityId));
		}

		final var entity = toAttachmentEntity(errandId, namespace, municipalityId, DOCUMENT_TYPE_MESSAGE_HISTORY, null, new SourceFile(fileName, APPLICATION_PDF_VALUE, content));
		return attachmentRepository.save(entity).getId();
	}

	/**
	 * Combine a list of heterogeneous files into a single PDF (PDFs inlined, images rasterised, {@code .docx} rendered,
	 * anything else a placeholder page), in order. Exposed for the conversation-archiving job, which merges the rendered
	 * message pages with the errand's conversation attachments.
	 *
	 * @param  sources the files to combine, in order (must contain at least one element)
	 * @return         the combined PDF as bytes
	 */
	public byte[] combineToPdf(final List<SourceFile> sources) {
		return PdfCombiner.combine(sources);
	}

	/**
	 * Persist each uploaded file as its own attachment and, in addition, a single combined PDF that merges them all (in
	 * order). The combined PDF is stored as a further attachment named {@value #COMBINED_PDF_FILE_NAME}. The file list is
	 * type-agnostic — PDFs, images and {@code .docx} are inlined, anything else becomes a placeholder page — so an odd
	 * attachment never blocks the merge.
	 *
	 * @param  files the uploaded files (must contain at least one element)
	 * @return       the ids of the created attachments — the sources in order, followed by the combined PDF
	 */
	public List<String> storeAndCombine(final String municipalityId, final String namespace, final String errandId, final List<MultipartFile> files) {
		ensureErrandExists(municipalityId, namespace, errandId);

		final var sources = files.stream().map(AttachmentService::toSourceFile).toList();

		final var ids = new ArrayList<String>();
		sources.forEach(source -> ids.add(attachmentRepository
			.save(toAttachmentEntity(errandId, namespace, municipalityId, DOCUMENT_TYPE_APPLICATION, SENDER_CLIENT, source))
			.getId()));

		final var combined = PdfCombiner.combine(sources);
		ids.add(attachmentRepository
			.save(toAttachmentEntity(errandId, namespace, municipalityId, DOCUMENT_TYPE_GENERATED, SENDER_CLIENT, new SourceFile(COMBINED_PDF_FILE_NAME, APPLICATION_PDF_VALUE, combined)))
			.getId());

		return ids;
	}

	/**
	 * Rebuild the consolidated client-attachment PDF for the errand from all client-sent (INBOUND) conversation
	 * attachments and store it in place as a single {@code CONVERSATION} attachment named {@value #CLIENT_PDF_FILE_NAME}
	 * (it consolidates conversation files, so it shares their documentType — a {@code ?documentType=CONVERSATION} listing
	 * returns the
	 * client's conversation files together with this consolidation). Idempotent: if the row already exists its content is
	 * overwritten (same id/URL), otherwise it is created — safe to call repeatedly (incl. on Modulith event
	 * re-delivery). No-op when the client has not sent any attachments yet.
	 */
	public void regenerateClientAttachmentPdf(final String municipalityId, final String namespace, final String errandId) {
		final var contents = conversationAttachmentQueryService.clientAttachmentContentsForErrand(errandId);
		if (contents.isEmpty()) {
			return;
		}

		// Lock the errand row so two near-simultaneous conversation events can't both insert a fresh consolidation —
		// the second blocks here, then finds the row written by the first and overwrites in place (same id/URL).
		lockErrand(municipalityId, namespace, errandId);

		final var sources = contents.stream()
			.map(content -> new SourceFile(content.fileName(), content.mimeType(), content.content()))
			.toList();
		final var combined = PdfCombiner.combine(sources);
		final var rebuilt = toAttachmentEntity(errandId, namespace, municipalityId, DOCUMENT_TYPE_CONVERSATION, SENDER_CLIENT, new SourceFile(CLIENT_PDF_FILE_NAME, APPLICATION_PDF_VALUE, combined));

		attachmentRepository.findFirstByErrandIdAndFileNameAndDocumentType(errandId, CLIENT_PDF_FILE_NAME, DOCUMENT_TYPE_CONVERSATION)
			.ifPresentOrElse(existing -> {
				existing.getAttachmentData().setFile(rebuilt.getAttachmentData().getFile());
				existing.setFileSize(rebuilt.getFileSize());
				attachmentRepository.save(existing);
			}, () -> attachmentRepository.save(rebuilt));
	}

	/**
	 * The unified errand document list: the errand's own attachments (application files, generated PDFs, manual
	 * uploads) merged with the conversation's attachments, each tagged with {@code documentType} + {@code senderRole},
	 * oldest
	 * first. {@code documentType} and {@code senderRole} are optional filters — a null value matches everything.
	 */
	@Transactional(readOnly = true)
	public List<Attachment> readAttachments(final String municipalityId, final String namespace, final String errandId, final String documentType, final String senderRole) {
		ensureErrandExists(municipalityId, namespace, errandId);

		final var errandAttachments = toAttachmentList(attachmentRepository.findByErrandId(errandId)).stream();
		final var conversationAttachments = conversationAttachmentQueryService.listForErrand(errandId).stream()
			.map(AttachmentMapper::toAttachment);

		return Stream.concat(errandAttachments, conversationAttachments)
			.filter(attachment -> documentType == null || documentType.equals(attachment.getDocumentType()))
			.filter(attachment -> senderRole == null || senderRole.equals(attachment.getSenderRole()))
			.sorted(Comparator.comparing(Attachment::getCreated, nullsLast(Comparator.naturalOrder())))
			.toList();
	}

	@Transactional(readOnly = true)
	public Attachment readAttachment(final String municipalityId, final String namespace, final String errandId, final String attachmentId) {
		return toAttachment(findAttachment(municipalityId, namespace, errandId, attachmentId));
	}

	public void streamAttachmentFile(final String municipalityId, final String namespace, final String errandId, final String attachmentId, final HttpServletResponse response) {
		final var attachment = findAttachment(municipalityId, namespace, errandId, attachmentId);
		try {
			response.addHeader(CONTENT_TYPE, attachment.getMimeType());
			response.addHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"");
			ofNullable(attachment.getFileSize()).ifPresent(response::setContentLength);
			StreamUtils.copy(attachment.getAttachmentData().getFile().getBinaryStream(), response.getOutputStream());
		} catch (final IOException | SQLException exception) {
			throw Problem.valueOf(INTERNAL_SERVER_ERROR, STREAM_ERROR_MESSAGE.formatted(exception.getClass().getSimpleName(), attachment.getId(), exception.getMessage()));
		}
	}

	public void deleteAttachment(final String municipalityId, final String namespace, final String errandId, final String attachmentId) {
		final var entity = findAttachment(municipalityId, namespace, errandId, attachmentId);
		attachmentRepository.delete(entity);
	}

	/**
	 * Read a multipart file fully into memory once, so it can be both persisted and merged without re-reading the stream.
	 */
	private static SourceFile toSourceFile(final MultipartFile file) {
		try {
			return new SourceFile(file.getOriginalFilename(), file.getContentType(), file.getBytes());
		} catch (final IOException ioException) {
			throw Problem.valueOf(BAD_REQUEST, READ_ERROR_MESSAGE.formatted(ioException.getMessage()));
		}
	}

	private void ensureErrandExists(final String municipalityId, final String namespace, final String errandId) {
		getErrand(municipalityId, namespace, errandId);
	}

	/**
	 * Acquires a pessimistic write lock on the errand row so the check-then-insert uniqueness guards (one CASE_DATA, one
	 * MESSAGE_HISTORY and one consolidated client PDF per errand) are serialized: concurrent calls for the same errand
	 * block here rather than both passing the existence check and inserting duplicates.
	 */
	private void lockErrand(final String municipalityId, final String namespace, final String errandId) {
		if (!errandRepository.existsWithLockingByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId));
		}
	}

	private ErrandEntity getErrand(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private AttachmentEntity findAttachment(final String municipalityId, final String namespace, final String errandId, final String attachmentId) {
		return attachmentRepository.findByNamespaceAndMunicipalityIdAndErrandIdAndId(namespace, municipalityId, errandId, attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ATTACHMENT_NOT_FOUND_MESSAGE.formatted(attachmentId, errandId, namespace, municipalityId)));
	}
}
