package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringsInfoDTO;
import generated.se.sundsvall.lifecarefamilycare.PostAktualiseringsBodyRequest;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCare;
import se.sundsvall.caremanagement.lifecare.service.mapper.ActualisationAssembler;
import se.sundsvall.caremanagement.lifecare.service.model.ActualisationSummary;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.caremanagement.lifecare.service.mapper.MapperUtil.normalize;

/**
 * Creates a financial-assistance intake (actualisation) in Lifecare FamilyCare through the API — the case-intake
 * step. Fetches the applicant's FamilyCare actualisation proposal, assembles the {@code PostAktualiseringsBodyRequest}
 * against it (via {@link ActualisationAssembler}), posts it, and returns the created actualisation id.
 *
 * <p>
 * The write is a two-call exchange (proposal GET → actualisation POST); both go through {@link
 * LifecareFamilyCare}, which keeps the generated FamilyCare DTOs and the privacy-safe logging inside the
 * integration layer. Mirrors {@link CalculationService}.
 *
 * <p>
 * FamilyCare cannot delete an actualisation, so the write must not be repeated for one errand: a step that may be
 * retrying an attempt that already reached Lifecare goes through {@link #createOrAdoptActualisation}.
 */
@Service
@EnableConfigurationProperties(ActualisationProperties.class)
public class ActualisationService {

	private static final Logger LOG = LoggerFactory.getLogger(ActualisationService.class);

	private static final String AMBIGUOUS_EARLIER_ATTEMPT = "Lifecare holds %d actualisations that an earlier attempt of this step may have created (ids %s), "
		+ "and which of them is the errand's cannot be told - none was created. Record the right one on the errand as a Decision(ACTUALISATION) and run the step again.";

	private final LifecareFamilyCare lifecareFamilyCareIntegration;
	private final CaseworkerResolver caseworkerResolver;
	private final ActualisationProperties actualisationProperties;

	public ActualisationService(final LifecareFamilyCare lifecareFamilyCareIntegration, final CaseworkerResolver caseworkerResolver,
		final ActualisationProperties actualisationProperties) {
		this.lifecareFamilyCareIntegration = lifecareFamilyCareIntegration;
		this.caseworkerResolver = caseworkerResolver;
		this.actualisationProperties = actualisationProperties;
	}

	/**
	 * Build and post the actualisation for the applicant and intake date. The caseworker is resolved off the applicant's
	 * most recent Lifecare Service and, when found, set as the actualisation {@code CaseworkerId}; the same user's network
	 * id is returned so the caller can assign the careM errand. Caseworker resolution is best-effort — a lookup failure
	 * is logged and the intake is still created without a caseworker.
	 *
	 * <p>
	 * Always creates. A caller that may be retrying an attempt that already reached Lifecare uses
	 * {@link #createOrAdoptActualisation} instead.
	 *
	 * @param  applicantPartyId the applicant's partyId (the FamilyCare actualisation owner)
	 * @param  date             the intake date
	 * @param  newApplication   {@code true} for a nyansökan, which takes the nyansökan actualisation type
	 * @return                  the created actualisation id and the errand assignee ({@code null} when no caseworker was
	 *                          found)
	 */
	public ActualisationResult createActualisation(final String municipalityId, final String applicantPartyId, final LocalDate date,
		final boolean newApplication) {

		return create(municipalityId, plan(municipalityId, applicantPartyId, date, newApplication));
	}

