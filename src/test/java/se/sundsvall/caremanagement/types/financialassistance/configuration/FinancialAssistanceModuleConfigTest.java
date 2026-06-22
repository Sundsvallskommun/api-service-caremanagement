package se.sundsvall.caremanagement.types.financialassistance.configuration;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.core.service.registry.ErrandTypeContribution;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderRoleContribution;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;

class FinancialAssistanceModuleConfigTest {

	private final FinancialAssistanceModuleConfig config = new FinancialAssistanceModuleConfig();

	@Test
	void registersThreeTypesWithSharedLifecycle() {
		assertType(config.financialAssistanceNewType(), SLUG_NEW, "Financial assistance – new application");
		assertType(config.financialAssistanceRenewalType(), SLUG_RENEWAL, "Financial assistance – renewal");
		assertType(config.financialAssistanceSupplementaryType(), SLUG_SUPPLEMENTARY, "Financial assistance – supplementary application");
	}

	private static void assertType(final ErrandTypeContribution contribution, final String slug, final String displayName) {
		assertThat(contribution.typeSlug()).isEqualTo(slug);
		assertThat(contribution.displayName()).isEqualTo(displayName);
		assertThat(contribution.allowedStatuses()).containsExactlyInAnyOrder(
			"RECEIVED", "UNDER_REVIEW", "AWAITING_DECISION", "SUPPLEMENT_REQUESTED", "GRANTED",
			"REJECTED", "PAID", "CLOSED", "WITHDRAWN");
		assertThat(contribution.isValidStatus("RECEIVED")).isTrue();
		assertThat(contribution.isValidStatus("BOGUS")).isFalse();
		assertThat(contribution.isValidTransition("RECEIVED", "UNDER_REVIEW")).isTrue();
		assertThat(contribution.isValidTransition("GRANTED", "PAID")).isTrue();
		assertThat(contribution.isValidTransition("RECEIVED", "PAID")).isFalse();
	}

	@Test
	void registersApplicantRolesForEverySlug() {
		assertRoles(config.financialAssistanceNewRoles(), SLUG_NEW);
		assertRoles(config.financialAssistanceRenewalRoles(), SLUG_RENEWAL);
		assertRoles(config.financialAssistanceSupplementaryRoles(), SLUG_SUPPLEMENTARY);
	}

	private static void assertRoles(final StakeholderRoleContribution contribution, final String slug) {
		assertThat(contribution.typeSlug()).isEqualTo(slug);
		assertThat(contribution.roles())
			.hasSize(2)
			.extracting(RoleDefinition::code)
			.containsExactlyInAnyOrder("APPLICANT", "CO_APPLICANT");
	}

	@Test
	void mapsEachSlugToItsApplicationType() {
		assertThat(FinancialAssistanceModuleConfig.applicationTypeForSlug(SLUG_NEW)).isEqualTo("NEW");
		assertThat(FinancialAssistanceModuleConfig.applicationTypeForSlug(SLUG_RENEWAL)).isEqualTo("RENEWAL");
		assertThat(FinancialAssistanceModuleConfig.applicationTypeForSlug(SLUG_SUPPLEMENTARY)).isEqualTo("SUPPLEMENTARY");
	}
}
