package se.sundsvall.caremanagement.core.integration.db.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandSpecificationTest {

	@Mock
	private Root<ErrandEntity> rootMock;

	@Mock
	private CriteriaQuery<?> queryMock;

	@Mock
	private CriteriaBuilder cbMock;

	@Mock
	@SuppressWarnings("rawtypes")
	private Path pathMock;

	@Mock
	private Predicate predicateMock;

	@Mock
	private Predicate conjunctionMock;

	@Test
	void withNamespaceAndMunicipalityIdBuildsAndPredicate() {
		when(rootMock.get("namespace")).thenReturn(pathMock);
		when(rootMock.get("municipalityId")).thenReturn(pathMock);
		when(cbMock.equal(pathMock, "ns")).thenReturn(predicateMock);
		when(cbMock.equal(pathMock, "2281")).thenReturn(predicateMock);
		when(cbMock.and(predicateMock, predicateMock)).thenReturn(predicateMock);

		final var spec = ErrandSpecification.withNamespaceAndMunicipalityId("ns", "2281");
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(predicateMock);
		verify(cbMock).and(predicateMock, predicateMock);
	}

	@Test
	void withStatusBuildsEqualWhenPresent() {
		when(rootMock.get("status")).thenReturn(pathMock);
		when(cbMock.equal(pathMock, "OPEN")).thenReturn(predicateMock);

		final var spec = ErrandSpecification.withStatus("OPEN");
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(predicateMock);
	}

	@Test
	void withStatusReturnsConjunctionWhenNull() {
		when(cbMock.conjunction()).thenReturn(conjunctionMock);

		final var spec = ErrandSpecification.withStatus(null);
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(conjunctionMock);
	}

	@Test
	void withTypeSlugBuildsEqualWhenPresent() {
		when(rootMock.get("typeSlug")).thenReturn(pathMock);
		when(cbMock.equal(pathMock, "fostercare")).thenReturn(predicateMock);

		final var spec = ErrandSpecification.withTypeSlug("fostercare");
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(predicateMock);
	}

	@Test
	void withTypeSlugReturnsConjunctionWhenNull() {
		when(cbMock.conjunction()).thenReturn(conjunctionMock);

		final var spec = ErrandSpecification.withTypeSlug(null);
		final var result = spec.toPredicate(rootMock, queryMock, cbMock);

		assertThat(result).isSameAs(conjunctionMock);
	}
}
