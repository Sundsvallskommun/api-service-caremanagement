package se.sundsvall.caremanagement.journal.integration.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.journal.integration.db.model.JournalEntryEntity;

public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, String> {

	List<JournalEntryEntity> findByErrandIdOrderByEntryDateDescEntryTimeDescCreatedDesc(String errandId);
}
