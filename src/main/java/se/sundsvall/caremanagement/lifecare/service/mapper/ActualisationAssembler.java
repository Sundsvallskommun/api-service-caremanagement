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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.util.StringUtils;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Optional.ofNullable;

/**
 * Assembles the FamilyCare {@link PostAktualiseringsBodyRequest} for a financial-assistance intake (actualisation) by
 * resolving the integer codes the POST body requires from the person's FamilyCare actualisation proposal. The
 * proposal's {@code FromWho}/{@code Reason} code lists — and the {@code SpecifyType}/{@code WorkingStatus}
 * requirement flags — live inside the chosen actualisation <em>type</em>; the organisation, service and investigation
 * links are top-level.
 *
 * <p>
 * Sprint defaults where the proposal offers a choice: the first offered actualisation type is taken, then its first
 * reason and first fromWho; the first organisation (id + unit), the first service and the first investigation. A
 * specify-type is only set when the chosen type marks it mandatory, and a working-status only when the chosen type asks
 * for it — then the first offered value is used. The {@code CaseworkerId} is set from the caseworker resolved off the
 * applicant's most recent Lifecare Service (see {@code CaseworkerResolver}) when one is supplied, and left unset
 * otherwise. These selections are intentionally simple and isolated here so they are easy to refine once real
 * FamilyCare proposals are available, mirroring {@link CalculationAssembler}.
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
	public static PostAktualiseringsBodyRequest assemble(final String applicantPersonId, final PersonBasedAktualiseringProposalDTO proposalDTO, final LocalDate date, final String caseworkerId) {
		final var body = new PostAktualiseringsBodyRequest()
			.personId(applicantPersonId)
			.date(date.format(ISO_LOCAL_DATE));

		ofNullable(caseworkerId).filter(StringUtils::hasText).ifPresent(body::caseworkerId);

		ofNullable(proposalDTO).ifPresent(proposal -> {
			firstActualisationType(proposal).ifPresent(type -> {
				body.type(type.getId());
				firstReasonId(type).ifPresent(body::reason);
				firstFromWhoId(type).ifPresent(body::fromWho);
				if (Boolean.TRUE.equals(type.getSpecifyTypeMandatory())) {
					firstSpecifyTypeId(proposal).ifPresent(body::specifies);
				}
				if (Boolean.TRUE.equals(type.getWorkingStatus())) {
					firstWorkingStatusId(proposal).ifPresent(body::workingStatus);
				}
			});
			firstOrganization(proposal).ifPresent(org -> {
				body.organisationId(org.getId());
				body.organisationUnitId(org.getUnitId());
			});
			firstServiceId(proposal).ifPresent(body::serviceId);
			firstInvestigationId(proposal).ifPresent(body::investigationId);
		});

		return body;
	}

	/** The first offered actualisation type — it carries the reason/fromWho code lists and the requirement flags. */
	private static Optional<PersonBasedAktualiseringsInfoDTO> firstActualisationType(final PersonBasedAktualiseringProposalDTO proposal) {
		return ofNullable(proposal.getActualisationTypes()).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(type -> type.getId() != null)
			.findFirst();
	}

	private static Optional<Integer> firstReasonId(final PersonBasedAktualiseringsInfoDTO type) {
		return ofNullable(type.getReasons()).orElseGet(List::of).stream()
			.map(PersonBasedAktualiseringsReasonDTO::getId)
			.filter(Objects::nonNull)
			.findFirst();
	}

	private static Optional<Integer> firstFromWhoId(final PersonBasedAktualiseringsInfoDTO type) {
		return ofNullable(type.getFromWho()).orElseGet(List::of).stream()
			.map(PersonBasedAktualiseringsFromWhoDTO::getId)
			.filter(Objects::nonNull)
			.findFirst();
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

	private static Optional<PersonBasedAktualiseringsOrganizationDTO> firstOrganization(final PersonBasedAktualiseringProposalDTO proposal) {
		return ofNullable(proposal.getOrganizations()).orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(org -> org.getId() != null)
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