	/**
	 * Create the actualisation, unless an earlier attempt of the same step already did — for the retry of a step that may
	 * have got as far as Lifecare without getting to record what it made. FamilyCare has no delete and no lookup by
	 * errand, so a second actualisation cannot be cleaned up afterwards; it has to be prevented.
	 *
	 * <p>
	 * The earlier attempt's actualisation is found among the person's actualisations on the intake date. FamilyCare
	 * returns no creation time and the write carries no free text to put an errand number in, so what identifies it is
	 * what the write did carry — the person, the intake date and the actualisation type the proposal gave — narrowed by
	 * {@code claimedByAnotherErrand}, which drops every actualisation careM already knows belongs to another errand.
	 * <ul>
	 * <li>exactly one match: it is adopted, and nothing is created;</li>
	 * <li>none: nothing was created before, so it is created now;</li>
	 * <li>more than one: it cannot be told which is ours, so nothing is created and the call fails with
	 * {@code 409 CONFLICT} for a person to resolve;</li>
	 * <li>the lookup itself fails: the call fails with the integration's {@code 502}, and nothing is created — an answer
	 * that could not be read is not the same as “none”.</li>
	 * </ul>
	 *
	 * @param  claimedByAnotherErrand whether an actualisation id is already recorded on another errand
	 * @return                        the adopted or created actualisation; {@link ActualisationResult#adopted()} tells
	 *                                which
	 */
	public ActualisationResult createOrAdoptActualisation(final String municipalityId, final String applicantPartyId, final LocalDate date,
		final boolean newApplication, final Predicate<Integer> claimedByAnotherErrand) {

		final var plan = plan(municipalityId, applicantPartyId, date, newApplication);
		final var earlier = findEarlierAttempt(municipalityId, applicantPartyId, date, plan.typeName(), claimedByAnotherErrand);
		if (earlier.isEmpty()) {
			return create(municipalityId, plan);
		}

		final var actualisation = earlier.get();
		LOG.warn("Lifecare actualisation {} was created by an earlier attempt of this step - using it instead of creating a second one", actualisation.id());
		// The insats is what Lifecare holds on the actualisation itself, not what this attempt would have linked.
		return new ActualisationResult(actualisation.id(), plan.assignedUserId(), actualisation.serviceId(), true);
	}

	/** Resolve the caseworker, fetch the proposal and assemble the body — everything short of writing to Lifecare. */
	private Plan plan(final String municipalityId, final String applicantPartyId, final LocalDate date, final boolean newApplication) {
		final var caseworker = resolveCaseworker(municipalityId, applicantPartyId, date);

		final var names = namesFor(newApplication);
		final var proposal = lifecareFamilyCareIntegration.getActualisationProposal(municipalityId, applicantPartyId);
		final var selection = ActualisationAssembler.assemble(applicantPartyId, proposal, date,
			caseworker.map(ResolvedCaseworker::caseworkerId).orElse(null), names, !newApplication);
		// A name that is not in the catalogue falls back to the first offered value, which is the guess the
		// configuration exists to remove - so it must never pass silently.
		if (!selection.misses().isEmpty()) {
			LOG.warn("Lifecare's actualisation catalogue has no entry for {} - falling back to the first offered value. "
				+ "Check the configured names against the catalogue.", String.join(", ", selection.misses()));
		}

		return new Plan(caseworker.map(ResolvedCaseworker::assignedUserId).orElse(null), typeNameOf(proposal, selection.body().getType()), selection.body());
	}

	private ActualisationResult create(final String municipalityId, final Plan plan) {
		final var actualisationId = lifecareFamilyCareIntegration.createActualisation(municipalityId, plan.body());

		return new ActualisationResult(actualisationId, plan.assignedUserId(), plan.body().getServiceId());
	}

	/**
	 * The person's actualisation an earlier attempt created, if there is one: on the intake date, of the type the write
	 * used, and not recorded on another errand.
	 */
	private Optional<ActualisationSummary> findEarlierAttempt(final String municipalityId, final String applicantPartyId, final LocalDate date,
		final String typeName, final Predicate<Integer> claimedByAnotherErrand) {

		// A type name that is not in the proposal identifies nothing; the write that follows fails on the missing type anyway.
		if (normalize(typeName).isEmpty()) {
			return Optional.empty();
		}

		// FamilyCare filters on the actualisation's own date, which is the intake date the write set, so the window is the day.
		final var candidates = listActualisations(municipalityId, applicantPartyId, date, date).stream()
			.filter(actualisation -> actualisation.id() != null)
			.filter(actualisation -> normalize(actualisation.type()).equals(normalize(typeName)))
			.filter(actualisation -> isOn(actualisation.date(), date))
			.filter(actualisation -> !claimedByAnotherErrand.test(actualisation.id()))
			.collect(toMap(ActualisationSummary::id, identity(), (first, second) -> first, LinkedHashMap::new));

		if (candidates.size() > 1) {
			throw Problem.valueOf(CONFLICT, AMBIGUOUS_EARLIER_ATTEMPT.formatted(candidates.size(),
				candidates.keySet().stream().map(String::valueOf).collect(joining(", "))));
		}
		return candidates.values().stream().findFirst();
	}

	/** Whether a FamilyCare date, with or without a time of day, is the given day. */
	private static boolean isOn(final String actualisationDate, final LocalDate date) {
		return ofNullable(actualisationDate).map(String::trim).filter(text -> text.startsWith(date.toString())).isPresent();
	}

