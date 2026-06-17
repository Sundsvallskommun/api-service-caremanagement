package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.types.financialassistance.api.model.DraftIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningDraft;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaNormberakningDraftRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormberakningDraftEntity;
import se.sundsvall.dept44.problem.Problem;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * The editable draft normberäkning. The daily prepare loop {@link #refresh refreshes} it from the freshly computed FC
 * income rows — overwriting while the handläggare has not touched it, and preserving the rows once {@link #replace
 * edited}, reporting any income types that have newly appeared so they can be raised as warnings. The rows are stored
 * as
 * a JSON array; on a beslut the (possibly edited) rows are read back via {@link #editedRows}.
 */
@Service
public class DraftService {

	private final FaNormberakningDraftRepository repository;
	private final ObjectMapper objectMapper;

	DraftService(final FaNormberakningDraftRepository repository, final ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	/**
	 * Refresh the draft from the freshly computed rows. While the draft is untouched the rows are overwritten; once a
	 * handläggare has edited it the rows are preserved and the income-type names that have newly appeared (present in
	 * {@code autoRows} but not in the edited draft) are returned so the caller can raise NEW_INCOME warnings.
	 */
	@Transactional
	public List<String> refresh(final String errandId, final String applicationMonth, final List<DraftIncomeRow> autoRows) {
		final var existing = repository.findById(errandId).orElse(null);
		final var rows = ofNullable(autoRows).orElseGet(List::of);

		if (existing == null) {
			repository.save(FaNormberakningDraftEntity.create()
				.withErrandId(errandId).withApplicationMonth(applicationMonth).withEdited(false).withRowsJson(serialize(rows)));
			return List.of();
		}
		if (!existing.isEdited()) {
			repository.save(existing.withApplicationMonth(applicationMonth).withRowsJson(serialize(rows)));
			return List.of();
		}

		// Edited → preserve the rows; report income types now present that the edited draft does not carry.
		final var draftTypes = deserialize(existing.getRowsJson()).stream()
			.map(DraftIncomeRow::getTypeName).filter(StringUtils::hasText).map(DraftService::normalize).collect(toSet());
		return rows.stream()
			.map(DraftIncomeRow::getTypeName).filter(StringUtils::hasText).distinct()
			.filter(name -> !draftTypes.contains(normalize(name)))
			.toList();
	}

	@Transactional(readOnly = true)
	public NormberakningDraft get(final String errandId) {
		return repository.findById(errandId).map(this::toDraft)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "No draft normberäkning for errand"));
	}

	/** Replace the draft's rows with the handläggare's edit and mark it edited (so the daily refresh stops overwriting). */
	@Transactional
	public NormberakningDraft replace(final String errandId, final String applicationMonth, final List<DraftIncomeRow> rows) {
		final var entity = repository.findById(errandId).orElseGet(() -> FaNormberakningDraftEntity.create().withErrandId(errandId));
		ofNullable(applicationMonth).filter(StringUtils::hasText).ifPresent(entity::setApplicationMonth);
		entity.setEdited(true);
		entity.setRowsJson(serialize(ofNullable(rows).orElseGet(List::of)));
		return toDraft(repository.save(entity));
	}

	/** The draft's rows when (and only when) a handläggare has edited them — what commit posts to Lifecare. */
	@Transactional(readOnly = true)
	public Optional<List<DraftIncomeRow>> editedRows(final String errandId) {
		return repository.findById(errandId).filter(FaNormberakningDraftEntity::isEdited).map(entity -> deserialize(entity.getRowsJson()));
	}

	private NormberakningDraft toDraft(final FaNormberakningDraftEntity entity) {
		return NormberakningDraft.create()
			.withErrandId(entity.getErrandId())
			.withApplicationMonth(entity.getApplicationMonth())
			.withEdited(entity.isEdited())
			.withRows(deserialize(entity.getRowsJson()))
			.withCreated(entity.getCreated())
			.withUpdated(entity.getUpdated());
	}

	private String serialize(final List<DraftIncomeRow> rows) {
		try {
			return objectMapper.writeValueAsString(rows);
		} catch (final JacksonException e) {
			throw new IllegalStateException("Failed to serialize draft rows to JSON", e);
		}
	}

	private List<DraftIncomeRow> deserialize(final String json) {
		if (!StringUtils.hasText(json)) {
			return List.of();
		}
		try {
			return List.of(objectMapper.readValue(json, DraftIncomeRow[].class));
		} catch (final JacksonException e) {
			throw new IllegalStateException("Failed to deserialize draft rows from JSON", e);
		}
	}

	private static String normalize(final String value) {
		return (value == null) ? "" : value.trim().toLowerCase();
	}
}
