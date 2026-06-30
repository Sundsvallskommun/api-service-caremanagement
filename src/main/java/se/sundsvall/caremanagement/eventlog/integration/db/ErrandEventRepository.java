package se.sundsvall.caremanagement.eventlog.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;

@CircuitBreaker(name = "errandEventRepository")
public interface ErrandEventRepository extends JpaRepository<ErrandEventEntity, String> {

	/**
	 * The errand's events newest-first, with all filters applied in the database. {@code action}/{@code actor}/
	 * {@code source} are optional (case-insensitive; {@code null} matches all); when {@code includeReads} is false the
	 * READ rows are dropped. Filtering at the DB avoids materialising the whole — potentially large — per-errand history
	 * into the heap just to filter it in Java.
	 */
	@Query("""
		select e from ErrandEventEntity e
		where e.errandId = :errandId
		  and (:action is null or lower(e.action) = lower(:action))
		  and (:actor is null or lower(e.actor) = lower(:actor))
		  and (:source is null or lower(e.source) = lower(:source))
		  and (:includeReads = true or lower(e.action) <> 'read')
		order by e.created desc
		""")
	List<ErrandEventEntity> findFiltered(@Param("errandId") String errandId, @Param("action") String action, @Param("actor") String actor, @Param("source") String source, @Param("includeReads") boolean includeReads);

	/** Counts the errand's events honouring the same filters as {@link #findFiltered}, DB-side (no row materialisation). */
	@Query("""
		select count(e) from ErrandEventEntity e
		where e.errandId = :errandId
		  and (:action is null or lower(e.action) = lower(:action))
		  and (:actor is null or lower(e.actor) = lower(:actor))
		  and (:source is null or lower(e.source) = lower(:source))
		  and (:includeReads = true or lower(e.action) <> 'read')
		""")
	long countFiltered(@Param("errandId") String errandId, @Param("action") String action, @Param("actor") String actor, @Param("source") String source, @Param("includeReads") boolean includeReads);
}
