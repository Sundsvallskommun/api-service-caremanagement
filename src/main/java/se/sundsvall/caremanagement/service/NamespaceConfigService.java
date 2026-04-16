package se.sundsvall.caremanagement.service;

import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.api.model.NamespaceConfig;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

@Service
@ExcludeFromJacocoGeneratedCoverageReport
public class NamespaceConfigService {

	private static final String NOT_IMPLEMENTED = "not implemented yet";

	public Long create(final String municipalityId, final String namespace, final NamespaceConfig config) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public NamespaceConfig read(final String municipalityId, final String namespace) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void update(final String municipalityId, final String namespace, final NamespaceConfig config) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void delete(final String municipalityId, final String namespace) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
