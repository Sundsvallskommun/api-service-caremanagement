package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.List;
import se.sundsvall.caremanagement.errandtypes.api.model.DecisionOption;
import se.sundsvall.caremanagement.errandtypes.api.model.FieldDescriptor;
import se.sundsvall.caremanagement.errandtypes.service.ErrandTypeSchemaContribution;

import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.APPLICATION_TYPE_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.APPLICATION_TYPE_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.APPLICATION_TYPE_SUPPLEMENTARY;

/**
 * The financial assistance form-field catalogue: the superset of {@code data.*} fields the three application types
 * collect, each tagged
 * with the application types that use it. {@link #contribution(String, String)} produces the per-slug
 * {@link ErrandTypeSchemaContribution} (the fields filtered to one application type), which
 * {@link FinancialAssistanceModuleConfig} registers as beans for the {@code /errand-types} endpoint.
 *
 * <p>
 * This is form guidance — the server stores the superset and does not enforce per-type requiredness. The matrix mirrors
 * {@code FinancialAssistanceData}; {@code applicationType} (server-derived from the slug) and {@code attestedAt} (a
 * server timestamp) are deliberately omitted from the collectable fields.
 * </p>
 */
final class FinancialAssistanceSchema {

	private FinancialAssistanceSchema() {}

	private static final String TYPE_INTEGER = "INTEGER";
	private static final String TYPE_STRING = "STRING";
	private static final String TYPE_BOOLEAN = "BOOLEAN";

	private static final List<String> NRS = List.of(APPLICATION_TYPE_NEW, APPLICATION_TYPE_RENEWAL, APPLICATION_TYPE_SUPPLEMENTARY);
	private static final List<String> NR = List.of(APPLICATION_TYPE_NEW, APPLICATION_TYPE_RENEWAL);
	private static final List<String> N = List.of(APPLICATION_TYPE_NEW);
	private static final List<String> R = List.of(APPLICATION_TYPE_RENEWAL);

	/**
	 * The allowed decision alternatives for every financial assistance type (the Decision-form outcome dropdown).
	 * {@code carriesAmount} is
	 * false
	 * for the outcomes that imply a 0 belopp (avslag/avvisning), which the frontend uses to zero the amount.
	 */
	private static final List<DecisionOption> DECISION_OPTIONS = List.of(
		decisionOption("BIFALL", "Bifall", true),
		decisionOption("DELAVSLAG", "Delavslag", true),
		decisionOption("AVSLAG", "Avslag", false),
		decisionOption("AVVISNING", "Avvisning", false));

