package se.sundsvall.caremanagement.types.financialassistance.service.model;

import java.util.List;

/**
 * What the daily refresh changed in the draft, per section — the labels of rows the process newly added and of system
 * rows that disappeared from the source. The caller turns these into NEW_* / *_DROPPED warnings.
 */
public record DraftChanges(
	List<String> addedIncomes,
	List<String> droppedIncomes,
	List<String> addedExpenses,
	List<String> droppedExpenses,
	List<String> addedPersons,
	List<String> droppedPersons) {
}
