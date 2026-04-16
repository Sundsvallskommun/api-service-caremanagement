package se.sundsvall.caremanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.api.model.Stakeholder;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

@Service
@ExcludeFromJacocoGeneratedCoverageReport
public class ErrandStakeholderService {

	private static final String NOT_IMPLEMENTED = "not implemented yet";

	public String create(final String municipalityId, final String namespace, final String errandId, final Stakeholder stakeholder) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public Stakeholder read(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public List<Stakeholder> readAll(final String municipalityId, final String namespace, final String errandId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void update(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final Stakeholder stakeholder) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
