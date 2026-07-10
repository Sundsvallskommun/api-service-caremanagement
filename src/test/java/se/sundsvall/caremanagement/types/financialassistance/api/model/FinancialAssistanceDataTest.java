package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FinancialAssistanceDataTest {

	private static final OffsetDateTime ATTESTED_AT = OffsetDateTime.parse("2026-06-01T09:30:00Z");
	private static final List<Child> CHILDREN = List.of(Child.create().withFirstName("Astrid"));
	private static final List<Cost> COSTS = List.of(Cost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("5400.00")));
	private static final List<Income> INCOMES = List.of(Income.create().withIncomeType("SALARY").withAmount(new BigDecimal("18500.00")));
	private static final List<PendingBenefit> PENDING_BENEFITS = List.of(PendingBenefit.create().withBenefitName("Bostadsbidrag"));
	private static final List<Asset> ASSETS = List.of(Asset.create().withAssetCategory("VEHICLE").withValue(new BigDecimal("120000.00")));
	private static final List<Person> PERSONS = List.of(Person.create().withRole("APPLICANT"));
	private static final List<Planning> PLANNINGS = List.of(Planning.create().withPerson("APPLICANT").withPlanningType("WORK"));
	private static final List<PlannedActivity> PLANNED_ACTIVITIES = List.of(PlannedActivity.create().withPerson("APPLICANT").withActivity("Arbetsträning"));
	private static final List<JobApplication> JOB_APPLICATIONS = List.of(JobApplication.create().withPerson("APPLICANT").withJobTitle("Lagerarbetare"));

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FinancialAssistanceData.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var data = FinancialAssistanceData.create()
			.withApplicationType("NEW")
			.withMaritalStatus("SINGLE")
			.withPeriodMonth(6)
			.withPeriodYear(2026)
			.withPeriodChoice("CURRENT_MONTH")
			.withNormType(List.of("NATIONAL_NORM"))
			.withOtherBenefitDescription("Establishment benefit")
			.withLivelihoodDescription("Söker arbete")
			.withHasChildrenUnder21(true)
			.withChildrenResidenceChanged(false)
			.withChildrenResidenceChangeDescription("Barnen bor växelvis")
			.withHousingForm("RENTAL")
			.withHousingPersonCount(3)
			.withHousingRoomsPlusKitchen(3)
			.withHousingDescription("Trerumslägenhet")
			.withHousingChanged(false)
			.withHousingChangeDescription("Flyttade i maj")
			.withHasIncomes(true)
			.withHasPendingBenefits(false)
			.withHasAssets(false)
			.withStaysInMunicipality(true)
			.withStayDescription("Lives at the registered address")
			.withAttestation(true)
			.withAttestedAt(ATTESTED_AT)
			.withChildren(CHILDREN)
			.withCosts(COSTS)
			.withIncomes(INCOMES)
			.withPendingBenefits(PENDING_BENEFITS)
			.withAssets(ASSETS)
			.withPersons(PERSONS)
			.withPlannings(PLANNINGS)
			.withPlannedActivities(PLANNED_ACTIVITIES)
			.withJobApplications(JOB_APPLICATIONS);

		assertThat(data.getApplicationType()).isEqualTo("NEW");
		assertThat(data.getMaritalStatus()).isEqualTo("SINGLE");
		assertThat(data.getPeriodMonth()).isEqualTo(6);
		assertThat(data.getPeriodYear()).isEqualTo(2026);
		assertThat(data.getPeriodChoice()).isEqualTo("CURRENT_MONTH");
		assertThat(data.getNormType()).isEqualTo(List.of("NATIONAL_NORM"));
		assertThat(data.getOtherBenefitDescription()).isEqualTo("Establishment benefit");
		assertThat(data.getLivelihoodDescription()).isEqualTo("Söker arbete");
		assertThat(data.getHasChildrenUnder21()).isTrue();
		assertThat(data.getChildrenResidenceChanged()).isFalse();
		assertThat(data.getChildrenResidenceChangeDescription()).isEqualTo("Barnen bor växelvis");
		assertThat(data.getHousingForm()).isEqualTo("RENTAL");
		assertThat(data.getHousingPersonCount()).isEqualTo(3);
		assertThat(data.getHousingRoomsPlusKitchen()).isEqualTo(3);
		assertThat(data.getHousingDescription()).isEqualTo("Trerumslägenhet");
		assertThat(data.getHousingChanged()).isFalse();
		assertThat(data.getHousingChangeDescription()).isEqualTo("Flyttade i maj");
		assertThat(data.getHasIncomes()).isTrue();
		assertThat(data.getHasPendingBenefits()).isFalse();
		assertThat(data.getHasAssets()).isFalse();
		assertThat(data.getStaysInMunicipality()).isTrue();
		assertThat(data.getStayDescription()).isEqualTo("Lives at the registered address");
	}

	@Test
	void testBuilderMethods2() {
		final var data = FinancialAssistanceData.create()
			.withApplicationType("NEW")
			.withMaritalStatus("SINGLE")
			.withPeriodMonth(6)
			.withPeriodYear(2026)
			.withPeriodChoice("CURRENT_MONTH")
			.withNormType(List.of("NATIONAL_NORM"))
			.withOtherBenefitDescription("Establishment benefit")
			.withLivelihoodDescription("Söker arbete")
			.withHasChildrenUnder21(true)
			.withChildrenResidenceChanged(false)
			.withChildrenResidenceChangeDescription("Barnen bor växelvis")
			.withHousingForm("RENTAL")
			.withHousingPersonCount(3)
			.withHousingRoomsPlusKitchen(3)
			.withHousingDescription("Trerumslägenhet")
			.withHousingChanged(false)
			.withHousingChangeDescription("Flyttade i maj")
			.withHasIncomes(true)
			.withHasPendingBenefits(false)
			.withHasAssets(false)
			.withStaysInMunicipality(true)
			.withStayDescription("Lives at the registered address")
			.withAttestation(true)
			.withAttestedAt(ATTESTED_AT)
			.withChildren(CHILDREN)
			.withCosts(COSTS)
			.withIncomes(INCOMES)
			.withPendingBenefits(PENDING_BENEFITS)
			.withAssets(ASSETS)
			.withPersons(PERSONS)
			.withPlannings(PLANNINGS)
			.withPlannedActivities(PLANNED_ACTIVITIES)
			.withJobApplications(JOB_APPLICATIONS);

		assertThat(data.getAttestation()).isTrue();
		assertThat(data.getAttestedAt()).isEqualTo(ATTESTED_AT);
		assertThat(data.getChildren()).isEqualTo(CHILDREN);
		assertThat(data.getCosts()).isEqualTo(COSTS);
		assertThat(data.getIncomes()).isEqualTo(INCOMES);
		assertThat(data.getPendingBenefits()).isEqualTo(PENDING_BENEFITS);
		assertThat(data.getAssets()).isEqualTo(ASSETS);
		assertThat(data.getPersons()).isEqualTo(PERSONS);
		assertThat(data.getPlannings()).isEqualTo(PLANNINGS);
		assertThat(data.getPlannedActivities()).isEqualTo(PLANNED_ACTIVITIES);
		assertThat(data.getJobApplications()).isEqualTo(JOB_APPLICATIONS);
		assertThat(data).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FinancialAssistanceData.create()).hasAllNullFieldsOrProperties();
	}

}
