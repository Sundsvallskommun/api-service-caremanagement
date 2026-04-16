package se.sundsvall.caremanagement.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.api.model.Errand;
import se.sundsvall.caremanagement.api.model.PatchErrand;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

@Service
@ExcludeFromJacocoGeneratedCoverageReport
public class ErrandService {

	private static final String NOT_IMPLEMENTED = "not implemented yet";

	public String createErrand(final String municipalityId, final String namespace, final Errand errand) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public Errand readErrand(final String municipalityId, final String namespace, final String errandId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public Page<Errand> findErrands(final String municipalityId, final String namespace, final Specification<ErrandEntity> filter, final Pageable pageable) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void updateErrand(final String municipalityId, final String namespace, final String errandId, final PatchErrand patch) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void deleteErrand(final String municipalityId, final String namespace, final String errandId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
