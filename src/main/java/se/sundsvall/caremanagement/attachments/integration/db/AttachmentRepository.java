package se.sundsvall.caremanagement.attachments.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentEntity;

@CircuitBreaker(name = "attachmentRepository")
public interface AttachmentRepository extends JpaRepository<AttachmentEntity, String> {

	List<AttachmentEntity> findByErrandId(String errandId);

	Optional<AttachmentEntity> findByNamespaceAndMunicipalityIdAndErrandIdAndId(String namespace, String municipalityId, String errandId, String id);

	List<AttachmentEntity> findByNamespaceAndMunicipalityIdAndIdIn(String namespace, String municipalityId, List<String> ids);

	/**
	 * Finds an existing generated attachment by its (errand, file name, documentType) so a rebuild can overwrite it in
	 * place
	 * instead of creating a duplicate — e.g. the consolidated client-attachment PDF that is regenerated as messages
	 * arrive.
	 */
	Optional<AttachmentEntity> findFirstByErrandIdAndFileNameAndDocumentType(String errandId, String fileName, String documentType);

	/** Guards the at-most-one-per-errand rule for the case-data attachment. */
	boolean existsByErrandIdAndDocumentType(String errandId, String documentType);
}
