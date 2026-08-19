package se.sundsvall.caremanagement.types.financialassistance.api;

/**
 * Swagger tag names + descriptions for the financial assistance API, shared by the per-workflow resources it is split
 * across. They share the "Financial Assistance ·" prefix, so springdoc's alpha tagsSorter keeps them clustered together
 * while still splitting the flat operation list into navigable sub-sections.
 */
final class FinancialAssistanceApiTags {

	private FinancialAssistanceApiTags() {}

	static final String ERRANDS = "Financial Assistance · Errands";
	static final String ERRANDS_DESC = "Create, read and replace financial assistance errands. Create against one of the three application-type slugs (financial-assistance-new / -renewal / -supplementary); read and replace the typed data via the shared financial-assistance path.";
	static final String INTAKE = "Financial Assistance · Intake";
	static final String INTAKE_DESC = "Pre-application and case-intake calls: eligibility routing (common entry point), renewal pre-fill from Lifecare, the income/cost type metadata catalogue, and Lifecare actualisation (case intake).";
	static final String CALCULATION = "Financial Assistance · Calculation";
	static final String CALCULATION_DESC = "The calculation: prepare the calculation each daily loop (no Lifecare write), commit it to Lifecare after a decision, and read or edit the draft header.";
	static final String DRAFT_ROWS = "Financial Assistance · Draft rows";
	static final String DRAFT_ROWS_DESC = "Caseworker edits to the draft calculation rows — add, edit, soft-delete and restore income, expense and person rows. Each touches only the caseworker value / note / soft-delete; the process columns are owned by the daily prepare.";
	static final String WARNINGS = "Financial Assistance · Warnings";
	static final String WARNINGS_DESC = "Acknowledgeable financial assistance income warnings on an errand — create, list and set status (OPEN / ACKNOWLEDGED / CLOSED). The daily prepare step reconciles them.";
	static final String APPROVALS = "Financial Assistance · Approvals";
	static final String APPROVALS_DESC = "Caseworker approval state of the three financial assistance view sections (CALCULATION / PAYMENT / DECISION) — read all three, or set/withdraw one.";
	static final String PAYMENT = "Financial Assistance · Payment";
	static final String PAYMENT_DESC = "Read whether the manual Lifecare payment for the applicant and application month has been effectuated. caremanagement makes no payment itself.";
	static final String LIFECARE = "Financial Assistance · Lifecare history";
	static final String LIFECARE_DESC = "Read the applicant's case history straight from Lifecare — the calculations, decisions and documents — plus a single document's PDF content. Keyed by partyId (resolved to a personnummer via the citizen service); the period defaults to the last 24 months. caremanagement only forwards the reads.";
}
