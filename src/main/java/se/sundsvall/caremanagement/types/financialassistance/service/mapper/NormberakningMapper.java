package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import se.sundsvall.caremanagement.lifecare.service.model.NormberakningResult;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekChangeWarning;
import se.sundsvall.caremanagement.lifecare.service.model.UnhandledIncome;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormberakningResponse;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

/**
 * Maps the service-layer {@link NormberakningResult} to the API {@link NormberakningResponse}, flattening the
 * structured
 * warnings into human-readable lines for the handläggare. Null-safe throughout.
 */
public final class NormberakningMapper {

	private NormberakningMapper() {}

	public static NormberakningResponse toResponse(final NormberakningResult result) {
		return ofNullable(result)
			.map(value -> NormberakningResponse.create()
				.withCalculationId(value.calculationId())
				.withUnhandledIncomes(toUnhandledLines(value.unhandledIncomes()))
				.withChangeWarnings(toChangeWarningLines(value.changeWarnings())))
			.orElseGet(NormberakningResponse::create);
	}

	private static List<String> toUnhandledLines(final List<UnhandledIncome> unhandledIncomes) {
		return ofNullable(unhandledIncomes).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.map(NormberakningMapper::describe)
			.toList();
	}

	private static String describe(final UnhandledIncome income) {
		final var label = Stream.of(income.forman(), income.delforman(), income.beloppstyp())
			.filter(Objects::nonNull)
			.filter(value -> !value.isBlank())
			.collect(joining(" / "));
		return label + " (" + income.reason() + ")";
	}

	private static List<String> toChangeWarningLines(final List<SsbtekChangeWarning> changeWarnings) {
		return ofNullable(changeWarnings).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.map(NormberakningMapper::describe)
			.toList();
	}

	private static String describe(final SsbtekChangeWarning warning) {
		return warning.forman() + ": " + warning.changePercent() + "% (jämförelse " + warning.jamforelseSum() + " → kontroll " + warning.kontrollSum() + ")";
	}
}
