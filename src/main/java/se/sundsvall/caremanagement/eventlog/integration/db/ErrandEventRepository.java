package se.sundsvall.caremanagement.eventlog.integration.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;

public interface ErrandEventRepository extends JpaRepository<ErrandEventEntity, String> {

	List<ErrandEventEntity> findByErrandIdOrderByCreatedDesc(String errandId);
}
