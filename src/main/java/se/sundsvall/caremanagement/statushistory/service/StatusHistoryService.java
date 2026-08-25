package se.sundsvall.caremanagement.statushistory.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.caremanagement.statushistory.api.model.StatusHistoryEntry;
import se.sundsvall.caremanagement.statushistory.integration.db.StatusHistoryRepository;

@Service
@Transactional
public class StatusHistoryService {

	private final ErrandAccessGuard errandGuard;
	private final StatusHistoryRepository statusHistoryRepository;

	StatusHistoryService(final ErrandAccessGuard errandGuard, final StatusHistoryRepository statusHistoryRepository) {
		this.errandGuard = errandGuard;
		this.statusHistoryRepository = statusHistoryRepository;
	}

	@Transactional(readOnly = true)
	public List<StatusHistoryEntry> listForErrand(final String municipalityId, final String namespace, final String errandId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		return statusHistoryRepository.findByErrandIdOrderByChangedAtDesc(errandId).stream()
			.map(e -> new StatusHistoryEntry(
				e.getId(), e.getErrandId(), e.getFromStatus(), e.getToStatus(),
				e.getChangedBy(), e.getChangedAt()))
			.toList();
	}
}
