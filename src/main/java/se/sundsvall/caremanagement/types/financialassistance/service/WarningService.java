package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaWarningRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaWarningEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * EB income warnings as acknowledgeable objects. The daily prepare step reconciles the current set against what is
 * stored — creating new warnings, refreshing open ones, and auto-closing ones whose cause has resolved — while never
 * re-opening a warning the handläggare has already acted on. A handläggare can acknowledge or close each warning.
 */
@Service
public class WarningService {

	public static final String TYPE_UNHANDLED_INCOME = "UNHANDLED_INCOME";
	public static final String TYPE_INCOME_CHANGE = "INCOME_CHANGE";
	public static final String TYPE_MISSING_SSBTEK = "MISSING_SSBTEK";
	public static final String TYPE_NEW_INCOME = "NEW_INCOME";

	public static final String STATUS_OPEN = "OPEN";
	public static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
	public static final String STATUS_CLOSED = "CLOSED";

	private final FaWarningRepository repository;

	WarningService(final FaWarningRepository repository) {
		this.repository = repository;
	}

	/** A computed warning before persistence — the dedup key is {@code type + sourceKey}. */
	public record WarningInput(String type, String sourceKey, String message) {
	}

	/**
	 * Reconcile the income warnings produced by the regelverk + completeness + draft checks into the errand's warning
	 * objects: unhandled / changed / still-missing incomes, plus income that has newly arrived in SSBTEK but is not in the
	 * handläggare's edited draft normberäkning.
	 */
	public void reconcileIncomeWarnings(final String errandId, final List<String> unhandled, final List<String> changes,
		final List<String> missing, final List<String> newIncome) {
		final List<WarningInput> inputs = new ArrayList<>();
		ofList(unhandled).forEach(text -> inputs.add(new WarningInput(TYPE_UNHANDLED_INCOME, sourceKey(text), text)));
		ofList(changes).forEach(text -> inputs.add(new WarningInput(TYPE_INCOME_CHANGE, sourceKey(text), text)));
		ofList(missing).forEach(text -> inputs.add(new WarningInput(TYPE_MISSING_SSBTEK, text, "Saknas ännu i SSBTEK: " + text)));
		ofList(newIncome).forEach(text -> inputs.add(new WarningInput(TYPE_NEW_INCOME, text, "Ny inkomst i SSBTEK, ej införd i normberäkningen: " + text)));
		reconcile(errandId, inputs);
	}

	/**
	 * Reconcile the errand's warnings against {@code current}: create the ones that are new, refresh the message of ones
	 * still OPEN/ACKNOWLEDGED, and auto-close ones whose cause has resolved (no longer in {@code current}). A CLOSED
	 * warning is never re-opened.
	 */
	@Transactional
	public void reconcile(final String errandId, final List<WarningInput> current) {
		final var existing = repository.findByErrandId(errandId);
		final var currentKeys = current.stream().map(input -> key(input.type(), input.sourceKey())).collect(toSet());

		for (final var input : current) {
			final var match = existing.stream()
				.filter(entity -> key(entity.getType(), entity.getSourceKey()).equals(key(input.type(), input.sourceKey())))
				.findFirst();
			if (match.isEmpty()) {
				repository.save(FaWarningEntity.create()
					.withErrandId(errandId)
					.withType(input.type())
					.withSourceKey(input.sourceKey())
					.withMessage(input.message())
					.withStatus(STATUS_OPEN)
					.withAutoResolved(false));
			} else if (!STATUS_CLOSED.equals(match.get().getStatus())) { // never re-open a closed warning
				repository.save(match.get().withMessage(input.message()));
			}
		}

		// Cause resolved (no longer computed) → auto-close the ones still open/acknowledged.
		existing.stream()
			.filter(entity -> !currentKeys.contains(key(entity.getType(), entity.getSourceKey())))
			.filter(entity -> !STATUS_CLOSED.equals(entity.getStatus()))
			.forEach(entity -> repository.save(entity.withStatus(STATUS_CLOSED).withAutoResolved(true)));
	}

	@Transactional(readOnly = true)
	public List<Warning> list(final String errandId) {
		return repository.findByErrandId(errandId).stream()
			.sorted(comparing(FaWarningEntity::getCreated, nullsLast(naturalOrder())))
			.map(WarningService::toWarning)
			.toList();
	}

	/** Acknowledge or close a warning (a handläggare action). Re-opening to OPEN is not allowed. */
	@Transactional
	public Warning updateStatus(final String errandId, final String warningId, final String status) {
		final var target = validateTargetStatus(status);
		final var entity = repository.findByIdAndErrandId(warningId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Warning not found on errand"));
		return toWarning(repository.save(entity.withStatus(target).withAutoResolved(false)));
	}

	private static String validateTargetStatus(final String status) {
		if (!STATUS_ACKNOWLEDGED.equals(status) && !STATUS_CLOSED.equals(status)) {
			throw Problem.valueOf(BAD_REQUEST, "status must be ACKNOWLEDGED or CLOSED");
		}
		return status;
	}

	/** A stable dedup/grouping key for the income a warning concerns — the förmån/type before any " (..." or ": ...". */
	private static String sourceKey(final String text) {
		return (text == null) ? "" : text.split("[(:]", 2)[0].trim();
	}

	private static String key(final String type, final String sourceKey) {
		return type + "::" + (sourceKey == null ? "" : sourceKey);
	}

	private static List<String> ofList(final List<String> list) {
		return (list == null) ? List.of() : list;
	}

	private static Warning toWarning(final FaWarningEntity entity) {
		return Warning.create()
			.withId(entity.getId())
			.withType(entity.getType())
			.withSourceKey(entity.getSourceKey())
			.withMessage(entity.getMessage())
			.withStatus(entity.getStatus())
			.withAutoResolved(entity.isAutoResolved())
			.withCreated(entity.getCreated())
			.withUpdated(entity.getUpdated());
	}
}
