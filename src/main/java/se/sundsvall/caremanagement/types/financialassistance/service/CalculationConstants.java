package se.sundsvall.caremanagement.types.financialassistance.service;

/**
 * Shared string codes for the calculation sections — row provenance (who may write which column) and the recipient /
 * role enumerations. Kept as {@code String} (not enums) to match the module's API + entity convention; the process path
 * stamps {@link #ORIGIN_SYSTEM}, the Draken edit path {@link #ORIGIN_CASEWORKER}.
 */
public final class CalculationConstants {

	private CalculationConstants() {}

	public static final String ORIGIN_SYSTEM = "SYSTEM";
	public static final String ORIGIN_CASEWORKER = "CASEWORKER";

	public static final String RECIPIENT_APPLICANT = "APPLICANT";
	public static final String RECIPIENT_CO_APPLICANT = "CO_APPLICANT";

	public static final String ROLE_APPLICANT = "APPLICANT";
	public static final String ROLE_CO_APPLICANT = "CO_APPLICANT";
	public static final String ROLE_CHILD = "CHILD";
	public static final String ROLE_VISITATION_CHILD = "VISITATION_CHILD";

	// Expense buckets — which FC array the cost posts to (and which GUI tab it shows on).
	public static final String BUCKET_EXPENSE = "EXPENSE";                 // EXPENSES → CalculationExpenses
	public static final String BUCKET_SPECIAL_EXPENSE = "SPECIAL_EXPENSE"; // OTHER LIVING COSTS → CalculationSpecialExpenses
}
