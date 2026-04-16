package se.sundsvall.caremanagement.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class LookupTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Lookup.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var name = "n";
		final var displayName = "dn";
		final var created = now();
		final var modified = now();

		final var result = Lookup.create()
			.withName(name)
			.withDisplayName(displayName)
			.withCreated(created)
			.withModified(modified);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getName()).isEqualTo(name);
		assertThat(result.getDisplayName()).isEqualTo(displayName);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Lookup.create()).hasAllNullFieldsOrProperties();
	}
}
