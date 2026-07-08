package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefc.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * Small shared helpers for the lifecare FC mappers — pulled out of the individual mappers where they were duplicated.
 */
public final class MapperUtil {

	private MapperUtil() {}

	/** Trim + lower-case a value for case/space-insensitive name matching; {@code null} becomes {@code ""}. */
	public static String normalize(final String value) {
		return ofNullable(value).map(v -> v.trim().toLowerCase()).orElse("");
	}

	/** A {@link LocalDate} at UTC start-of-day, or {@code null} when the date is {@code null}. */
	public static OffsetDateTime toOffsetDateTime(final LocalDate date) {
		return ofNullable(date).map(value -> value.atStartOfDay().atOffset(ZoneOffset.UTC)).orElse(null);
	}

	/**
	 * Index a proposal's calculation income types by normalized name → FC type id, keeping insertion order and the first
	 * id when two types normalize to the same name. Types with a null name or id are skipped.
	 */
	public static Map<String, Integer> indexIncomeTypeIds(final PersonBasedCalculationProposalDTO proposal) {
		return ofNullable(proposal)
			.map(PersonBasedCalculationProposalDTO::getCalculationIncomeTypes)
			.orElseGet(List::of).stream()
			.filter(type -> (type.getName() != null) && (type.getId() != null))
			.collect(toMap(type -> normalize(type.getName()), PersonBasedCalculationCalculationIncomeTypeDTO::getId, (first, second) -> first, LinkedHashMap::new));
	}
}
