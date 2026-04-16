package se.sundsvall.caremanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.api.model.Parameter;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.ParameterRepository;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.ParameterEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.service.mapper.ParameterMapper.toParameter;
import static se.sundsvall.caremanagement.service.mapper.ParameterMapper.toParameterEntity;
import static se.sundsvall.caremanagement.service.mapper.ParameterMapper.toParameterList;
import static se.sundsvall.caremanagement.service.mapper.ParameterMapper.updateParameterEntity;

@Service
@Transactional
public class ErrandParameterService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String PARAMETER_NOT_FOUND_MESSAGE = "No parameter with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";

	private final ErrandRepository errandRepository;
	private final ParameterRepository parameterRepository;

	ErrandParameterService(final ErrandRepository errandRepository, final ParameterRepository parameterRepository) {
		this.errandRepository = errandRepository;
		this.parameterRepository = parameterRepository;
	}

	public String create(final String municipalityId, final String namespace, final String errandId, final Parameter parameter) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		final var saved = parameterRepository.save(toParameterEntity(parameter, errand));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public Parameter read(final String municipalityId, final String namespace, final String errandId, final String parameterId) {
		return toParameter(findParameter(municipalityId, namespace, errandId, parameterId));
	}

	@Transactional(readOnly = true)
	public List<Parameter> readAll(final String municipalityId, final String namespace, final String errandId) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		return toParameterList(errand.getParameters());
	}

	public void update(final String municipalityId, final String namespace, final String errandId, final String parameterId, final Parameter parameter) {
		final var entity = findParameter(municipalityId, namespace, errandId, parameterId);
		updateParameterEntity(entity, parameter);
		parameterRepository.save(entity);
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String parameterId) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		final var parameter = errand.getParameters().stream()
			.filter(p -> p.getId().equals(parameterId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, PARAMETER_NOT_FOUND_MESSAGE.formatted(parameterId, errandId, namespace, municipalityId)));
		errand.getParameters().remove(parameter);
		errandRepository.save(errand);
	}

	private ErrandEntity findErrand(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private ParameterEntity findParameter(final String municipalityId, final String namespace, final String errandId, final String parameterId) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		return errand.getParameters().stream()
			.filter(p -> p.getId().equals(parameterId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, PARAMETER_NOT_FOUND_MESSAGE.formatted(parameterId, errandId, namespace, municipalityId)));
	}
}
