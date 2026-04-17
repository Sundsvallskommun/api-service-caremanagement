package se.sundsvall.caremanagement.service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.api.model.Attachment;
import se.sundsvall.caremanagement.integration.db.AttachmentRepository;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.service.mapper.AttachmentMapper.toAttachment;
import static se.sundsvall.caremanagement.service.mapper.AttachmentMapper.toAttachmentEntity;
import static se.sundsvall.caremanagement.service.mapper.AttachmentMapper.toAttachmentList;

@Service
@Transactional
public class ErrandAttachmentService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String ATTACHMENT_NOT_FOUND_MESSAGE = "No attachment with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";
	private static final String STREAM_ERROR_MESSAGE = "%s occurred when copying file with attachment id '%s' to response: %s";

	private final ErrandRepository errandRepository;
	private final AttachmentRepository attachmentRepository;

	ErrandAttachmentService(final ErrandRepository errandRepository, final AttachmentRepository attachmentRepository) {
		this.errandRepository = errandRepository;
		this.attachmentRepository = attachmentRepository;
	}

	public String createAttachment(final String municipalityId, final String namespace, final String errandId, final MultipartFile file) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		final var saved = attachmentRepository.save(toAttachmentEntity(errand, file));
		return saved.getId();
	}

	public List<Attachment> readAttachments(final String municipalityId, final String namespace, final String errandId) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		return toAttachmentList(errand.getAttachments());
	}

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
		final var errand = findErrand(municipalityId, namespace, errandId);
		final var attachment = errand.getAttachments().stream()
			.filter(entity -> entity.getId().equals(attachmentId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ATTACHMENT_NOT_FOUND_MESSAGE.formatted(attachmentId, errandId, namespace, municipalityId)));
		errand.getAttachments().remove(attachment);
		errandRepository.save(errand);
	}

	private ErrandEntity findErrand(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private AttachmentEntity findAttachment(final String municipalityId, final String namespace, final String errandId, final String attachmentId) {
		return attachmentRepository.findByNamespaceAndMunicipalityIdAndErrandEntityIdAndId(namespace, municipalityId, errandId, attachmentId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ATTACHMENT_NOT_FOUND_MESSAGE.formatted(attachmentId, errandId, namespace, municipalityId)));
	}
}
