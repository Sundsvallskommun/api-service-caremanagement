package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.sundsvall.caremanagement.core.service.registry.ErrandTypeContribution;
import se.sundsvall.caremanagement.errandtypes.service.ErrandTypeSchemaContribution;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderRoleContribution;

/**
 * Registers the three financial assistance (EB) errand types and their stakeholder roles.
 *
 * <p>
 * Per the frontend contract there is one slug per application type — {@code financial-assistance-new} (new
 * application),
 * {@code financial-assistance-renewal} (renewal) and {@code financial-assistance-supplementary} (supplementary
 * application).
 * The three share the same data model, stakeholder roles and status lifecycle; the slug is the discriminator and the
 * service derives the stored {@code applicationType} from it.
 * </p>
 *
 * <p>
 * Status lifecycle (Swedish codes; bifall/avslag split — outcome detail lives on the {@code Decision} row, not the
 * status): {@code RECEIVED → UNDER_REVIEW → AWAITING_DECISION → GRANTED → PAID → CLOSED}, with
 * {@code SUPPLEMENT_REQUESTED}, {@code REJECTED} and {@code WITHDRAWN} branches. Maps onto the canonical EB BPMN:
 * UNDER_REVIEW = recommendation write, AWAITING_DECISION = the receiveTask pause (caseworkern i loopen),
 * GRANTED/REJECTED = the gateway after the caseworker's process-message.
 * </p>
 */
@Configuration
public class FinancialAssistanceModuleConfig {

	// Type slugs (one per application type)
	public static final String SLUG_NEW = "financial-assistance-new";
	public static final String SLUG_RENEWAL = "financial-assistance-renewal";
	public static final String SLUG_SUPPLEMENTARY = "financial-assistance-supplementary";

	// Application types stored on the row (derived from the slug)
	public static final String APPLICATION_TYPE_NEW = "NEW";
	public static final String APPLICATION_TYPE_RENEWAL = "RENEWAL";
	public static final String APPLICATION_TYPE_SUPPLEMENTARY = "SUPPLEMENTARY";

	/** Slug → applicationType, and the set of valid slugs (also the path-variable regex in the resource). */
	public static final Map<String, String> SLUG_TO_APPLICATION_TYPE = Map.of(
		SLUG_NEW, APPLICATION_TYPE_NEW,
		SLUG_RENEWAL, APPLICATION_TYPE_RENEWAL,
		SLUG_SUPPLEMENTARY, APPLICATION_TYPE_SUPPLEMENTARY);

	public static final Set<String> SLUGS = SLUG_TO_APPLICATION_TYPE.keySet();

	private static final String DISPLAY_NEW = "Financial assistance – new application";
	private static final String DISPLAY_RENEWAL = "Financial assistance – renewal";
	private static final String DISPLAY_SUPPLEMENTARY = "Financial assistance – supplementary application";

	// Status codes
	public static final String STATUS_RECEIVED = "RECEIVED";
	/**
	 * A freshly created re-application that hit the recently-closed guard — frozen for a caseworker to reopen + release.
	 */
	public static final String STATUS_NEEDS_MANUAL_REVIEW = "NEEDS_MANUAL_REVIEW";
	public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
	public static final String STATUS_AWAITING_DECISION = "AWAITING_DECISION";
	public static final String STATUS_SUPPLEMENT_REQUESTED = "SUPPLEMENT_REQUESTED";
	public static final String STATUS_GRANTED = "GRANTED";
	public static final String STATUS_REJECTED = "REJECTED";
	public static final String STATUS_PAID = "PAID";
	public static final String STATUS_CLOSED = "CLOSED";
	public static final String STATUS_WITHDRAWN = "WITHDRAWN";

	// Status display names (Swedish — the labels Draken shows the handläggare)
	private static final String DISPLAY_RECEIVED = "Inkommen";
	private static final String DISPLAY_NEEDS_MANUAL_REVIEW = "Kräver manuell granskning";
	private static final String DISPLAY_UNDER_REVIEW = "Under utredning";
	private static final String DISPLAY_SUPPLEMENT_REQUESTED = "Komplettering begärd";
	private static final String DISPLAY_AWAITING_DECISION = "Väntar på beslut";
	private static final String DISPLAY_GRANTED = "Beviljad";
	private static final String DISPLAY_REJECTED = "Avslagen";
	private static final String DISPLAY_PAID = "Utbetald";
	private static final String DISPLAY_WITHDRAWN = "Återtagen";
	private static final String DISPLAY_CLOSED = "Avslutad";

	// Stakeholder roles
	public static final String ROLE_APPLICANT = "APPLICANT";
	public static final String ROLE_CO_APPLICANT = "CO_APPLICANT";

