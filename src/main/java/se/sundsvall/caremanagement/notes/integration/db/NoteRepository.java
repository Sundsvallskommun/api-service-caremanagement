package se.sundsvall.caremanagement.notes.integration.db;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.notes.integration.db.model.NoteEntity;

public interface NoteRepository extends JpaRepository<NoteEntity, String> {

	List<NoteEntity> findByErrandIdOrderByCreatedDesc(String errandId);
}
