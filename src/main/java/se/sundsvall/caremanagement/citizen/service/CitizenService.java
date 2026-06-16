package se.sundsvall.caremanagement.citizen.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.citizen.integration.CitizenClient;

import static org.springframework.util.StringUtils.hasText;

/**
 * Citizen lookups for case preparation. Wraps {@link CitizenClient}; the operation needed so far is resolving a
 * personnummer from a partyId (personId GUID) — the public identifier the frontend works with — so callers never have
 * to accept or pass personnummer around. Upstream failures already surface as dept44 problems via the client's
 * {@code ProblemErrorDecoder}.
 */
@Service
public class CitizenService {

	private final CitizenClient citizenClient;

	CitizenService(final CitizenClient citizenClient) {
		this.citizenClient = citizenClient;
	}

	/**
	 * Resolve the personnummer behind a partyId (personId GUID).
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  partyId        the citizen's partyId (personId GUID)
	 * @return                the personnummer, or empty when the citizen service has none (204 No Content)
	 */
	public Optional<String> getPersonalNumber(final String municipalityId, final String partyId) {
		return Optional.ofNullable(citizenClient.getPersonNumber(municipalityId, partyId)).filter(value -> hasText(value));
	}
}
