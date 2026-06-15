package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.sundsvall.caremanagement.core.service.registry.ErrandTypeContribution;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderRoleContribution;

/**
 * Registers the three financial assistance (EB) errand types and their stakeholder roles.
 *
 * <p>
 * Per the frontend contract there is one slug per application type — {@code financial-assistance-new} (nyansökan),
 * {@code financial-assistance-renewal} (återansökan) and {@code financial-assistance-supplementary} (tilläggsansökan).
 * The three share the same data model, stakeholder roles and status lifecycle; the slug is the discriminator and the
 * service derives the stored {@code applicationType} from it.
 * </p>
 *
 * <p>
 * Status lifecycle (Swedish codes; bifall/avslag split — outcome detail lives on the {@code Decision} row, not the
 * status): {@code INKOMMEN → UNDER_BEREDNING → VANTAR_PA_BESLUT → BEVILJAD → UTBETALD → AVSLUTAD}, with
 * {@code KOMPLETTERING}, {@code AVSLAGEN} and {@code ATERKALLAD} branches. Maps onto the canonical EB BPMN:
 * UNDER_BEREDNING = recommendation write, VANTAR_PA_BESLUT = the receiveTask pause (handläggaren i loopen),
 * BEVILJAD/AVSLAGEN = the gateway after the handläggare's process-message.
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

	private static final String DISPLAY_NEW = "Ekonomiskt bistånd – nyansökan";
	private static final String DISPLAY_RENEWAL = "Ekonomiskt bistånd – återansökan";
	private static final String DISPLAY_SUPPLEMENTARY = "Ekonomiskt bistånd – tilläggsansökan";

	// Status codes
	public static final String STATUS_INKOMMEN = "INKOMMEN";
	public static final String STATUS_UNDER_BEREDNING = "UNDER_BEREDNING";
	public static final String STATUS_VANTAR_PA_BESLUT = "VANTAR_PA_BESLUT";
	public static final String STATUS_KOMPLETTERING = "KOMPLETTERING";
	public static final String STATUS_BEVILJAD = "BEVILJAD";
	public static final String STATUS_AVSLAGEN = "AVSLAGEN";
	public static final String STATUS_UTBETALD = "UTBETALD";
	public static final String STATUS_AVSLUTAD = "AVSLUTAD";
	public static final String STATUS_ATERKALLAD = "ATERKALLAD";

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

	/** Identical status lifecycle for every EB slug — only the slug and display name differ. */
	private static ErrandTypeContribution typeContribution(final String slug, final String displayName) {
		return ErrandTypeContribution.builder(slug)
			.displayName(displayName)
			.allowedStatuses(STATUS_INKOMMEN, STATUS_UNDER_BEREDNING, STATUS_VANTAR_PA_BESLUT, STATUS_KOMPLETTERING,
				STATUS_BEVILJAD, STATUS_AVSLAGEN, STATUS_UTBETALD, STATUS_AVSLUTAD, STATUS_ATERKALLAD)
			.allowedTransition(STATUS_INKOMMEN, STATUS_UNDER_BEREDNING, STATUS_ATERKALLAD)
			.allowedTransition(STATUS_UNDER_BEREDNING, STATUS_VANTAR_PA_BESLUT, STATUS_KOMPLETTERING)
			.allowedTransition(STATUS_KOMPLETTERING, STATUS_UNDER_BEREDNING, STATUS_VANTAR_PA_BESLUT, STATUS_ATERKALLAD)
			.allowedTransition(STATUS_VANTAR_PA_BESLUT, STATUS_BEVILJAD, STATUS_AVSLAGEN, STATUS_KOMPLETTERING)
			.allowedTransition(STATUS_BEVILJAD, STATUS_UTBETALD)
			.allowedTransition(STATUS_UTBETALD, STATUS_AVSLUTAD)
			.allowedTransition(STATUS_AVSLAGEN, STATUS_AVSLUTAD)
			.allowedTransition(STATUS_ATERKALLAD, STATUS_AVSLUTAD)
			.build();
	}

	/** Identical roles for every EB slug. */
	private static StakeholderRoleContribution roleContribution(final String slug) {
		return new StakeholderRoleContribution(slug, Set.of(
			new RoleDefinition(ROLE_APPLICANT, "Sökande", 1, true),
			new RoleDefinition(ROLE_CO_APPLICANT, "Medsökande", 1, false)));
	}
}