	/** The superset of collectable fields, in form order. */
	private static final List<FieldDescriptor> CATALOG = List.of(
		enumField("maritalStatus", List.of("SINGLE", "COHABITING"), true, NRS, null, "Marital status of the applicant"),
		scalar("periodMonth", TYPE_INTEGER, true, NRS, null, "Month (1-12) the application period concerns"),
		scalar("periodYear", TYPE_INTEGER, true, NRS, null, "Year the application period concerns"),
		enumField("periodChoice", List.of("CURRENT_MONTH", "NEXT_MONTH", "OTHER_BENEFIT"), true, N, null, "Which period the new application concerns"),
		scalar("otherBenefitDescription", TYPE_STRING, false, N, "periodChoice == OTHER_BENEFIT", "Free-text description of the other benefit applied for"),
		enumArray("normType", List.of("NATIONAL_NORM", "OTHER_NORM"), true, NRS, null, "The norms used for the calculation"),
		scalar("livelihoodDescription", TYPE_STRING, true, N, null, "How the applicant has supported themselves"),
		scalar("hasChildrenUnder21", TYPE_BOOLEAN, true, NR, null, "Whether the household has children under 21 (gates children)"),
		array("children", "Child", NR, "hasChildrenUnder21 == true", "Children in the household"),
		scalar("childrenResidenceChanged", TYPE_BOOLEAN, true, R, null, "Whether the children's residence situation changed since the last application"),
		scalar("childrenResidenceChangeDescription", TYPE_STRING, false, R, "childrenResidenceChanged == true", "Description of the change in children's residence"),
		enumField("housingForm", List.of("NO_HOUSING_OR_INSTITUTION", "RENTAL", "SUBLET", "LODGER", "CONDOMINIUM", "OWNED_HOUSE", "RENTED_HOUSE", "LIVING_WITH_PARENTS"), false, NR, "New: required; renewal: only when housing changed",
			"The household's housing form"),
		scalar("housingPersonCount", TYPE_INTEGER, false, NR, "Per housing form", "Total persons living in the housing"),
		scalar("housingRoomsPlusKitchen", TYPE_INTEGER, false, NR, "Lodger housing form", "Number of rooms plus kitchen"),
		scalar("housingDescription", TYPE_STRING, false, NR, null, "Free-text description of the housing"),
		scalar("housingChanged", TYPE_BOOLEAN, true, R, null, "Whether the housing situation changed since the last application"),
		scalar("housingChangeDescription", TYPE_STRING, false, R, "housingChanged == true", "Description of the housing change"),
		scalar("hasIncomes", TYPE_BOOLEAN, true, NR, null, "Whether the household has incomes (gates incomes)"),
		array("incomes", "Income", NR, "hasIncomes == true", "Incomes reported by the household"),
		scalar("hasPendingBenefits", TYPE_BOOLEAN, true, NR, null, "Whether the household awaits decisions on benefits (gates pendingBenefits)"),
		array("pendingBenefits", "PendingBenefit", NR, "hasPendingBenefits == true", "Benefits the household is awaiting a decision on"),
		scalar("hasAssets", TYPE_BOOLEAN, true, NR, null, "Whether the household has assets (gates assets)"),
		array("assets", "Asset", NR, "hasAssets == true", "Assets owned by the household"),
		array("plannings", "Planning", NR, null, "Per-person planning towards self-sufficiency"),
		array("plannedActivities", "PlannedActivity", N, null, "Planned activities (e.g. AF-planering)"),
		array("jobApplications", "JobApplication", N, null, "Jobs applied for"),
		scalar("staysInMunicipality", TYPE_BOOLEAN, true, NR, null, "Whether the applicant stays in the municipality during the period"),
		scalar("stayDescription", TYPE_STRING, false, NR, "staysInMunicipality == false", "Description of why the applicant stays elsewhere"),
		array("costs", "Cost", NRS, null, "Costs applied for"),
		array("persons", "Person", NRS, null, "Applicant and optional co-applicant, including payment details"),
		scalar("attestation", TYPE_BOOLEAN, true, NRS, null, "Applicant's attestation on heder och samvete (must be true)"));

	/** The catalogue filtered to one application type, preserving form order. */
	static List<FieldDescriptor> forApplicationType(final String applicationType) {
		return CATALOG.stream()
			.filter(field -> field.getAppliesTo().contains(applicationType))
			.toList();
	}

	/**
	 * A schema contribution for one slug — its fields filtered to the slug's application type, plus the financial
	 * assistance outcomes.
	 */
	static ErrandTypeSchemaContribution contribution(final String typeSlug, final String applicationType) {
		return new Contribution(typeSlug, applicationType, forApplicationType(applicationType), DECISION_OPTIONS);
	}

	private record Contribution(String typeSlug, String applicationType, List<FieldDescriptor> fields, List<DecisionOption> decisionOptions)
		implements
		ErrandTypeSchemaContribution {}

	private static DecisionOption decisionOption(final String code, final String displayName, final boolean carriesAmount) {
		return DecisionOption.create()
			.withCode(code)
			.withDisplayName(displayName)
			.withCarriesAmount(carriesAmount);
	}

	private static FieldDescriptor scalar(final String name, final String type, final boolean required,
		final List<String> appliesTo, final String condition, final String description) {
		return FieldDescriptor.create()
			.withName(name)
			.withType(type)
			.withRequired(required)
			.withAppliesTo(appliesTo)
			.withCondition(condition)
			.withDescription(description);
	}

	private static FieldDescriptor enumField(final String name, final List<String> options, final boolean required,
		final List<String> appliesTo, final String condition, final String description) {
		return scalar(name, "ENUM", required, appliesTo, condition, description)
			.withOptions(options);
	}

	/**
	 * A multi-select enum field — an {@code ARRAY} whose element values are drawn from {@code options} (no
	 * {@code itemsRef}).
	 */
	private static FieldDescriptor enumArray(final String name, final List<String> options, final boolean required,
		final List<String> appliesTo, final String condition, final String description) {
		return scalar(name, "ARRAY", required, appliesTo, condition, description)
			.withOptions(options);
	}

	private static FieldDescriptor array(final String name, final String itemsRef, final List<String> appliesTo,
		final String condition, final String description) {
		return FieldDescriptor.create()
			.withName(name)
			.withType("ARRAY")
			.withRequired(false)
			.withItemsRef(itemsRef)
			.withAppliesTo(appliesTo)
			.withCondition(condition)
			.withDescription(description);
	}
}