	/** The applicationType to store for a given slug. */
	public static String applicationTypeForSlug(final String slug) {
		return SLUG_TO_APPLICATION_TYPE.get(slug);
	}

	@Bean
	ErrandTypeContribution financialAssistanceNewType() {
		return typeContribution(SLUG_NEW, DISPLAY_NEW);
	}

	@Bean
	ErrandTypeContribution financialAssistanceRenewalType() {
		return typeContribution(SLUG_RENEWAL, DISPLAY_RENEWAL);
	}

	@Bean
	ErrandTypeContribution financialAssistanceSupplementaryType() {
		return typeContribution(SLUG_SUPPLEMENTARY, DISPLAY_SUPPLEMENTARY);
	}

	@Bean
	StakeholderRoleContribution financialAssistanceNewRoles() {
		return roleContribution(SLUG_NEW);
	}

	@Bean
	StakeholderRoleContribution financialAssistanceRenewalRoles() {
		return roleContribution(SLUG_RENEWAL);
	}

	@Bean
	StakeholderRoleContribution financialAssistanceSupplementaryRoles() {
		return roleContribution(SLUG_SUPPLEMENTARY);
	}

	@Bean
	ErrandTypeSchemaContribution financialAssistanceNewSchema() {
		return FinancialAssistanceSchema.contribution(SLUG_NEW, APPLICATION_TYPE_NEW);
	}

	@Bean
	ErrandTypeSchemaContribution financialAssistanceRenewalSchema() {
		return FinancialAssistanceSchema.contribution(SLUG_RENEWAL, APPLICATION_TYPE_RENEWAL);
	}

	@Bean
	ErrandTypeSchemaContribution financialAssistanceSupplementarySchema() {
		return FinancialAssistanceSchema.contribution(SLUG_SUPPLEMENTARY, APPLICATION_TYPE_SUPPLEMENTARY);
	}

	/** Identical status lifecycle for every EB slug — only the slug and display name differ. */
	private static ErrandTypeContribution typeContribution(final String slug, final String displayName) {
		return ErrandTypeContribution.builder(slug)
			.displayName(displayName)
			// Declared in lifecycle order — the order the frontend renders them in
			.status(STATUS_RECEIVED, DISPLAY_RECEIVED)
			.status(STATUS_NEEDS_MANUAL_REVIEW, DISPLAY_NEEDS_MANUAL_REVIEW)
			.status(STATUS_UNDER_REVIEW, DISPLAY_UNDER_REVIEW)
			.status(STATUS_SUPPLEMENT_REQUESTED, DISPLAY_SUPPLEMENT_REQUESTED)
			.status(STATUS_AWAITING_DECISION, DISPLAY_AWAITING_DECISION)
			.status(STATUS_GRANTED, DISPLAY_GRANTED)
			.status(STATUS_REJECTED, DISPLAY_REJECTED)
			.status(STATUS_PAID, DISPLAY_PAID)
			.status(STATUS_WITHDRAWN, DISPLAY_WITHDRAWN)
			.status(STATUS_CLOSED, DISPLAY_CLOSED)
			.allowedTransition(STATUS_RECEIVED, STATUS_NEEDS_MANUAL_REVIEW, STATUS_UNDER_REVIEW, STATUS_WITHDRAWN)
			.allowedTransition(STATUS_NEEDS_MANUAL_REVIEW, STATUS_UNDER_REVIEW, STATUS_WITHDRAWN)
			.allowedTransition(STATUS_UNDER_REVIEW, STATUS_AWAITING_DECISION, STATUS_SUPPLEMENT_REQUESTED)
			.allowedTransition(STATUS_SUPPLEMENT_REQUESTED, STATUS_UNDER_REVIEW, STATUS_AWAITING_DECISION, STATUS_WITHDRAWN)
			.allowedTransition(STATUS_AWAITING_DECISION, STATUS_GRANTED, STATUS_REJECTED, STATUS_SUPPLEMENT_REQUESTED)
			.allowedTransition(STATUS_GRANTED, STATUS_PAID)
			.allowedTransition(STATUS_PAID, STATUS_CLOSED)
			.allowedTransition(STATUS_REJECTED, STATUS_CLOSED)
			.allowedTransition(STATUS_WITHDRAWN, STATUS_CLOSED)
			.build();
	}

	/** Identical roles for every EB slug. */
	private static StakeholderRoleContribution roleContribution(final String slug) {
		return new StakeholderRoleContribution(slug, Set.of(
			new RoleDefinition(ROLE_APPLICANT, "Applicant", 1, true),
			new RoleDefinition(ROLE_CO_APPLICANT, "Co-applicant", 1, false)));
	}
}
