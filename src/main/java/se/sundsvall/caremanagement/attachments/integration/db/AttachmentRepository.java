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

	long deleteByErrandId(String errandId);
}
