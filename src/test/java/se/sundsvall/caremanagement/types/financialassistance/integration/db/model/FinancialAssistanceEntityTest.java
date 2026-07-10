package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FinancialAssistanceEntityTest {

	private static final OffsetDateTime ATTESTED_AT = OffsetDateTime.parse("2026-06-01T09:30:00Z");
	private static final OffsetDateTime LAST_DAILY_RUN_AT = OffsetDateTime.parse("2026-06-02T09:30:00Z");
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-03T10:00:00Z");
	private static final OffsetDateTime MODIFIED = OffsetDateTime.parse("2026-06-04T11:00:00Z");

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	// The exclusion lists mirror the entity's deliberate design, they are not padding: equals/hashCode omit the six LONG32
	// free-text description fields (see FinancialAssistanceEntity#equals), and toString prints only a compact 11-field
	// summary
	// of the 37 fields, so every other field must be excluded here.
	@Test
	void testBean() {
		MatcherAssert.assertThat(FinancialAssistanceEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("otherBenefitDescription", "livelihoodDescription", "childrenResidenceChangeDescription", "housingDescription", "housingChangeDescription", "stayDescription"),
			hasValidBeanEqualsExcluding("otherBenefitDescription", "livelihoodDescription", "childrenResidenceChangeDescription", "housingDescription", "housingChangeDescription", "stayDescription"),
			hasValidBeanToStringExcluding("periodChoice", "otherBenefitDescription", "livelihoodDescription", "hasChildrenUnder21", "childrenResidenceChanged",
				"childrenResidenceChangeDescription", "housingPersonCount", "housingRoomsPlusKitchen", "housingDescription", "housingChanged",
				"housingChangeDescription", "hasIncomes", "hasPendingBenefits", "hasAssets", "staysInMunicipality", "stayDescription",
				"attestedAt", "children", "costs", "incomes", "pendingBenefits", "assets", "persons", "plannings", "plannedActivities", "jobApplications")));
	}

	@Test
	void testBuilderMethods() {
		final var children = List.of(FaChild.create().withPartyId("20180101-1234").withFirstName("Kid"));
		final var costs = List.of(FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("5000.00")));
		final var incomes = List.of(FaIncome.create().withIncomeType("SALARY").withAmount(new BigDecimal("12000.00")));
		final var pendingBenefits = List.of(FaPendingBenefit.create().withBenefitName("BOSTADSBIDRAG").withApplicantName("Anna"));
		final var assets = List.of(FaAsset.create().withAssetCategory("VEHICLE").withValue(new BigDecimal("80000.00")));
		final var persons = List.of(FaPerson.create().withRole("APPLICANT").withPartyId("19900101-1234"));
		final var plannings = List.of(FaPlanning.create().withPerson("Anna").withPlanningType("WORK"));
		final var plannedActivities = List.of(FaPlannedActivity.create().withPerson("Anna").withActivity("Jobbsokning"));
		final var jobApplications = List.of(FaJobApplication.create().withPerson("Anna").withJobTitle("Snickare"));

		final var entity = FinancialAssistanceEntity.create()
			.withErrandId("errand-1")
			.withApplicationType("NEW")
			.withMaritalStatus("SINGLE")
			.withPeriodMonth(6)
			.withPeriodYear(2026)
			.withPeriodChoice("CURRENT_MONTH")
			.withNormType(List.of("NATIONAL_NORM"))
			.withOtherBenefitDescription("Establishment benefit")
			.withLivelihoodDescription("Soker arbete")
			.withHasChildrenUnder21(true)
			.withChildrenResidenceChanged(false)
			.withChildrenResidenceChangeDescription("Bor vaxelvis")
			.withHousingForm("RENTAL")
			.withHousingPersonCount(3)
			.withHousingRoomsPlusKitchen(3)
			.withHousingDescription("Trerumslagenhet")
			.withHousingChanged(false)
			.withHousingChangeDescription("Flyttade i maj")
			.withHasIncomes(true)
			.withHasPendingBenefits(false)
			.withHasAssets(true)
			.withStaysInMunicipality(true)
			.withStayDescription("Lives at the registered address")
			.withAttestation(true)
			.withAttestedAt(ATTESTED_AT)
			.withLastDailyRunAt(LAST_DAILY_RUN_AT)
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

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getApplicationType()).isEqualTo("NEW");
		assertThat(entity.getMaritalStatus()).isEqualTo("SINGLE");
		assertThat(entity.getPeriodMonth()).isEqualTo(6);
		assertThat(entity.getPeriodYear()).isEqualTo(2026);
		assertThat(entity.getPeriodChoice()).isEqualTo("CURRENT_MONTH");
		assertThat(entity.getNormType()).isEqualTo(List.of("NATIONAL_NORM"));
		assertThat(entity.getOtherBenefitDescription()).isEqualTo("Establishment benefit");
		assertThat(entity.getLivelihoodDescription()).isEqualTo("Soker arbete");
		assertThat(entity.getHasChildrenUnder21()).isTrue();
		assertThat(entity.getChildrenResidenceChanged()).isFalse();
		assertThat(entity.getChildrenResidenceChangeDescription()).isEqualTo("Bor vaxelvis");
		assertThat(entity.getHousingForm()).isEqualTo("RENTAL");
		assertThat(entity.getHousingPersonCount()).isEqualTo(3);
		assertThat(entity.getHousingRoomsPlusKitchen()).isEqualTo(3);
		assertThat(entity.getHousingDescription()).isEqualTo("Trerumslagenhet");
		assertThat(entity.getHousingChanged()).isFalse();
		assertThat(entity.getHousingChangeDescription()).isEqualTo("Flyttade i maj");
		assertThat(entity.getHasIncomes()).isTrue();
		assertThat(entity.getHasPendingBenefits()).isFalse();
		assertThat(entity.getHasAssets()).isTrue();
		assertThat(entity.getStaysInMunicipality()).isTrue();
		assertThat(entity.getStayDescription()).isEqualTo("Lives at the registered address");
		assertThat(entity.getAttestation()).isTrue();
		assertThat(entity.getAttestedAt()).isEqualTo(ATTESTED_AT);
		assertThat(entity.getLastDailyRunAt()).isEqualTo(LAST_DAILY_RUN_AT);
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
	void testNoDirtOnCreatedBean() {
		assertThat(FinancialAssistanceEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new FinancialAssistanceEntity()).hasAllNullFieldsOrProperties();
	}
}
