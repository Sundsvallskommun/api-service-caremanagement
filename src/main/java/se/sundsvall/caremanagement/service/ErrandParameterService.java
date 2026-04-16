package se.sundsvall.caremanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.api.model.Parameter;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

@Service
@ExcludeFromJacocoGeneratedCoverageReport
public class ErrandParameterService {

	private static final String NOT_IMPLEMENTED = "not implemented yet";

	public String create(final String municipalityId, final String namespace, final String errandId, final Parameter parameter) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public Parameter read(final String municipalityId, final String namespace, final String errandId, final String parameterId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public List<Parameter> readAll(final String municipalityId, final String namespace, final String errandId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void update(final String municipalityId, final String namespace, final String errandId, final String parameterId, final Parameter parameter) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String parameterId) {
		throw new UnsupportedOperationException(NOT_IMPLEMENTED);
	}
}
