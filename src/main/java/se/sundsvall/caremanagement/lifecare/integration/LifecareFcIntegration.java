package se.sundsvall.caremanagement.lifecare.integration;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedExecutionDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedInvestigationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedPaymentDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedResourceAllocationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedServiceDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedContactDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedPersonDTO;
import generated.se.sundsvall.lifecarefc.PostAktualiseringsBodyRequest;
import generated.se.sundsvall.lifecarefc.PostCalculationBodyRequest;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

/**
 * Thin wrapper over {@link LifecareFcClient}. Every call goes through {@link #call(String, Supplier)}, which translates
 * any transport/FC failure into a {@code BAD_GATEWAY} problem carrying the upstream status into the log and problem
 * detail. Deliberately logs no {@code personId} or request/response payloads — FC carries personnummer and income data
 * (sprint privacy rule, vof-ekonomiskt-bistand/CLAUDE.md).
 */
@Component
public class LifecareFcIntegration {

	public static final String CLIENT_ID = LifecareFcClient.class.getSimpleName();

	private static final Logger LOG = LoggerFactory.getLogger(LifecareFcIntegration.class);

	private final LifecareFcClient lifecareFcClient;

	public LifecareFcIntegration(final LifecareFcClient lifecareFcClient) {
		this.lifecareFcClient = lifecareFcClient;
	}

	// ---- Person-based reads ------------------------------------------------------------------------------------------

	public PersonBasedPersonDTO getPerson(final String personId) {
		return call("fetching person", () -> lifecareFcClient.getPerson(personId));
	}

	public List<PersonBasedContactDTO> getContacts(final String personId) {
		return call("fetching contacts", () -> lifecareFcClient.getContacts(personId));
	}

	public ApiPaginationCompositePersonBasedAktualiseringDTO getActualisations(final String personId, final String startDate, final String endDate, final Integer pageSize, final Integer pageNr, final Boolean ascending) {
		return call("fetching aktualiseringar", () -> lifecareFcClient.getActualisations(personId, startDate, endDate, pageSize, pageNr, ascending));
	}

	public ApiPaginationCompositePersonBasedCalculationDTO getCalculations(final String personId, final String startDate, final String endDate, final Integer pageSize, final Integer pageNr, final Boolean ascending) {
		return call("fetching normberäkningar", () -> lifecareFcClient.getCalculations(personId, startDate, endDate, pageSize, pageNr, ascending));
	}

	public ApiPaginationCompositePersonBasedDecisionDTO getDecisions(final String personId, final String startDate, final String endDate, final Integer pageSize, final Integer pageNr, final Boolean ascending) {
		return call("fetching beslut", () -> lifecareFcClient.getDecisions(personId, startDate, endDate, pageSize, pageNr, ascending));
	}

	public ApiPaginationCompositePersonBasedPaymentDTO getPayments(final String personId, final String startDate, final String endDate, final Integer pageSize, final Integer pageNr, final Boolean ascending) {
		return call("fetching utbetalningar", () -> lifecareFcClient.getPayments(personId, startDate, endDate, pageSize, pageNr, ascending));
	}

	public ApiPaginationCompositePersonBasedInvestigationDTO getInvestigations(final String personId, final String startDate, final String endDate, final Integer pageSize, final Integer pageNr, final Boolean ascending) {
		return call("fetching utredningar", () -> lifecareFcClient.getInvestigations(personId, startDate, endDate, pageSize, pageNr, ascending));
	}

	public ApiPaginationCompositePersonBasedServiceDTO getServices(final String personId, final String startDate, final String endDate, final Integer pageSize, final Integer pageNr, final Boolean ascending) {
		return call("fetching insatser", () -> lifecareFcClient.getServices(personId, startDate, endDate, pageSize, pageNr, ascending));
	}

	public ApiPaginationCompositePersonBasedExecutionDTO getExecutions(final String personId, final String startDate, final String endDate, final Integer pageSize, final Integer pageNr, final Boolean ascending) {
		return call("fetching verkställigheter", () -> lifecareFcClient.getExecutions(personId, startDate, endDate, pageSize, pageNr, ascending));
	}

	public ApiPaginationCompositePersonBasedResourceAllocationDTO getResourceAllocations(final String personId, final String startDate, final String endDate, final Integer pageSize, final Integer pageNr, final Boolean ascending) {
		return call("fetching resursfördelning", () -> lifecareFcClient.getResourceAllocations(personId, startDate, endDate, pageSize, pageNr, ascending));
	}

	// ---- Write-back (aktualisering + normberäkning) and the proposals that drive it ----------------------------------

	public PersonBasedAktualiseringProposalDTO getActualisationProposal(final String personId) {
		return call("fetching aktualisering proposal", () -> lifecareFcClient.getActualisationProposal(personId));
	}

	public Integer createActualisation(final PostAktualiseringsBodyRequest body) {
		return call("creating aktualisering", () -> lifecareFcClient.createActualisation(body));
	}

	public PersonBasedCalculationProposalDTO getCalculationProposal(final String personId) {
		return call("fetching calculation proposal", () -> lifecareFcClient.getCalculationProposal(personId));
	}

	public Integer createCalculation(final PostCalculationBodyRequest body) {
		return call("creating normberäkning", () -> lifecareFcClient.createCalculation(body));
	}

	/**
	 * Runs an FC call, translating any failure into a {@code BAD_GATEWAY} problem. The {@code action} is a short verb
	 * phrase ("creating aktualisering") used only for the log/problem detail — never a personId or payload.
	 */
	private <T> T call(final String action, final Supplier<T> operation) {
		try {
			return operation.get();
		} catch (final Exception e) {
			LOG.error("Error {} in Lifecare FC: {}", action, describe(e), e);
			throw Problem.valueOf(BAD_GATEWAY, "Error %s in Lifecare FC: %s".formatted(action, describe(e)));
		}
	}

	/** Short upstream descriptor (HTTP status when available) to make failures self-diagnosing without leaking payloads. */
	private static String describe(final Throwable e) {
		if (e instanceof final ThrowableProblem problem) {
			return ofNullable(problem.getStatus()).map(status -> status.value() + " " + problem.getMessage()).orElseGet(problem::getMessage);
		}
		return e.getClass().getSimpleName() + ": " + e.getMessage();
	}
}
