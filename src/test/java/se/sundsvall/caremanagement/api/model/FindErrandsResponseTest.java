package se.sundsvall.caremanagement.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.models.api.paging.PagingAndSortingMetaData;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class FindErrandsResponseTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> PagingAndSortingMetaData.create().withPage(new Random().nextInt()), PagingAndSortingMetaData.class);
	}

	@Test
	void testBean() {
		assertThat(FindErrandsResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var errands = List.of(Errand.create());
		final var metaData = PagingAndSortingMetaData.create().withPage(1).withLimit(10).withCount(1).withTotalRecords(1).withTotalPages(1);

		final var result = FindErrandsResponse.create()
			.withErrands(errands)
			.withMetaData(metaData);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getErrands()).isEqualTo(errands);
		assertThat(result.getMetaData()).isEqualTo(metaData);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FindErrandsResponse.create()).hasAllNullFieldsOrProperties();
	}
}
