package se.sundsvall.caremanagement.service.mapper;

import java.io.IOException;
import java.util.List;
import org.hibernate.Hibernate;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.api.model.Attachment;
import se.sundsvall.caremanagement.integration.db.model.AttachmentDataEntity;
import se.sundsvall.caremanagement.integration.db.model.AttachmentEntity;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public final class AttachmentMapper {

	private AttachmentMapper() {}

	public static Attachment toAttachment(final AttachmentEntity entity) {
		return ofNullable(entity)
			.map(e -> Attachment.create()
				.withId(e.getId())
				.withFileName(e.getFileName())
				.withMimeType(e.getMimeType())
				.withFileSize(e.getFileSize())
				.withCreated(e.getCreated())
				.withModified(e.getModified()))
			.orElse(null);
	}

	public static AttachmentEntity toAttachmentEntity(final ErrandEntity errandEntity, final MultipartFile file) {
		if (errandEntity == null || file == null) {
			return null;
		}
		try {
			return AttachmentEntity.create()
				.withErrandEntity(errandEntity)
				.withNamespace(errandEntity.getNamespace())
				.withMunicipalityId(errandEntity.getMunicipalityId())
				.withFileName(file.getOriginalFilename())
				.withMimeType(file.getContentType())
				.withFileSize(Math.toIntExact(file.getSize()))
				.withAttachmentData(AttachmentDataEntity.create()
					.withFile(Hibernate.getLobHelper().createBlob(file.getInputStream(), file.getSize())));
		} catch (final IOException e) {
			throw Problem.valueOf(BAD_REQUEST, "Could not read input stream: %s".formatted(e.getMessage()));
		}
	}

	public static List<Attachment> toAttachmentList(final List<AttachmentEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(AttachmentMapper::toAttachment)
			.toList();
	}
}
