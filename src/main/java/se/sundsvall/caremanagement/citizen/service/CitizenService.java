package se.sundsvall.caremanagement.citizen.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.citizen.integration.CitizenClient;

import static org.springframework.util.StringUtils.hasText;

/**
 * Citizen lookups for case preparation. Wraps {@link CitizenClient}; translates between a partyId (personId GUID) — the
 * public identifier the frontend works with — and the personnummer external systems (Lifecare, SSBTEK) need, so callers
 * never have to accept or pass personnummer at the API edge. Upstream failures already surface as dept44 problems via
 * the client's {@code ProblemErrorDecoder}.
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

	/**
	 * Resolve the partyId (personId GUID) behind a personnummer — the reverse of
	 * {@link #getPersonalNumber(String, String)}, used to hand personnummer-bearing data from external systems back to the
	 * frontend as partyIds.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  personalNumber the citizen's personnummer
	 * @return                the partyId, or empty when the citizen service has none (204 No Content)
	 */
	public Optional<String> getPartyId(final String municipalityId, final String personalNumber) {
		return Optional.ofNullable(citizenClient.getGuid(municipalityId, personalNumber)).filter(value -> hasText(value));
	}

	/**
	 * Whether the citizen has skyddad identitet in folkbokföring — a sekretessmarkering ({@code protectedNR}) or skyddad
	 * folkbokföring/classification ({@code classified}). The lookup asks for classified data ({@code ShowClassified=true})
	 * since otherwise the citizen service filters protected persons out entirely; the flags only surface that way.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  partyId        the citizen's partyId (personId GUID)
	 * @return                {@code true} when either protection flag is set; {@code false} when neither is set or the
	 *                        citizen service has no record (204 No Content)
	 */
	public boolean hasProtectedIdentity(final String municipalityId, final String partyId) {
		return Optional.ofNullable(citizenClient.getCitizen(municipalityId, partyId, true))
			.map(citizen -> hasText(citizen.getClassified()) || hasText(citizen.getProtectedNR()))
			.orElse(false);
	}
}
