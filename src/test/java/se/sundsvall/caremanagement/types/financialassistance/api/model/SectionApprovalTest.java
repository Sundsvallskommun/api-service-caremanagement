package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
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

class SectionApprovalTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(SectionApproval.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var when = OffsetDateTime.parse("2026-06-18T09:00:00Z");
		final var approval = SectionApproval.create()
			.withSection("CALCULATION")
			.withApproved(true)
			.withApprovedBy("jane02doe")
			.withApprovedAt(when);

		assertThat(approval.getSection()).isEqualTo("CALCULATION");
		assertThat(approval.isApproved()).isTrue();
		assertThat(approval.getApprovedBy()).isEqualTo("jane02doe");
		assertThat(approval.getApprovedAt()).isEqualTo(when);
		assertThat(approval).hasNoNullFieldsOrProperties();
	}

	@Test
	void testCreateReturnsEmptyInstance() {
		assertThat(SectionApproval.create()).hasAllNullFieldsOrPropertiesExcept("approved");
	}
}
