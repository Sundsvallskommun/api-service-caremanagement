package se.sundsvall.caremanagement.lifecare.service.model;

import java.util.List;

/**
 * Whether this month's normberäkning covers every income type the previous month's had. {@code informationComplete} is
 * false while SSBTEK data is still missing; {@code missingIncomeTypes} lists the previous-month income types not yet
 * present, which the EB process polls SSBTEK daily to fill.
 */
public record Completeness(
	boolean informationComplete,
	List<String> missingIncomeTypes) {
}
