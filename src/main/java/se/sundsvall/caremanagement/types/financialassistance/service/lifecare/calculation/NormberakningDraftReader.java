package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.service.FinancialAssistanceCalculationService;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareErrand;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationDraftFill.DraftPerson;

/**
 * careM's draft normberäkning with the personnummer of its persons, which the caseworker identifies a member by and a
 * Lifecare household member is matched on. The personnummer are resolved best-effort: a person the citizen service
 * cannot resolve keeps none.
 */
@Component
class NormberakningDraftReader {

	private static final Logger LOG = LoggerFactory.getLogger(NormberakningDraftReader.class);

	private final FinancialAssistanceCalculationService calculationService;
	private final CitizenService citizenService;

	NormberakningDraftReader(final FinancialAssistanceCalculationService calculationService, final CitizenService citizenService) {
		this.calculationService = calculationService;
		this.citizenService = citizenService;
	}

	/**
	 * The draft and the personnummer of its persons.
	 *
	 * @param  errand the errand
	 * @return        the draft, 404 when there is none
	 */
	DraftWithNumbers read(final LifecareErrand errand) {
		final var draft = calculationService.getDraft(errand.municipalityId(), errand.namespace(), errand.errandId());
		final var numbers = new LinkedHashMap<String, String>();
		Optional.ofNullable(draft.getPersons()).orElse(List.of()).stream()
			.map(NormPersonRow::getPartyId)
			.filter(Objects::nonNull)
			.distinct()
			.forEach(partyId -> personalNumber(errand.municipalityId(), partyId).ifPresent(number -> numbers.put(partyId, number)));
		return new DraftWithNumbers(draft, numbers);
	}

	/**
	 * The first day of the draft's period, read as the draft is. Best-effort: empty when there is no draft.
	 *
	 * @param  errand the errand
	 * @return        the period start, yyyy-MM-dd
	 */
	Optional<String> periodStart(final LifecareErrand errand) {
		try {
			return Optional.ofNullable(calculationService.getDraft(errand.municipalityId(), errand.namespace(), errand.errandId()).getCalculationFromDate())
				.map(Object::toString);
		} catch (final RuntimeException e) {
			LOG.info("No draft period for errand {} ({})", errand.errandId(), e.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	private Optional<String> personalNumber(final String municipalityId, final String partyId) {
		try {
			return citizenService.getPersonalNumber(municipalityId, partyId);
		} catch (final RuntimeException e) {
			LOG.info("Could not resolve a draft person's personnummer ({})", e.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	/**
	 * careM's draft with the personnummer of its persons.
	 *
	 * @param draft           the draft
	 * @param personalNumbers personnummer by partyId
	 */
	record DraftWithNumbers(CalculationDraft draft, Map<String, String> personalNumbers) {

		DraftWithNumbers {
			personalNumbers = Map.copyOf(personalNumbers);
		}

		/** The draft's persons with their personnummer, for matching them to Lifecare's household. */
		List<DraftPerson> persons() {
			return Optional.ofNullable(draft.getPersons()).orElse(List.of()).stream()
				.map(row -> new DraftPerson(row, Optional.ofNullable(row.getPartyId()).map(personalNumbers::get).orElse(null)))
				.toList();
		}
	}
}
