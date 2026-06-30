package se.sundsvall.caremanagement.notes.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.notes.integration.db.model.NoteEntity;

@CircuitBreaker(name = "noteRepository")
public interface NoteRepository extends JpaRepository<NoteEntity, String> {

	List<NoteEntity> findByErrandIdOrderByCreatedDesc(String errandId);

	Optional<NoteEntity> findByErrandIdAndId(String errandId, String id);

	long countByErrandId(String errandId);
}
