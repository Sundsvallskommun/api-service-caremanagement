package se.sundsvall.caremanagement.service;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.caremanagement.api.model.Attachment;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

@Service
@ExcludeFromJacocoGeneratedCoverageReport
public class ErrandAttachmentService {

	private static final String NOT_IMPLEMENTED = "not implemented yet";

	public String createAttachment(final String municipalityId, final String namespace, final String errandId, final MultipartFile file) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public List<Attachment> readAttachments(final String municipalityId, final String namespace, final String errandId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public Attachment readAttachment(final String municipalityId, final String namespace, final String errandId, final String attachmentId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void streamAttachmentFile(final String municipalityId, final String namespace, final String errandId, final String attachmentId, final HttpServletResponse response) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void deleteAttachment(final String municipalityId, final String namespace, final String errandId, final String attachmentId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
