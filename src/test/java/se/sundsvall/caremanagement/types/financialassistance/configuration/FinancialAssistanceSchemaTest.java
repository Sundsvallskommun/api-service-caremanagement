package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.errandtypes.api.model.FieldDescriptor;
import se.sundsvall.caremanagement.errandtypes.service.ErrandTypeSchemaContribution;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_SUPPLEMENTARY;

class FinancialAssistanceSchemaTest {

	private final FinancialAssistanceModuleConfig config = new FinancialAssistanceModuleConfig();

	@Test
	void newSchemaCollectsTheNewOnlyFields() {
		final var contribution = config.financialAssistanceNewSchema();

		assertThat(contribution.typeSlug()).isEqualTo(SLUG_NEW);
		assertThat(contribution.applicationType()).isEqualTo("NEW");
		assertThat(names(contribution))
			.contains("maritalStatus", "periodChoice", "livelihoodDescription", "plannedActivities", "jobApplications")
			.doesNotContain("childrenResidenceChanged", "housingChanged");
		assertThat(contribution.fields()).allSatisfy(field -> assertThat(field.getAppliesTo()).contains("NEW"));
	}

	@Test
	void renewalSchemaCollectsTheRenewalOnlyFields() {
		final var contribution = config.financialAssistanceRenewalSchema();

		assertThat(contribution.typeSlug()).isEqualTo(SLUG_RENEWAL);
		assertThat(contribution.applicationType()).isEqualTo("RENEWAL");
		assertThat(names(contribution))
			.contains("childrenResidenceChanged", "housingChanged")
			.doesNotContain("periodChoice", "livelihoodDescription", "plannedActivities", "jobApplications");
		assertThat(contribution.fields()).allSatisfy(field -> assertThat(field.getAppliesTo()).contains("RENEWAL"));
	}

	@Test
	void supplementarySchemaIsTheMinimalSet() {
		final var contribution = config.financialAssistanceSupplementarySchema();

		assertThat(contribution.typeSlug()).isEqualTo(SLUG_SUPPLEMENTARY);
		assertThat(contribution.applicationType()).isEqualTo("SUPPLEMENTARY");
		assertThat(names(contribution)).containsExactlyInAnyOrder(
			"maritalStatus", "periodMonth", "periodYear", "normType", "costs", "persons", "attestation");
	}

	@Test
	void fieldsCarryTheirTypeMetadata() {
		final var fields = config.financialAssistanceNewSchema().fields();

		final var maritalStatus = field(fields, "maritalStatus");
		assertThat(maritalStatus.getType()).isEqualTo("ENUM");
		assertThat(maritalStatus.isRequired()).isTrue();
		assertThat(maritalStatus.getOptions()).containsExactly("SINGLE", "COHABITING");

		final var costs = field(fields, "costs");
		assertThat(costs.getType()).isEqualTo("ARRAY");
		assertThat(costs.getItemsRef()).isEqualTo("Cost");

		final var otherBenefit = field(fields, "otherBenefitDescription");
		assertThat(otherBenefit.isRequired()).isFalse();
		assertThat(otherBenefit.getCondition()).isEqualTo("periodChoice == OTHER_BENEFIT");
	}

	private static List<String> names(final ErrandTypeSchemaContribution contribution) {
		return contribution.fields().stream().map(FieldDescriptor::getName).toList();
	}

	private static FieldDescriptor field(final List<FieldDescriptor> fields, final String name) {
		return fields.stream().filter(field -> name.equals(field.getName())).findFirst().orElseThrow();
	}
}
