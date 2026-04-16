package se.sundsvall.caremanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.api.model.Lookup;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

@Service
@ExcludeFromJacocoGeneratedCoverageReport
public class MetadataService {

	private static final String NOT_IMPLEMENTED = "not implemented yet";

	public String create(final String municipalityId, final String namespace, final LookupKind kind, final Lookup lookup) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public Lookup read(final String municipalityId, final String namespace, final LookupKind kind, final String name) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public List<Lookup> readAll(final String municipalityId, final String namespace, final LookupKind kind) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void update(final String municipalityId, final String namespace, final LookupKind kind, final String name, final Lookup lookup) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void delete(final String municipalityId, final String namespace, final LookupKind kind, final String name) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
