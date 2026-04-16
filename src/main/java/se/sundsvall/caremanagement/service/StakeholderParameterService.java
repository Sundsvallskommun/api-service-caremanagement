package se.sundsvall.caremanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.api.model.StakeholderParameter;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

@Service
@ExcludeFromJacocoGeneratedCoverageReport
public class StakeholderParameterService {

	private static final String NOT_IMPLEMENTED = "not implemented yet";

	public Long create(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final StakeholderParameter parameter) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public StakeholderParameter read(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final Long parameterId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public List<StakeholderParameter> readAll(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void update(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final Long parameterId, final StakeholderParameter parameter) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final Long parameterId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
