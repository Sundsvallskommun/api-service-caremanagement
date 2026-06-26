package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class LifecareDecisionTest {

	private static final List<LifecareDecisionPerson> PERSONS = List.of(LifecareDecisionPerson.create().withPersonId("200001011234").withName("Anna Andersson"));

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> LifecareDecisionPerson.create().withPersonId("200001011234"), LifecareDecisionPerson.class);
	}

	@Test
	void testBean() {
		assertThat(LifecareDecision.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var decision = LifecareDecision.create()
			.withId(9900)
			.withDate("2026-06-02")
			.withType("Bifall")
			.withFromDate("2026-06-01")
			.withToDate("2026-06-30")
			.withReason("Beviljas enligt norm")
			.withDecisionMaker("Anna Andersson")
			.withOrganization("IFO")
			.withAmount(8500.0)
			.withCoApplicant("198001019999")
			.withReasonCoApplicant("Sammanboende")
			.withPersons(PERSONS);

		assertThat(decision.getId()).isEqualTo(9900);
		assertThat(decision.getDate()).isEqualTo("2026-06-02");
		assertThat(decision.getType()).isEqualTo("Bifall");
		assertThat(decision.getFromDate()).isEqualTo("2026-06-01");
		assertThat(decision.getToDate()).isEqualTo("2026-06-30");
		assertThat(decision.getReason()).isEqualTo("Beviljas enligt norm");
		assertThat(decision.getDecisionMaker()).isEqualTo("Anna Andersson");
		assertThat(decision.getOrganization()).isEqualTo("IFO");
		assertThat(decision.getAmount()).isEqualTo(8500.0);
		assertThat(decision.getCoApplicant()).isEqualTo("198001019999");
		assertThat(decision.getReasonCoApplicant()).isEqualTo("Sammanboende");
		assertThat(decision.getPersons()).isEqualTo(PERSONS);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(LifecareDecision.create()).hasAllNullFieldsOrProperties();
	}
}
