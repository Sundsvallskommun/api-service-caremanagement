package se.sundsvall.caremanagement.core.service;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.integration.db.ErrandNumberSequenceRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandNumberSequenceEntity;

import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandNumberGeneratorTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final LocalDate TODAY = LocalDate.now(systemDefault());
	private static final int YEAR = TODAY.getYear();
	private static final int MONTH = TODAY.getMonthValue();
	// Two-digit year + month stamp, e.g. "2606" in June 2026.
	private static final String STAMP = "%02d%02d".formatted(YEAR % 100, MONTH);

	@Mock
	private ErrandNumberPrefixResolver prefixResolverMock;

	@Mock
	private ErrandNumberSequenceRepository sequenceRepositoryMock;

	@InjectMocks
	private ErrandNumberGenerator generator;

	@Test
	void usesShortCodePrefixAndIncrementsExistingSequence() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of("EB"));
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYearAndSequenceMonth(MUNICIPALITY_ID, NAMESPACE, YEAR, MONTH))
			.thenReturn(Optional.of(ErrandNumberSequenceEntity.create().withCurrentValue(70L)));

		final var number = generator.generate(MUNICIPALITY_ID, NAMESPACE);

		assertThat(number).isEqualTo("EB-%s0071".formatted(STAMP));
		final var captor = ArgumentCaptor.forClass(ErrandNumberSequenceEntity.class);
		verify(sequenceRepositoryMock).save(captor.capture());
		assertThat(captor.getValue().getCurrentValue()).isEqualTo(71L);
	}

	@Test
	void startsAtOneWhenNoSequenceRowExists() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of("EB"));
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYearAndSequenceMonth(MUNICIPALITY_ID, NAMESPACE, YEAR, MONTH))
			.thenReturn(Optional.empty());

		final var number = generator.generate(MUNICIPALITY_ID, NAMESPACE);

		assertThat(number).isEqualTo("EB-%s0001".formatted(STAMP));
		final var captor = ArgumentCaptor.forClass(ErrandNumberSequenceEntity.class);
		verify(sequenceRepositoryMock).save(captor.capture());
		assertThat(captor.getValue()).satisfies(saved -> {
			assertThat(saved.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
			assertThat(saved.getNamespace()).isEqualTo(NAMESPACE);
			assertThat(saved.getSequenceYear()).isEqualTo(YEAR);
			assertThat(saved.getSequenceMonth()).isEqualTo(MONTH);
			assertThat(saved.getCurrentValue()).isEqualTo(1L);
		});

		// The counter row is seeded idempotently before the locked read, making the first-of-month create race-safe.
		final InOrder inOrder = inOrder(sequenceRepositoryMock);
		inOrder.verify(sequenceRepositoryMock).ensureSequenceRow(MUNICIPALITY_ID, NAMESPACE, YEAR, MONTH);
		inOrder.verify(sequenceRepositoryMock).findByMunicipalityIdAndNamespaceAndSequenceYearAndSequenceMonth(MUNICIPALITY_ID, NAMESPACE, YEAR, MONTH);
	}

	@Test
	void fallsBackToUpperCaseNamespaceWhenNoShortCodeConfigured() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, "my_namespace")).thenReturn(Optional.empty());
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYearAndSequenceMonth(MUNICIPALITY_ID, "my_namespace", YEAR, MONTH))
			.thenReturn(Optional.empty());

		final var number = generator.generate(MUNICIPALITY_ID, "my_namespace");

		assertThat(number).isEqualTo("MY_NAMESPACE-%s0001".formatted(STAMP));
	}

	@Test
	void ignoresBlankShortCodeAndFallsBackToNamespace() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of("   "));
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYearAndSequenceMonth(MUNICIPALITY_ID, NAMESPACE, YEAR, MONTH))
			.thenReturn(Optional.empty());

		final var number = generator.generate(MUNICIPALITY_ID, NAMESPACE);

		assertThat(number).isEqualTo("FINANCIAL_ASSISTANCE-%s0001".formatted(STAMP));
	}

	@Test
	void fallsBackToErrandPrefixWhenNamespaceBlank() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, " ")).thenReturn(Optional.empty());
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYearAndSequenceMonth(MUNICIPALITY_ID, " ", YEAR, MONTH))
			.thenReturn(Optional.empty());

		final var number = generator.generate(MUNICIPALITY_ID, " ");

		assertThat(number).isEqualTo("ERRAND-%s0001".formatted(STAMP));
	}

	@Test
	void growsBeyondFourDigitsWithoutTruncation() {
		when(prefixResolverMock.resolvePrefix(MUNICIPALITY_ID, NAMESPACE)).thenReturn(Optional.of("EB"));
		when(sequenceRepositoryMock.findByMunicipalityIdAndNamespaceAndSequenceYearAndSequenceMonth(MUNICIPALITY_ID, NAMESPACE, YEAR, MONTH))
			.thenReturn(Optional.of(ErrandNumberSequenceEntity.create().withCurrentValue(9999L)));

		final var number = generator.generate(MUNICIPALITY_ID, NAMESPACE);

		assertThat(number).isEqualTo("EB-%s10000".formatted(STAMP));
	}
}
