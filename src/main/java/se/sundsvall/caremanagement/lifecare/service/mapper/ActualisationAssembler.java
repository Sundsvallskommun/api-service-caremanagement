package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsFromWhoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInfoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInvestigationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsOrganizationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsReasonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsSpecifyTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsWorkingStatusDTO;
import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.service.ActualisationProperties;

import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.lifecare.integration.FamilyCareDates.startOfDay;
import static se.sundsvall.caremanagement.lifecare.service.mapper.MapperUtil.normalize;

/**
 * Assembles the FamilyCare {@link PostAktualiseringsBodyRequest} for a financial-assistance intake (actualisation) by
 * resolving the integer codes the POST body requires from the person's FamilyCare actualisation proposal. The
 * proposal's {@code FromWho}/{@code Reason} code lists — and the {@code SpecifyType}/{@code WorkingStatus}
 * requirement flags — live inside the chosen actualisation <em>type</em>; the organisation, service and investigation
 * links are top-level.
 *
 * <p>
 * The type, reason, fromWho and organisation verksamheten named (see {@link ActualisationProperties}) are looked up
 * by catalogue name, case- and whitespace-insensitively, and fall back to the first offered value when the name is
 * not in the proposal — an intake must never be blocked by a renamed catalogue entry. Every miss is reported through
 * {@link Selection#misses()} so the caller can log it: a silent fallback is the guess the configuration exists to
 * remove. The order matters — {@code reason} and {@code fromWho} are looked up <em>inside the chosen type</em>, not
 * at the top level, so choosing the type by name has to happen first.
 *
 * <p>
 * Still "first offered", because verksamheten has not named them: the service, the investigation, and the
 * specify-type / working-status that are only set when the chosen type asks for them. The {@code CaseworkerId} is
 * set from the caseworker resolved off the applicant's most recent Lifecare Service (see {@code CaseworkerResolver})
 * when one is supplied, and left unset otherwise — the regelverk's "Handläggare = Rakel" is an open question.
 */
public final class ActualisationAssembler {

	private ActualisationAssembler() {}

	/**
	 * Build the FamilyCare actualisation body for one applicant and intake date.
	 *
	 * @param  applicantPersonId the applicant's personal identity number (the FamilyCare actualisation owner)
	 * @param  proposalDTO       the FamilyCare actualisation proposal supplying the code lists; may be {@code null}
	 * @param  date              the intake date
	 * @param  caseworkerId      the resolved FamilyCare caseworker id; {@code null}/blank leaves it unset
	 * @return                   the assembled {@link PostAktualiseringsBodyRequest}
	 */
	public static Selection assemble(final String applicantPersonId, final PersonBasedAktualiseringProposalDTO proposalDTO, final LocalDate date,
		final String caseworkerId, final ActualisationProperties names) {

		final var body = new PostAktualiseringsBodyRequest()
			.personId(applicantPersonId)
			.date(startOfDay(date));
		final var misses = new ArrayList<String>();

		ofNullable(caseworkerId).filter(StringUtils::hasText).ifPresent(body::caseworkerId);

		ofNullable(proposalDTO).ifPresent(proposal -> {
			actualisationType(proposal, names, misses).ifPresent(type -> {
				body.type(type.getId());
				// reason and fromWho are the chosen type's own code lists, not the proposal's
				byName(type.getReasons(), PersonBasedAktualiseringsReasonDTO::getName, PersonBasedAktualiseringsReasonDTO::getId,
					names.reason(), "reason", misses).ifPresent(body::reason);
				byName(type.getFromWho(), PersonBasedAktualiseringsFromWhoDTO::getName, PersonBasedAktualiseringsFromWhoDTO::getId,
					names.fromWho(), "fromWho", misses).ifPresent(body::fromWho);
				if (Boolean.TRUE.equals(type.getSpecifyTypeMandatory())) {
					firstSpecifyTypeId(proposal).ifPresent(body::specifies);
				}
				if (Boolean.TRUE.equals(type.getWorkingStatus())) {
					firstWorkingStatusId(proposal).ifPresent(body::workingStatus);
				}
			});
			organization(proposal, names, misses).ifPresent(org -> {
				body.organisationId(org.getId());
				body.organisationUnitId(org.getUnitId());
			});
			firstServiceId(proposal).ifPresent(body::serviceId);
			firstInvestigationId(proposal).ifPresent(body::investigationId);
		});

		return new Selection(body, List.copyOf(misses));
	}

