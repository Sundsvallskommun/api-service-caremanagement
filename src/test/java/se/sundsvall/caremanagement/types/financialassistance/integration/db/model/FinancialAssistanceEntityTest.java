package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialAssistanceEntityTest {

	private static final OffsetDateTime ATTESTED_AT = OffsetDateTime.parse("2026-06-01T09:30:00Z");
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-03T10:00:00Z");
	private static final OffsetDateTime MODIFIED = OffsetDateTime.parse("2026-06-04T11:00:00Z");

	@Test
	void builderMethods() {
		final var children = List.of(FaChild.create().withPersonalNumber("20180101-1234").withFirstName("Kid"));
		final var costs = List.of(FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("5000.00")));
		final var incomes = List.of(FaIncome.create().withIncomeType("SALARY").withAmount(new BigDecimal("12000.00")));
		final var pendingBenefits = List.of(FaPendingBenefit.create().withBenefitName("BOSTADSBIDRAG").withApplicantName("Anna"));
		final var assets = List.of(FaAsset.create().withAssetCategory("VEHICLE").withValue(new BigDecimal("80000.00")));
		final var persons = List.of(FaPerson.create().withRole("APPLICANT").withPersonalNumber("19900101-1234"));
		final var plannings = List.of(FaPlanning.create().withPerson("Anna").withPlanningType("WORK"));
		final var plannedActivities = List.of(FaPlannedActivity.create().withPerson("Anna").withActivity("Jobbsökning"));
		final var jobApplications = List.of(FaJobApplication.create().withPerson("Anna").withJobTitle("Snickare"));

		final var entity = FinancialAssistanceEntity.create()
			.withErrandId("errand-1")
			.withApplicationType("NEW")
			.withMaritalStatus("SINGLE")
			.withPeriodMonth(6)
			.withPeriodYear(2026)
			.withPeriodChoice("CURRENT_MONTH")
			.withNormType("RIKSNORM")
			.withOtherBenefitDescription("Etableringsersättning")
			.withLivelihoodDescription("Söker arbete")
			.withHasChildrenUnder21(true)
			.withChildrenResidenceChanged(false)
			.withChildrenResidenceChangeDescription("Bor växelvis")
			.withHousingForm("RENTAL")
			.withHousingAdultsCount(2)
			.withHousingChildrenCount(1)
			.withHousingRoomsPlusKitchen(3)
			.withHousingDescription("Trerumslägenhet")
			.withHousingChanged(false)
			.withHousingChangeDescription("Flyttade i maj")
			.withHasIncomes(true)
			.withHasPendingBenefits(false)
			.withHasAssets(true)
			.withStaysInMunicipality(true)
			.withStayDescription("Bor på folkbokföringsadressen")
			.withAttestation(true)
			.withAttestedAt(ATTESTED_AT)
			.withChildren(children)
			.withCosts(costs)
			.withIncomes(incomes)
			.withPendingBenefits(pendingBenefits)
			.withAssets(assets)
			.withPersons(persons)
			.withPlannings(plannings)
			.withPlannedActivities(plannedActivities)
			.withJobApplications(jobApplications)
			.withCreated(CREATED)
			.withModified(MODIFIED);

		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getApplicationType()).isEqualTo("NEW");
		assertThat(entity.getMaritalStatus()).isEqualTo("SINGLE");
		assertThat(entity.getPeriodMonth()).isEqualTo(6);
		assertThat(entity.getPeriodYear()).isEqualTo(2026);
		assertThat(entity.getPeriodChoice()).isEqualTo("CURRENT_MONTH");
		assertThat(entity.getNormType()).isEqualTo("RIKSNORM");
		assertThat(entity.getOtherBenefitDescription()).isEqualTo("Etableringsersättning");
		assertThat(entity.getLivelihoodDescription()).isEqualTo("Söker arbete");
		assertThat(entity.getHasChildrenUnder21()).isTrue();
		assertThat(entity.getChildrenResidenceChanged()).isFalse();
		assertThat(entity.getChildrenResidenceChangeDescription()).isEqualTo("Bor växelvis");
		assertThat(entity.getHousingForm()).isEqualTo("RENTAL");
		assertThat(entity.getHousingAdultsCount()).isEqualTo(2);
		assertThat(entity.getHousingChildrenCount()).isEqualTo(1);
		assertThat(entity.getHousingRoomsPlusKitchen()).isEqualTo(3);
		assertThat(entity.getHousingDescription()).isEqualTo("Trerumslägenhet");
		assertThat(entity.getHousingChanged()).isFalse();
		assertThat(entity.getHousingChangeDescription()).isEqualTo("Flyttade i maj");
		assertThat(entity.getHasIncomes()).isTrue();
		assertThat(entity.getHasPendingBenefits()).isFalse();
		assertThat(entity.getHasAssets()).isTrue();
		assertThat(entity.getStaysInMunicipality()).isTrue();
		assertThat(entity.getStayDescription()).isEqualTo("Bor på folkbokföringsadressen");
		assertThat(entity.getAttestation()).isTrue();
		assertThat(entity.getAttestedAt()).isEqualTo(ATTESTED_AT);
		assertThat(entity.getChildren()).isSameAs(children);
		assertThat(entity.getCosts()).isSameAs(costs);
		assertThat(entity.getIncomes()).isSameAs(incomes);
		assertThat(entity.getPendingBenefits()).isSameAs(pendingBenefits);
		assertThat(entity.getAssets()).isSameAs(assets);
		assertThat(entity.getPersons()).isSameAs(persons);
		assertThat(entity.getPlannings()).isSameAs(plannings);
		assertThat(entity.getPlannedActivities()).isSameAs(plannedActivities);
		assertThat(entity.getJobApplications()).isSameAs(jobApplications);
		assertThat(entity.getCreated()).isEqualTo(CREATED);
		assertThat(entity.getModified()).isEqualTo(MODIFIED);
	}

	@Test
	void settersWork() {
		final var children = List.of(FaChild.create().withFirstName("Kid"));
		final var costs = List.of(FaCost.create().withCostType("RENT"));
		final var incomes = List.of(FaIncome.create().withIncomeType("SALARY"));
		final var pendingBenefits = List.of(FaPendingBenefit.create().withBenefitName("BOSTADSBIDRAG"));
		final var assets = List.of(FaAsset.create().withAssetCategory("VEHICLE"));
		final var persons = List.of(FaPerson.create().withRole("APPLICANT"));
		final var plannings = List.of(FaPlanning.create().withPlanningType("WORK"));
		final var plannedActivities = List.of(FaPlannedActivity.create().withActivity("Jobbsökning"));
		final var jobApplications = List.of(FaJobApplication.create().withJobTitle("Snickare"));

		final var entity = new FinancialAssistanceEntity();
		entity.setErrandId("errand-2");
		entity.setApplicationType("RENEWAL");
		entity.setMaritalStatus("COHABITING");
		entity.setPeriodMonth(5);
		entity.setPeriodYear(2026);
		entity.setPeriodChoice("NEXT_MONTH");
		entity.setNormType("OTHER_NORM");
		entity.setOtherBenefitDescription("Skuld");
		entity.setLivelihoodDescription("Arbetslös");
		entity.setHasChildrenUnder21(false);
		entity.setChildrenResidenceChanged(true);
		entity.setChildrenResidenceChangeDescription("Flytt");
		entity.setHousingForm("SUBLET");
		entity.setHousingAdultsCount(1);
		entity.setHousingChildrenCount(0);
		entity.setHousingRoomsPlusKitchen(2);
		entity.setHousingDescription("Andrahand");
		entity.setHousingChanged(true);
		entity.setHousingChangeDescription("Ny adress");
		entity.setHasIncomes(true);
		entity.setHasPendingBenefits(true);
		entity.setHasAssets(false);
		entity.setStaysInMunicipality(false);
		entity.setStayDescription("Utomlands del av månaden");
		entity.setAttestation(true);
		entity.setAttestedAt(ATTESTED_AT);
		entity.setChildren(children);
		entity.setCosts(costs);
		entity.setIncomes(incomes);
		entity.setPendingBenefits(pendingBenefits);
		entity.setAssets(assets);
		entity.setPersons(persons);
		entity.setPlannings(plannings);
		entity.setPlannedActivities(plannedActivities);
		entity.setJobApplications(jobApplications);
		entity.setCreated(CREATED);
		entity.setModified(MODIFIED);

		assertThat(entity.getErrandId()).isEqualTo("errand-2");
		assertThat(entity.getApplicationType()).isEqualTo("RENEWAL");
		assertThat(entity.getMaritalStatus()).isEqualTo("COHABITING");
		assertThat(entity.getPeriodMonth()).isEqualTo(5);
		assertThat(entity.getPeriodYear()).isEqualTo(2026);
		assertThat(entity.getPeriodChoice()).isEqualTo("NEXT_MONTH");
		assertThat(entity.getNormType()).isEqualTo("OTHER_NORM");
		assertThat(entity.getOtherBenefitDescription()).isEqualTo("Skuld");
		assertThat(entity.getLivelihoodDescription()).isEqualTo("Arbetslös");
		assertThat(entity.getHasChildrenUnder21()).isFalse();
		assertThat(entity.getChildrenResidenceChanged()).isTrue();
		assertThat(entity.getChildrenResidenceChangeDescription()).isEqualTo("Flytt");
		assertThat(entity.getHousingForm()).isEqualTo("SUBLET");
		assertThat(entity.getHousingAdultsCount()).isEqualTo(1);
		assertThat(entity.getHousingChildrenCount()).isZero();
		assertThat(entity.getHousingRoomsPlusKitchen()).isEqualTo(2);
		assertThat(entity.getHousingDescription()).isEqualTo("Andrahand");
		assertThat(entity.getHousingChanged()).isTrue();
		assertThat(entity.getHousingChangeDescription()).isEqualTo("Ny adress");
		assertThat(entity.getHasIncomes()).isTrue();
		assertThat(entity.getHasPendingBenefits()).isTrue();
		assertThat(entity.getHasAssets()).isFalse();
		assertThat(entity.getStaysInMunicipality()).isFalse();
		assertThat(entity.getStayDescription()).isEqualTo("Utomlands del av månaden");
		assertThat(entity.getAttestation()).isTrue();
		assertThat(entity.getAttestedAt()).isEqualTo(ATTESTED_AT);
		assertThat(entity.getChildren()).isSameAs(children);
		assertThat(entity.getCosts()).isSameAs(costs);
		assertThat(entity.getIncomes()).isSameAs(incomes);
		assertThat(entity.getPendingBenefits()).isSameAs(pendingBenefits);
		assertThat(entity.getAssets()).isSameAs(assets);
		assertThat(entity.getPersons()).isSameAs(persons);
		assertThat(entity.getPlannings()).isSameAs(plannings);
		assertThat(entity.getPlannedActivities()).isSameAs(plannedActivities);
		assertThat(entity.getJobApplications()).isSameAs(jobApplications);
		assertThat(entity.getCreated()).isEqualTo(CREATED);
		assertThat(entity.getModified()).isEqualTo(MODIFIED);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(FinancialAssistanceEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new FinancialAssistanceEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var a = FinancialAssistanceEntity.create().withErrandId("errand-1").withApplicationType("NEW").withPeriodYear(2026);
		final var b = FinancialAssistanceEntity.create().withErrandId("errand-1").withApplicationType("NEW").withPeriodYear(2026);
		final var c = FinancialAssistanceEntity.create().withErrandId("errand-2");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}

	@Test
	void toStringContainsKeyFields() {
		final var entity = FinancialAssistanceEntity.create().withErrandId("errand-1").withAttestedAt(ATTESTED_AT);

		assertThat(entity.toString()).contains("errand-1");
	}
}
