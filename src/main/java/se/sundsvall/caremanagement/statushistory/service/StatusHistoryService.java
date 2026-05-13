package se.sundsvall.caremanagement.statushistory.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.statushistory.api.model.StatusHistoryEntry;
import se.sundsvall.caremanagement.statushistory.integration.db.StatusHistoryRepository;

@Service
@Transactional
public class StatusHistoryService {

	private final StatusHistoryRepository repository;

	StatusHistoryService(final StatusHistoryRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<StatusHistoryEntry> listForErrand(final String errandId) {
		return repository.findByErrandIdOrderByChangedAtDesc(errandId).stream()
			.map(e -> new StatusHistoryEntry(
				e.getId(), e.getErrandId(), e.getFromStatus(), e.getToStatus(),
				e.getChangedBy(), e.getChangedAt()))
			.toList();
	}
}
