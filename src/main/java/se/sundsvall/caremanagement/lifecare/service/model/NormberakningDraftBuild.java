package se.sundsvall.caremanagement.lifecare.service.model;

import java.util.List;

/**
 * The result of building a draft normberäkning from classified incomes without writing to Lifecare: the FC income rows
 * (for the editable draft) plus the completeness verdict against the previous month's normberäkning.
 *
 * @param rows                the computed FC income rows
 * @param informationComplete whether every previous-month income type is present this month
 * @param missingIncomeTypes  the previous-month income types not yet present this month
 */
public record NormberakningDraftBuild(List<DraftRow> rows, boolean informationComplete, List<String> missingIncomeTypes) {
}
