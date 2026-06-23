package se.sundsvall.caremanagement.document.integration.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.document.integration.db.model.DocumentEntity;

public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {

	List<DocumentEntity> findByErrandIdOrderByDocumentDateDescDocumentTimeDescCreatedDesc(String errandId);
}
