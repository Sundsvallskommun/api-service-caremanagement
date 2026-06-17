package se.sundsvall.caremanagement.core.service;

import java.time.Year;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.integration.db.ErrandNumberSequenceRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandNumberSequenceEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandNumberGeneratorTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final int YEAR = Year.now().getValue();

	@Mock
	private ErrandNumberPrefixResolver prefixResolverMock;

	@Mock
	private ErrandNumberSequenceRepository sequenceRepositoryMock;

	@InjectMocks
	private ErrandNumberGenerator generator;

	@Test
	void usesShortCodePrefixAndIncrementsExistingSequence() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of("EB"));
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYear(MUNICIPALITY_ID, NAMESPACE, YEAR))
			.thenReturn(Optional.of(ErrandNumberSequenceEntity.create().withCurrentValue(41L)));

		final var number = generator.generate(MUNICIPALITY_ID, NAMESPACE);

		assertThat(number).isEqualTo("EB_%d_0042".formatted(YEAR));
		final var captor = ArgumentCaptor.forClass(ErrandNumberSequenceEntity.class);
		verify(sequenceRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getCurrentValue()).isEqualTo(42L);
	}

	@Test
	void startsAtOneWhenNoSequenceRowExists() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of("EB"));
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYear(MUNICIPALITY_ID, NAMESPACE, YEAR))
			.thenReturn(Optional.empty());

		final var number = generator.generate(MUNICIPALITY_ID, NAMESPACE);

		assertThat(number).isEqualTo("EB_%d_0001".formatted(YEAR));
		final var captor = ArgumentCaptor.forClass(ErrandNumberSequenceEntity.class);
		verify(sequenceRepositoryMock).save(captor.capture());
		assertThat(captor.getValue()).satisfies(saved -> {
			assertThat(saved.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
			assertThat(saved.getNamespace()).isEqualTo(NAMESPACE);
			assertThat(saved.getSequenceYear()).isEqualTo(YEAR);
			assertThat(saved.getCurrentValue()).isEqualTo(1L);
		});
	}

	@Test
	void fallsBackToUpperCaseNamespaceWhenNoShortCodeConfigured() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, "my_namespace")).thenReturn(Optional.empty());
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYear(MUNICIPALITY_ID, "my_namespace", YEAR))
			.thenReturn(Optional.empty());

		final var number = generator.generate(MUNICIPALITY_ID, "my_namespace");

		assertThat(number).isEqualTo("MY_NAMESPACE_%d_0001".formatted(YEAR));
	}

	@Test
	void ignoresBlankShortCodeAndFallsBackToNamespace() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of("   "));
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYear(MUNICIPALITY_ID, NAMESPACE, YEAR))
			.thenReturn(Optional.empty());

		final var number = generator.generate(MUNICIPALITY_ID, NAMESPACE);

		assertThat(number).isEqualTo("FINANCIAL_ASSISTANCE_%d_0001".formatted(YEAR));
	}

	@Test
	void fallsBackToErrandPrefixWhenNamespaceBlank() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, " ")).thenReturn(Optional.empty());
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYear(MUNICIPALITY_ID, " ", YEAR))
			.thenReturn(Optional.empty());

		final var number = generator.generate(MUNICIPALITY_ID, " ");

		assertThat(number).isEqualTo("ERRAND_%d_0001".formatted(YEAR));
	}

	@Test
	void growsBeyondFourDigitsWithoutTruncation() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of("EB"));
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYear(MUNICIPALITY_ID, NAMESPACE, YEAR))
			.thenReturn(Optional.of(ErrandNumberSequenceEntity.create().withCurrentValue(9999L)));

		final var number = generator.generate(MUNICIPALITY_ID, NAMESPACE);

		assertThat(number).isEqualTo("EB_%d_10000".formatted(YEAR));
	}
}
