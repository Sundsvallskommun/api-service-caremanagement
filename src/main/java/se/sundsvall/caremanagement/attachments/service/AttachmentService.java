package se.sundsvall.caremanagement.attachments.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.attachments.api.model.Attachment;
import se.sundsvall.caremanagement.attachments.integration.db.AttachmentRepository;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.attachments.service.mapper.AttachmentMapper.toAttachment;
import static se.sundsvall.caremanagement.attachments.service.mapper.AttachmentMapper.toAttachmentEntity;
import static se.sundsvall.caremanagement.attachments.service.mapper.AttachmentMapper.toAttachmentList;

@Service
@Transactional
public class AttachmentService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String ATTACHMENT_NOT_FOUND_MESSAGE = "No attachment with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";
	private static final String STREAM_ERROR_MESSAGE = "%s occurred when copying file with attachment id '%s' to response: %s";
	private static final String READ_ERROR_MESSAGE = "Could not read input stream: %s";

	/** Filename + MIME type of the generated combined-PDF attachment that merges all uploaded files. */
	private static final String COMBINED_PDF_FILE_NAME = "sammanstallning.pdf";
	private static final String PDF_MIME_TYPE = "application/pdf";

	private final ErrandRepository errandRepository;
	private final AttachmentRepository attachmentRepository;

	AttachmentService(final ErrandRepository errandRepository, final AttachmentRepository attachmentRepository) {
		this.errandRepository = errandRepository;
		this.attachmentRepository = attachmentRepository;
	}

	public String createAttachment(final String municipalityId, final String namespace, final String errandId, final MultipartFile file) {
		ensureErrandExists(municipalityId, namespace, errandId);
		final var saved = attachmentRepository.save(toAttachmentEntity(errandId, namespace, municipalityId, file));
		return saved.getId();
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

		final var sources = files.stream().map(AttachmentService::read).toList();

		final var ids = new ArrayList<String>();
		sources.forEach(source -> ids.add(attachmentRepository
			.save(toAttachmentEntity(errandId, namespace, municipalityId, source.fileName(), source.contentType(), source.content()))
			.getId()));

		final var combined = PdfCombiner.combine(sources);
		ids.add(attachmentRepository
			.save(toAttachmentEntity(errandId, namespace, municipalityId, COMBINED_PDF_FILE_NAME, PDF_MIME_TYPE, combined))
			.getId());

		return ids;
	}

	@Transactional(readOnly = true)
	public List<Attachment> readAttachments(final String municipalityId, final String namespace, final String errandId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return toAttachmentList(attachmentRepository.findByErrandId(errandId));
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
	private static SourceFile read(final MultipartFile file) {
		try {
			return new SourceFile(file.getOriginalFilename(), file.getContentType(), file.getBytes());
		} catch (final IOException ioException) {
			throw Problem.valueOf(BAD_REQUEST, READ_ERROR_MESSAGE.formatted(ioException.getMessage()));
		}
	}

	private void ensureErrandExists(final String municipalityId, final String namespace, final String errandId) {
		errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private AttachmentEntity findAttachment(final String municipalityId, final String namespace, final String errandId, final String attachmentId) {
		return attachmentRepository.findByNamespaceAndMunicipalityIdAndErrandIdAndId(namespace, municipalityId, errandId, attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ATTACHMENT_NOT_FOUND_MESSAGE.formatted(attachmentId, errandId, namespace, municipalityId)));
	}
}
