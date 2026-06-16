package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbCaseService;
import se.sundsvall.caremanagement.lifecare.service.LifecareEbRoster;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PrefillPerson;
import se.sundsvall.caremanagement.types.financialassistance.api.model.RenewalPrefill;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.ROLE_CO_APPLICANT;

/**
 * Builds an EB återansökan pre-fill from Lifecare. Reads the applicant's most recent normberäkning roster (and the
 * co-applicant from the most recent beslut) via {@link LifecareEbCaseService}, then maps it to a
 * {@link RenewalPrefill}:
 * the applicant + co-applicant as {@code persons}, the remaining household members as {@code children}. Lifecare only
 * supplies personnummer + name, so everything else (boendeomfattning, skola, utbetalning) is left for the citizen. The
 * lookup is best-effort — a Lifecare failure yields an empty pre-fill with {@code lifecareChecked=false} rather than an
 * error, mirroring the eligibility check.
 */
@Service
@Transactional(readOnly = true)
public class RenewalPrefillService {

	private final LifecareEbCaseService lifecareEbCaseService;

	RenewalPrefillService(final LifecareEbCaseService lifecareEbCaseService) {
		this.lifecareEbCaseService = lifecareEbCaseService;
	}

	public RenewalPrefill prefill(final String personalNumber) {
		try {
			return toPrefill(lifecareEbCaseService.latestRoster(personalNumber, LocalDate.now()));
		} catch (final ThrowableProblem e) {
			return RenewalPrefill.create().withLifecareChecked(false).withPersons(List.of()).withChildren(List.of());
		}
	}

	private static RenewalPrefill toPrefill(final LifecareEbRoster roster) {
		final var persons = new ArrayList<PrefillPerson>();
		persons.add(person(ROLE_APPLICANT, roster.applicant(), nameOf(roster, roster.applicant())));
		if (roster.coApplicant() != null) {
			persons.add(person(ROLE_CO_APPLICANT, roster.coApplicant(), nameOf(roster, roster.coApplicant())));
		}

		final var children = roster.members().stream()
			.filter(member -> !Objects.equals(member.personalNumber(), roster.applicant()))
			.filter(member -> !Objects.equals(member.personalNumber(), roster.coApplicant()))
			.map(member -> person(null, member.personalNumber(), member.name()))
			.toList();

		return RenewalPrefill.create()
			.withLifecareChecked(true)
			.withPersons(persons)
			.withChildren(children);
	}

	private static PrefillPerson person(final String role, final String personalNumber, final String name) {
		return PrefillPerson.create().withRole(role).withPersonalNumber(personalNumber).withName(name);
	}

	private static String nameOf(final LifecareEbRoster roster, final String personalNumber) {
		return roster.members().stream()
			.filter(member -> Objects.equals(member.personalNumber(), personalNumber))
			.map(LifecareEbRoster.Member::name)
			.findFirst()
			.orElse(null);
	}
}
