package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Warning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FaWarningRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaWarningEntity;
import se.sundsvall.caremanagement.types.financialassistance.service.model.DraftChanges;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * financial assistance income warnings as acknowledgeable objects. The daily prepare step reconciles the current set
 * against what is
 * stored — creating new warnings, refreshing open ones, and auto-closing ones whose cause has resolved — while never
 * re-opening a warning the caseworker has already acted on. A caseworker can acknowledge or close each warning.
 */
@Service
public class WarningService {

	public static final String TYPE_UNHANDLED_INCOME = "UNHANDLED_INCOME";
	public static final String TYPE_INCOME_CHANGE = "INCOME_CHANGE";
	public static final String TYPE_MISSING_SSBTEK = "MISSING_SSBTEK";
	public static final String TYPE_NEW_INCOME = "NEW_INCOME";
	public static final String TYPE_NEW_EXPENSE = "NEW_EXPENSE";
	public static final String TYPE_NEW_PERSON = "NEW_PERSON";
	public static final String TYPE_INCOME_DROPPED = "INCOME_DROPPED";
	public static final String TYPE_HOUSEHOLD_CHANGE = "HOUSEHOLD_CHANGE";
	public static final String TYPE_HOUSING_COST_CHANGE = "HOUSING_COST_CHANGE";
	public static final String TYPE_EXPENSE_REVIEW = "EXPENSE_REVIEW";
	public static final String TYPE_EXPENSE_CAPPED = "EXPENSE_CAPPED";

	public static final String STATUS_OPEN = "OPEN";
	public static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
	public static final String STATUS_CLOSED = "CLOSED";

	/** Warning type → Swedish display name for the frontend (the machine {@code type} stays for logic). */
	private static final Map<String, String> TYPE_DISPLAY_NAME = Map.ofEntries(
		Map.entry(TYPE_UNHANDLED_INCOME, "Ej hanterad inkomst"),
		Map.entry(TYPE_INCOME_CHANGE, "Inkomständring"),
		Map.entry(TYPE_MISSING_SSBTEK, "Saknas i SSBTEK"),
		Map.entry(TYPE_NEW_INCOME, "Ny inkomst"),
		Map.entry(TYPE_NEW_EXPENSE, "Ny utgift"),
		Map.entry(TYPE_NEW_PERSON, "Ny hushållsmedlem"),
		Map.entry(TYPE_INCOME_DROPPED, "Inkomst borttagen"),
		Map.entry(TYPE_HOUSEHOLD_CHANGE, "Förändrat hushåll"),
		Map.entry(TYPE_HOUSING_COST_CHANGE, "Förändrad boendekostnad"),
		Map.entry(TYPE_EXPENSE_REVIEW, "Manuell skälighetsbedömning"),
		Map.entry(TYPE_EXPENSE_CAPPED, "Kapad kostnad"));

	/** Warning status → Swedish display name. */
	private static final Map<String, String> STATUS_DISPLAY_NAME = Map.ofEntries(
		Map.entry(STATUS_OPEN, "Öppen"),
		Map.entry(STATUS_ACKNOWLEDGED, "Kvitterad"),
		Map.entry(STATUS_CLOSED, "Stängd"));

	private final FaWarningRepository repository;

	WarningService(final FaWarningRepository repository) {
		this.repository = repository;
	}

	/** A computed warning before persistence — the dedup key is {@code type + sourceKey}. */
	public record WarningInput(String type, String sourceKey, String message) {
	}

	/**
	 * Reconcile the income warnings produced by the rules + completeness + draft checks into the errand's warning
	 * objects: unhandled / changed / still-missing incomes, plus income that has newly arrived in SSBTEK but is not in the
	 * caseworker's edited draft calculation.
	 */
	@Transactional
	public void reconcileIncomeWarnings(final String errandId, final List<String> unhandled, final List<String> changes,
		final List<String> missing, final List<String> newIncome) {
		final List<WarningInput> inputs = new ArrayList<>();
		ofList(unhandled).forEach(text -> inputs.add(new WarningInput(TYPE_UNHANDLED_INCOME, sourceKey(text), text)));
		ofList(changes).forEach(text -> inputs.add(new WarningInput(TYPE_INCOME_CHANGE, sourceKey(text), text)));
		ofList(missing).forEach(text -> inputs.add(new WarningInput(TYPE_MISSING_SSBTEK, text, "Saknas fortfarande i SSBTEK: " + text)));
		ofList(newIncome).forEach(text -> inputs.add(new WarningInput(TYPE_NEW_INCOME, text, "Ny inkomst i SSBTEK, ej införd i beräkningen: " + text)));
		reconcile(errandId, inputs);
	}

