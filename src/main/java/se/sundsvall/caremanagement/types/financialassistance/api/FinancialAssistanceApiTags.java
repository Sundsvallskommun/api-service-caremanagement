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
	static final String CALCULATION_DESC = "The calculation: prepare the calculation draft each daily loop (no Lifecare write — Draken saves the normberäkning in Lifecare), and read or edit the draft header.";
	static final String DRAFT_ROWS = "Financial Assistance · Draft rows";
	static final String DRAFT_ROWS_DESC = "Caseworker edits to the draft calculation rows — add, edit, soft-delete and restore income, expense and person rows. Each touches only the caseworker value / note / soft-delete; the process columns are owned by the daily prepare.";
	static final String WARNINGS = "Financial Assistance · Warnings";
	static final String WARNINGS_DESC = "Acknowledgeable financial assistance income warnings on an errand — create, list and set status (OPEN / ACKNOWLEDGED / CLOSED). The daily prepare step reconciles them.";
	static final String APPROVALS = "Financial Assistance · Approvals";
	static final String APPROVALS_DESC = "DEPRECATED - being retired. Caseworker check-offs of the three financial assistance view sections (CALCULATION / PAYMENT / DECISION). They no longer gate finalize: whether the normberäkning is final, the beslut locked and the payment registered is Lifecare's status, read from Lifecare. Kept, with their stored rows, only until Draken stops reading and setting them.";
	static final String FINALIZE = "Financial Assistance · Finalize";
	static final String FINALIZE_DESC = "Besluta och utbetala: record the caseworker's decision and resume the process. The Lifecare writes (normberäkning, beslut, payments, bevakningar, journal, documents) are made by Draken's BFF directly in Lifecare before the decision; the errand only carries the references (lifecareCalculationId, lifecareDecisionId, lifecarePaymentIds). The decision is sent to the applicant by the frontend.";
	static final String PAYMENT = "Financial Assistance · Payment";
	static final String PAYMENT_DESC = "Read from Lifecare whether the payments a bifall is linked to have been paid out. caremanagement makes no payment and stores no payment status.";
	static final String PROPOSALS = "Financial Assistance · Proposals";
	static final String PROPOSALS_DESC = "The decision proposal (beslutsförslag) — derived on every read from the calculation draft and the applicant's Lifecare history; only the section warnings it raises are stored. Recomputed when the CALCULATION section is approved.";
	static final String SSBTEK = "Financial Assistance · SSBTEK";
	static final String SSBTEK_DESC = "Read the applicant's SSBTEK basis live — the answer per responding agency (af/csn/fk/skv/so/tns/miv), forwarded verbatim so a caseworker can see the source behind the classified incomes. Keyed by partyId (resolved to a personnummer via the citizen service); the period defaults to the three rule periods (M−2 through M). caremanagement only forwards the read and stores nothing.";
	static final String LIFECARE = "Financial Assistance · Lifecare history";
	static final String LIFECARE_DESC = "Read the applicant's case history straight from Lifecare — the calculations, decisions and documents — plus a single document's PDF content. Keyed by partyId (resolved to a personnummer via the citizen service); the period defaults to the last 24 months. caremanagement only forwards the reads.";
}
