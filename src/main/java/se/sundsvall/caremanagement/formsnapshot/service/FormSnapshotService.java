package se.sundsvall.caremanagement.formsnapshot.service;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot;
import se.sundsvall.caremanagement.formsnapshot.integration.db.FormSnapshotRepository;
import se.sundsvall.caremanagement.formsnapshot.service.mapper.FormSnapshotMapper;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Captures and reads the immutable form snapshot of an errand — the citizen-facing application form as it was rendered
 * and answered, authored by the frontend and stored verbatim. Write-once: one snapshot per errand, enforced by the
 * {@code existsByErrandId} guard here and the unique constraint on the table; there is no update path.
 *
 * <p>
 * The payload is sensitive personal data. Logging is deliberately limited to {@code errandId}, {@code typeSlug}, the
 * section count and the hash — never field values or the payload itself.
 * </p>
 */
@Service
@Transactional
public class FormSnapshotService {

	private static final Logger LOG = LoggerFactory.getLogger(FormSnapshotService.class);

	private final FormSnapshotRepository repository;
	private final ObjectMapper objectMapper;

	FormSnapshotService(final FormSnapshotRepository repository, final ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	/**
	 * Persist the form snapshot for an errand, write-once. {@code payload} is the verbatim JSON the frontend rendered;
	 * it is parsed only to validate it and lift the envelope metadata onto indexed columns, then stored as received so
	 * the {@code contentHash} stays reproducible. Rejects with {@code 400} when the payload is blank, malformed, or
	 * structurally invalid, or when a snapshot already exists for the errand.
	 */
	public void capture(final String municipalityId, final String namespace, final String errandId, final String typeSlug, final String payload) {
		if (!StringUtils.hasText(payload)) {
			throw Problem.valueOf(BAD_REQUEST, "formSnapshot is blank");
		}
		if (repository.existsByErrandId(errandId)) {
			throw Problem.valueOf(BAD_REQUEST, "A form snapshot already exists on errand '%s'".formatted(errandId));
		}

		final var snapshot = parse(payload);
		validate(snapshot);

		final var entity = FormSnapshotMapper.toEntity(municipalityId, namespace, errandId, typeSlug, payload, snapshot, OffsetDateTime.now());
		final var saved = repository.save(entity);
		LOG.info("Captured form snapshot for errand {} (typeSlug={}, sections={}, hash={})",
			errandId, typeSlug, snapshot.getSections().size(), abbreviate(saved.getContentHash()));
	}

	/**
	 * The structured snapshot for an errand, for re-display. Throws {@code 404} when no snapshot was captured. Callers
	 * are responsible for the municipality/namespace scope check before calling this.
	 */
	@Transactional(readOnly = true)
	public FormSnapshot read(final String errandId) {
		return repository.findByErrandId(errandId)
			.map(entity -> parse(entity.getPayload()))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No form snapshot for errand '%s'".formatted(errandId)));
	}

	private FormSnapshot parse(final String payload) {
		try {
			return objectMapper.readValue(payload, FormSnapshot.class);
		} catch (final JacksonException e) {
			// Do not include the payload (sensitive data) in the message.
			throw Problem.valueOf(BAD_REQUEST, "formSnapshot is not valid JSON");
		}
	}

	private static void validate(final FormSnapshot snapshot) {
		if (!StringUtils.hasText(snapshot.getSchemaVersion())) {
			throw Problem.valueOf(BAD_REQUEST, "formSnapshot.schemaVersion is required");
		}
		if (snapshot.getSections() == null || snapshot.getSections().isEmpty()) {
			throw Problem.valueOf(BAD_REQUEST, "formSnapshot.sections must not be empty");
		}
	}

	private static String abbreviate(final String hash) {
		return hash.length() <= 12 ? hash : hash.substring(0, 12);
	}
}