	/**
	 * Reconcile the full calculation warnings into the errand's warning objects: the rules income warnings
	 * (unhandled / changed / still-missing), the rows the daily refresh newly added (NEW_*) or saw disappear, and the
	 * section warnings the feeder pre-typed and DMN-classified — the expense rules (reasonableness review + cap) and the
	 * renewal delta (household-size + housing drift). Supersedes {@link #reconcileIncomeWarnings} once the three-section
	 * draft is in play.
	 */
	@Transactional
	public void reconcileCalculationWarnings(final String errandId, final List<String> unhandled, final List<String> changes,
		final List<String> missing, final DraftChanges draftChanges, final List<WarningInput> sectionWarnings) {

		final List<WarningInput> inputs = new ArrayList<>();
		ofList(unhandled).forEach(text -> inputs.add(new WarningInput(TYPE_UNHANDLED_INCOME, sourceKey(text), text)));
		ofList(changes).forEach(text -> inputs.add(new WarningInput(TYPE_INCOME_CHANGE, sourceKey(text), text)));
		ofList(missing).forEach(text -> inputs.add(new WarningInput(TYPE_MISSING_SSBTEK, text, "Saknas fortfarande i SSBTEK: " + text)));

		if (draftChanges != null) {
			ofList(draftChanges.addedIncomes()).forEach(text -> inputs.add(new WarningInput(TYPE_NEW_INCOME, sourceKey(text), "Ny inkomst i SSBTEK, ej införd i beräkningen: " + text)));
			ofList(draftChanges.addedExpenses()).forEach(text -> inputs.add(new WarningInput(TYPE_NEW_EXPENSE, sourceKey(text), "Ny utgift i ansökan: " + text)));
			ofList(draftChanges.addedPersons()).forEach(text -> inputs.add(new WarningInput(TYPE_NEW_PERSON, sourceKey(text), "Ny hushållsmedlem: " + text)));
			ofList(draftChanges.droppedIncomes()).forEach(text -> inputs.add(new WarningInput(TYPE_INCOME_DROPPED, sourceKey(text), "Inkomst inte längre i SSBTEK: " + text)));
		}

		ofNullable(sectionWarnings).ifPresent(inputs::addAll);
		reconcile(errandId, inputs);
	}

	/**
	 * Reconcile the errand's warnings against {@code current}: create the ones that are new, refresh the message of ones
	 * still OPEN/ACKNOWLEDGED, and auto-close ones whose cause has resolved (no longer in {@code current}). A CLOSED
	 * warning is never re-opened.
	 *
	 * <p>
	 * Package-private and intentionally not {@code @Transactional}: it is only ever invoked by the public
	 * {@code reconcile*Warnings} entry points above, which carry the transaction — a self-invoked {@code @Transactional}
	 * method would bypass the Spring proxy and silently run without one.
	 */
	void reconcile(final String errandId, final List<WarningInput> current) {
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

	/** How many warnings on the errand are still active (OPEN or ACKNOWLEDGED — i.e. not CLOSED). */
	@Transactional(readOnly = true)
	public long countActive(final String errandId) {
		return repository.countByErrandIdAndStatusNot(errandId, STATUS_CLOSED);
	}

	/**
	 * Create a warning directly on an errand — the careM temp stage, with no Lifecare round-trip. The warning is born
	 * {@code OPEN}; the {@code sourceKey} is derived from the message when not supplied (the same rule reconcile uses).
	 */
	@Transactional
	public Warning create(final String errandId, final String type, final String sourceKey, final String message) {
		return toWarning(repository.save(FaWarningEntity.create()
			.withErrandId(errandId)
			.withType(type)
			.withSourceKey(ofNullable(sourceKey).filter(StringUtils::hasText).orElseGet(() -> sourceKey(message)))
			.withMessage(message)
			.withStatus(STATUS_OPEN)
			.withAutoResolved(false)));
	}

	/**
	 * Set a warning's status (a caseworker action) — acknowledge, close, or re-open to {@code OPEN} (undo an earlier
	 * acknowledge/close). Re-opening clears the auto-resolved flag, since it is a manual action.
	 */
	@Transactional
	public Warning updateStatus(final String errandId, final String warningId, final String status) {
		final var target = validateTargetStatus(status);
		final var entity = repository.findByIdAndErrandId(warningId, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Warning not found on errand"));
		return toWarning(repository.save(entity.withStatus(target).withAutoResolved(false)));
	}

	private static String validateTargetStatus(final String status) {
		if (!STATUS_OPEN.equals(status) && !STATUS_ACKNOWLEDGED.equals(status) && !STATUS_CLOSED.equals(status)) {
			throw Problem.valueOf(BAD_REQUEST, "status must be OPEN, ACKNOWLEDGED or CLOSED");
		}
		return status;
	}

	/** A stable dedup/grouping key for the income a warning concerns — the benefit/type before any " (..." or ": ...". */
	private static String sourceKey(final String text) {
		if (text == null) {
			return "";
		}
		return text.split("[(:]", 2)[0].trim();
	}

	private static String key(final String type, final String sourceKey) {
		return type + "::" + ofNullable(sourceKey).orElse("");
	}

	private static List<String> ofList(final List<String> list) {
		return ofNullable(list).orElseGet(List::of);
	}

	private static Warning toWarning(final FaWarningEntity entity) {
		return Warning.create()
			.withId(entity.getId())
			.withType(entity.getType())
			.withTypeDisplayName(TYPE_DISPLAY_NAME.get(entity.getType()))
			.withSourceKey(entity.getSourceKey())
			.withMessage(entity.getMessage())
			.withStatus(entity.getStatus())
			.withStatusDisplayName(STATUS_DISPLAY_NAME.get(entity.getStatus()))
			.withAutoResolved(entity.isAutoResolved())
			.withCreated(entity.getCreated())
			.withUpdated(entity.getUpdated());
	}
}
