package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ActualisationTest {

	@Test
	void testBean() {
		assertThat(Actualisation.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var actualisation = Actualisation.create()
			.withId(5012)
			.withType("Ansökan")
			.withName("Ekonomiskt bistånd")
			.withDate("2026-06-01")
			.withReason("Nyansökan")
			.withRegards("Försörjningsstöd")
			.withFromWho("Den enskilde")
			.withCaseworker("Anna Andersson")
			.withOrganization("IFO")
			.withStatus("Pågående")
			.withInvestigationId(8801)
			.withServiceId(7700)
			.withDecisionId(9900);

		assertThat(actualisation.getId()).isEqualTo(5012);
		assertThat(actualisation.getType()).isEqualTo("Ansökan");
		assertThat(actualisation.getName()).isEqualTo("Ekonomiskt bistånd");
		assertThat(actualisation.getDate()).isEqualTo("2026-06-01");
		assertThat(actualisation.getReason()).isEqualTo("Nyansökan");
		assertThat(actualisation.getRegards()).isEqualTo("Försörjningsstöd");
		assertThat(actualisation.getFromWho()).isEqualTo("Den enskilde");
		assertThat(actualisation.getCaseworker()).isEqualTo("Anna Andersson");
		assertThat(actualisation.getOrganization()).isEqualTo("IFO");
		assertThat(actualisation.getStatus()).isEqualTo("Pågående");
		assertThat(actualisation.getInvestigationId()).isEqualTo(8801);
		assertThat(actualisation.getServiceId()).isEqualTo(7700);
		assertThat(actualisation.getDecisionId()).isEqualTo(9900);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(Actualisation.create()).hasAllNullFieldsOrProperties();
	}
}
