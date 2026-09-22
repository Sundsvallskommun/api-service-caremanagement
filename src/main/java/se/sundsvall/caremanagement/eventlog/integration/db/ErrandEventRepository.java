package se.sundsvall.caremanagement.eventlog.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.eventlog.integration.db.model.ErrandEventEntity;

@CircuitBreaker(name = "errandEventRepository")
public interface ErrandEventRepository extends JpaRepository<ErrandEventEntity, String> {

	/**
	 * The errand's events newest-first, with all filters applied in the database. Scoped to the owning tenant
	 * ({@code municipalityId} + {@code namespace}) so a caller on one tenant's path can never read another tenant's
	 * activity log by errand id. {@code action}/{@code actor}/{@code source} are optional (case-insensitive;
	 * {@code null} matches all); when {@code includeReads} is false the READ rows are dropped. Filtering at the DB avoids
	 * materialising the whole — potentially large — per-errand history into the heap just to filter it in Java.
	 */
	@Query("""
		select e from ErrandEventEntity e
		where e.errandId = :errandId
		  and e.municipalityId = :municipalityId
		  and e.namespace = :namespace
		  and (:action is null or lower(e.action) = lower(:action))
		  and (:actor is null or lower(e.actor) = lower(:actor))
		  and (:source is null or lower(e.source) = lower(:source))
		  and (:includeReads = true or lower(e.action) <> 'read')
		order by e.created desc
		""")
	List<ErrandEventEntity> findFiltered(@Param("municipalityId") String municipalityId, @Param("namespace") String namespace, @Param("errandId") String errandId, @Param("action") String action, @Param("actor") String actor, @Param("source") String source,
		@Param("includeReads") boolean includeReads);

	/**
	 * One actor's events across every errand in the tenant, newest-first — the logguppföljning read.
	 * <p>
	 * Verksamhetens regelverk (revision 2026-09-22): <em>”Måste kunna söka upp loggar på en användare, inte bara per
	 * ärende”</em>. {@link #findFiltered} can already narrow by actor, but only inside one errand, which answers ”who
	 * touched this case” and not ”what has this caseworker looked at” — and the second question is the one a
	 * logguppföljning asks.
	 * <p>
	 * Bounded by a window that includes {@code from} and excludes {@code to}, rather than unbounded: an actor's history
	 * spans every errand
	 * they have ever opened, and a follow-up is always about a period. Both bounds are optional ({@code null} matches
	 * all) so the query still answers “everything” when that is what is wanted.
	 * <p>
	 * Reads are included by design — a logguppföljning is mostly about who <em>read</em> what, which is the opposite of
	 * the errand timeline's default use.
	 */
	@Query("""
		select e from ErrandEventEntity e
		where e.municipalityId = :municipalityId
		  and e.namespace = :namespace
		  and lower(e.actor) = lower(:actor)
		  and (:action is null or lower(e.action) = lower(:action))
		  and (:source is null or lower(e.source) = lower(:source))
		  and (:from is null or e.created >= :from)
		  and (:to is null or e.created < :to)
		order by e.created desc
		""")
	List<ErrandEventEntity> findByActor(@Param("municipalityId") String municipalityId, @Param("namespace") String namespace, @Param("actor") String actor,
		@Param("action") String action, @Param("source") String source, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, Pageable pageable);

	/**
	 * Counts one actor's events across the tenant, honouring the same scope and filters as {@link #findByActor} — so a
	 * capped listing can say how much it did not show.
	 */
	@Query("""
		select count(e) from ErrandEventEntity e
		where e.municipalityId = :municipalityId
		  and e.namespace = :namespace
		  and lower(e.actor) = lower(:actor)
		  and (:action is null or lower(e.action) = lower(:action))
		  and (:source is null or lower(e.source) = lower(:source))
		  and (:from is null or e.created >= :from)
		  and (:to is null or e.created < :to)
		""")
	long countByActor(@Param("municipalityId") String municipalityId, @Param("namespace") String namespace, @Param("actor") String actor,
		@Param("action") String action, @Param("source") String source, @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

	/**
	 * Counts the errand's events honouring the same tenant scope and filters as {@link #findFiltered}, DB-side (no row
	 * materialisation).
	 */
	@Query("""
		select count(e) from ErrandEventEntity e
		where e.errandId = :errandId
		  and e.municipalityId = :municipalityId
		  and e.namespace = :namespace
		  and (:action is null or lower(e.action) = lower(:action))
		  and (:actor is null or lower(e.actor) = lower(:actor))
		  and (:source is null or lower(e.source) = lower(:source))
		  and (:includeReads = true or lower(e.action) <> 'read')
		""")
	long countFiltered(@Param("municipalityId") String municipalityId, @Param("namespace") String namespace, @Param("errandId") String errandId, @Param("action") String action, @Param("actor") String actor, @Param("source") String source,
		@Param("includeReads") boolean includeReads);

	/**
	 * Disposes an errand's entire activity log — used only when the errand itself is deleted (gallrat), so the legal
	 * who/what/when record lives exactly as long as the errand and is never trimmed on a time or size basis. Tenant-scoped
	 * like the read queries. A single {@code @Modifying} delete on the {@code errand_id} index; {@code errand_event} has
	 * no child rows, so there is nothing to cascade.
	 *
	 * @return the number of rows deleted
	 */
	@Modifying
	@Query("delete from ErrandEventEntity e where e.errandId = :errandId and e.municipalityId = :municipalityId and e.namespace = :namespace")
	int deleteByErrandIdAndMunicipalityIdAndNamespace(@Param("errandId") String errandId, @Param("municipalityId") String municipalityId, @Param("namespace") String namespace);
}