	/** The name of the actualisation type with the given id in the proposal, or {@code null} when it has none. */
	private static String typeNameOf(final PersonBasedAktualiseringProposalDTO proposal, final Integer typeId) {
		return ofNullable(proposal)
			.map(PersonBasedAktualiseringProposalDTO::getActualisationTypes)
			.orElseGet(List::of).stream()
			.filter(Objects::nonNull)
			.filter(type -> (type.getId() != null) && type.getId().equals(typeId))
			.findFirst()
			.map(PersonBasedAktualiseringsInfoDTO::getName)
			.orElse(null);
	}

	/** What a create needs, decided before anything is written to Lifecare. */
	private record Plan(String assignedUserId, String typeName, PostAktualiseringsBodyRequest body) {
	}

	/** The configured catalogue names, with the nyansökan actualisation type for a new application. */
	private ActualisationProperties namesFor(final boolean newApplication) {
		if (newApplication) {
			return actualisationProperties.forNewApplication();
		}
		return actualisationProperties;
	}

	/**
	 * The person's open financial-assistance service (insats) id in Lifecare — the key Lifecare's own case reads take
	 * (reminders, jobbstimulans, document proposals). Chosen by the same rule an intake is linked with, see
	 * {@link ActualisationAssembler#linkedServiceId}.
	 *
	 * @param  partyId the person's partyId
	 * @return         the service id, or empty when the person has no open EB insats
	 */
	public Optional<Integer> findFinancialAssistanceServiceId(final String municipalityId, final String partyId) {
		final var proposal = lifecareFamilyCareIntegration.getActualisationProposal(municipalityId, partyId);
		return ActualisationAssembler.linkedServiceId(proposal, actualisationProperties);
	}

	/** Best-effort caseworker resolution — never blocks intake creation; a lookup failure resolves to no caseworker. */
	private Optional<ResolvedCaseworker> resolveCaseworker(final String municipalityId, final String applicantPartyId, final LocalDate date) {
		try {
			return caseworkerResolver.resolve(municipalityId, applicantPartyId, date);
		} catch (final RuntimeException e) {
			LOG.warn("Could not resolve caseworker for actualisation; creating intake without one: {}", e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * List the actualisations (case intakes) registered on a person in the given period, mapped to the privacy-safe
	 * {@link ActualisationSummary} projection (the personal identity number is dropped). The dates bound the Lifecare query
	 * and are
	 * formatted as ISO local dates. An empty/absent FamilyCare page maps to an empty list.
	 *
	 * @param  partyId  the person's partyId (the actualisation owner)
	 * @param  fromDate the inclusive start of the listing period
	 * @param  toDate   the inclusive end of the listing period
	 * @return          the person's actualisations in the period (newest-first as Lifecare returns them)
	 */
	public List<ActualisationSummary> listActualisations(final String municipalityId, final String partyId, final LocalDate fromDate, final LocalDate toDate) {
		return ofNullable(lifecareFamilyCareIntegration.getActualisations(municipalityId, partyId, fromDate, toDate))
			.map(ApiPaginationCompositePersonBasedAktualiseringDTO::getResult)
			.orElseGet(List::of)
			.stream()
			.map(ActualisationService::toSummary)
			.toList();
	}

	/**
	 * Project the generated FamilyCare DTO onto the privacy-safe summary — deliberately omitting the personId (personal
	 * identity number).
	 */
	private static ActualisationSummary toSummary(final PersonBasedAktualiseringDTO dto) {
		return new ActualisationSummary(dto.getId(), dto.getType(), dto.getName(), dto.getDate(), dto.getReason(), dto.getRegards(),
			dto.getFromWho(), dto.getCaseworker(), dto.getOrganization(), dto.getStatus(), dto.getInvestigationId(), dto.getServiceId(), dto.getDecisionId());
	}

	/**
	 * Upload a generated PDF and bind it to an existing Lifecare actualisation (the document write-back used by the
	 * conversation-archiving job). The file is sent as {@code application/pdf}.
	 *
	 * @param actualisationId the Lifecare actualisation the document is bound to
	 * @param attachment      the document's type codes, title, sender and PDF content
	 */
	public void uploadAttachment(final String municipalityId, final Integer actualisationId, final AttachmentUpload attachment) {
		lifecareFamilyCareIntegration.postActualisationAttachment(municipalityId, actualisationId, attachment);
	}
}