	/**
	 * The assembled body plus the configured names that were not found in the proposal and therefore fell back to the
	 * first offered value. Empty misses means every name verksamheten gave us matched a catalogue entry.
	 */
	public record Selection(PostAktualiseringsBodyRequest body, List<String> misses) {}

	/** The actualisation type verksamheten named, or the first offered one. */
	private static Optional<PersonBasedAktualiseringsInfoDTO> actualisationType(final PersonBasedAktualiseringProposalDTO proposal,
		final ActualisationProperties names, final List<String> misses) {

		final var candidates = ofNullable(proposal.getActualisationTypes()).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(type -> type.getId() != null)
			.toList();
		final var named = candidates.stream()
			.filter(type -> normalize(type.getName()).equals(normalize(names.type())))
			.findFirst();
		if (named.isEmpty() && !candidates.isEmpty()) {
			misses.add("type=" + names.type());
		}
		return named.or(() -> candidates.stream().findFirst());
	}

	/** The organisation verksamheten named, or the first offered one. */
	private static Optional<PersonBasedAktualiseringsOrganizationDTO> organization(final PersonBasedAktualiseringProposalDTO proposal,
		final ActualisationProperties names, final List<String> misses) {

		final var candidates = ofNullable(proposal.getOrganizations()).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(org -> org.getId() != null)
			.toList();
		final var named = candidates.stream()
			.filter(org -> normalize(org.getName()).equals(normalize(names.organisation())))
			.findFirst();
		if (named.isEmpty() && !candidates.isEmpty()) {
			misses.add("organisation=" + names.organisation());
		}
		return named.or(() -> candidates.stream().findFirst());
	}

	/** One code list, matched on the catalogue name with a first-offered fallback. */
	private static <T> Optional<Integer> byName(final List<T> candidates, final Function<T, String> name, final Function<T, Integer> id,
		final String wanted, final String field, final List<String> misses) {

		final var withId = ofNullable(candidates).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(candidate -> id.apply(candidate) != null)
			.toList();
		final var named = withId.stream()
			.filter(candidate -> normalize(name.apply(candidate)).equals(normalize(wanted)))
			.findFirst();
		if (named.isEmpty() && !withId.isEmpty()) {
			misses.add(field + "=" + wanted);
		}
		return named.or(() -> withId.stream().findFirst()).map(id);
	}

	private static Optional<Integer> firstSpecifyTypeId(final PersonBasedAktualiseringProposalDTO proposal) {
		return ofNullable(proposal.getSpecifyTypes()).orElseGet(List::of).stream()
			.map(PersonBasedAktualiseringsSpecifyTypeDTO::getId)
			.filter(Objects::nonNull)
			.findFirst();
	}

	private static Optional<Integer> firstWorkingStatusId(final PersonBasedAktualiseringProposalDTO proposal) {
		return ofNullable(proposal.getWorkingStatus()).orElseGet(List::of).stream()
			.map(PersonBasedAktualiseringsWorkingStatusDTO::getId)
			.filter(Objects::nonNull)
			.findFirst();
	}

	private static Optional<Integer> firstServiceId(final PersonBasedAktualiseringProposalDTO proposal) {
		return ofNullable(proposal.getServices()).orElseGet(List::of).stream()
			.map(PersonBasedAktualiseringsServiceDTO::getId)
			.filter(Objects::nonNull)
			.findFirst();
	}

	private static Optional<Integer> firstInvestigationId(final PersonBasedAktualiseringProposalDTO proposal) {
		return ofNullable(proposal.getInvestigations()).orElseGet(List::of).stream()
			.map(PersonBasedAktualiseringsInvestigationDTO::getId)
			.filter(Objects::nonNull)
			.findFirst();
	}
}
