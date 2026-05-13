package se.sundsvall.caremanagement.statushistory.integration.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.statushistory.integration.db.model.StatusHistoryEntity;

public interface StatusHistoryRepository extends JpaRepository<StatusHistoryEntity, String> {

	List<StatusHistoryEntity> findByErrandIdOrderByChangedAtDesc(String errandId);
}
