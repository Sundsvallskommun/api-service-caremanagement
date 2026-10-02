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

	private static final String FLAG_NOT_SET = "N";

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
	 * folkbokföring/classification ({@code classified}). Citizen v3 answers both flags for every person, {@code "N"} when
	 * the flag is not set, so a flag counts only when it holds something other than {@code "N"} (the reading Open
	 * ePlatform's SMEX person provider uses).
	 * <p>
	 * The lookup does not ask for classified data ({@code ShowClassified=false}). The flags come back without it, and the
	 * citizen service leaves a classified person out of such an answer instead, so an empty answer is not a clearance.
	 * {@code ShowClassified=true} would only add the protected person's addresses, which this check does not need, and
	 * Citizen v3 answers it with 500 in the test environment.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  partyId        the citizen's partyId (personId GUID)
	 * @return                {@code true} when either protection flag is set or the citizen service returns no record
	 *                        (204 No Content); {@code false} when both flags are {@code "N"} or absent
	 */
	public boolean hasProtectedIdentity(final String municipalityId, final String partyId) {
		return Optional.ofNullable(citizenClient.getCitizen(municipalityId, partyId, false))
			.map(citizen -> isSet(citizen.getClassified()) || isSet(citizen.getProtectedNR()))
			.orElse(true);
	}

	private static boolean isSet(final String flag) {
		return hasText(flag) && !FLAG_NOT_SET.equalsIgnoreCase(flag.strip());
	}
}
