package se.sundsvall.caremanagement.statushistory.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.statushistory.api.model.StatusHistoryEntry;
import se.sundsvall.caremanagement.statushistory.integration.db.StatusHistoryRepository;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class StatusHistoryService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";

	private final ErrandRepository errandRepository;
	private final StatusHistoryRepository repository;

	StatusHistoryService(final ErrandRepository errandRepository, final StatusHistoryRepository repository) {
		this.errandRepository = errandRepository;
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<StatusHistoryEntry> listForErrand(final String municipalityId, final String namespace, final String errandId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return repository.findByErrandIdOrderByChangedAtDesc(errandId).stream()
			.map(e -> new StatusHistoryEntry(
				e.getId(), e.getErrandId(), e.getFromStatus(), e.getToStatus(),
				e.getChangedBy(), e.getChangedAt()))
			.toList();
	}

	private void ensureErrandExists(final String municipalityId, final String namespace, final String errandId) {
		if (!errandRepository.existsByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId));
		}
	}
}
