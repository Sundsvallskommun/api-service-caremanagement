package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.List;
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

class NormberakningDraftTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> List.of(DraftIncomeRow.create().withTypeName("type-" + new Random().nextInt())), List.class);
	}

	@Test
	void testBean() {
		assertThat(NormberakningDraft.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var draft = NormberakningDraft.create()
			.withErrandId("errand")
			.withApplicationMonth("2026-06")
			.withEdited(true)
			.withRows(List.of(DraftIncomeRow.create().withTypeName("Bostadsbidrag")));

		org.assertj.core.api.Assertions.assertThat(draft.getErrandId()).isEqualTo("errand");
		org.assertj.core.api.Assertions.assertThat(draft.isEdited()).isTrue();
		org.assertj.core.api.Assertions.assertThat(draft.getRows()).extracting(DraftIncomeRow::getTypeName).containsExactly("Bostadsbidrag");
	}

	@Test
	void createReturnsEmptyRows() {
		org.assertj.core.api.Assertions.assertThat(NormberakningDraft.create().getRows()).isEmpty();
	}
}
