package se.sundsvall.caremanagement.lifecare.service.mapper;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsFromWhoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInfoDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInvestigationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInvestigationTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsOrganizationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsReasonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsServiceTypeDTO;
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
import static java.util.stream.Collectors.toSet;
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
 * The service and investigation links are <strong>not</strong> free choices. The proposal's top-level
 * {@code Services}/{@code Investigations} are everything the person has open anywhere in socialtjänsten — a
 * vuxenutredning, a BoU-avgift — and the chosen type names which of their types it accepts. Taking the first offered
 * one attached a "Vux Utredning 14 kap 2 § SoL" to an EB-återansökan, which FamilyCare answers 400 to; an accepted
 * type list that is empty means the type takes no such link, so none is sent. The specify-type and working-status
 * are still first-offered when the type asks for them, minus the unnamed {@code {id: 0}} placeholder that leads the
 * working-status catalogue. The {@code CaseworkerId} is set from the caseworker resolved off the applicant's most
 * recent Lifecare Service (see {@code CaseworkerResolver}) when one is supplied, and left unset otherwise — the
 * regelverk's "Handläggare = Rakel" is an open question.
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
				// The proposal lists every open service and investigation the person has, across the whole of
				// socialtjänsten - a vuxenutredning and a BoU-avgift sit in the same list as the EB ones. Only the
				// ones whose type the chosen actualisation type accepts may be linked, and a type that accepts none
				// (as EK Återansökan does for investigations) gets no link at all.
				linkedId(proposal.getServices(), PersonBasedAktualiseringsServiceDTO::getType, PersonBasedAktualiseringsServiceDTO::getId,
					type.getServiceTypes(), PersonBasedAktualiseringsServiceTypeDTO::getId).ifPresent(body::serviceId);
				linkedId(proposal.getInvestigations(), PersonBasedAktualiseringsInvestigationDTO::getType, PersonBasedAktualiseringsInvestigationDTO::getId,
					type.getInvestigationTypes(), PersonBasedAktualiseringsInvestigationTypeDTO::getId).ifPresent(body::investigationId);
			});
			organization(proposal, names, misses).ifPresent(org -> {
				body.organisationId(org.getId());
				body.organisationUnitId(org.getUnitId());
			});
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

	/**
	 * The first working status that actually names something. The catalogue leads with an unnamed placeholder
	 * ({@code {id: 0, name: ""}}) meaning "no status", which is not an answer for a type that marks working status
	 * mandatory - so it is skipped and the first named status is used.
	 */
	private static Optional<Integer> firstWorkingStatusId(final PersonBasedAktualiseringProposalDTO proposal) {
		return ofNullable(proposal.getWorkingStatus()).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(status -> status.getId() != null)
			.filter(status -> StringUtils.hasText(status.getName()))
			.map(PersonBasedAktualiseringsWorkingStatusDTO::getId)
			.findFirst();
	}

	/**
	 * The first of the person's items whose type the chosen actualisation type accepts, or empty when the type accepts
	 * none. An empty accepted-types list is a statement, not a gap: the type takes no such link, and sending one
	 * anyway is what FamilyCare answers 400 to.
	 */
	private static <T, U> Optional<Integer> linkedId(final List<T> candidates, final Function<T, Integer> typeOf, final Function<T, Integer> idOf,
		final List<U> acceptedTypes, final Function<U, Integer> acceptedTypeId) {

		final var accepted = ofNullable(acceptedTypes).orElseGet(List::<U>of).stream()
			.filter(Objects::nonNull)
			.map(acceptedTypeId)
			.filter(Objects::nonNull)
			.collect(toSet());

		return ofNullable(candidates).orElseGet(List::<T>of).stream()
			.filter(Objects::nonNull)
			.filter(candidate -> idOf.apply(candidate) != null)
			.filter(candidate -> accepted.contains(typeOf.apply(candidate)))
			.findFirst()
			.map(idOf);
	}
}
