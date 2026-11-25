package se.sundsvall.caremanagement.lifecare.service;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * The Lifecare actualisation codes verksamheten named for a financial-assistance återansökan (Regelverk Drakel,
 * 2026-09-21). They are configured rather than hardcoded because the values are Lifecare catalogue entries, not our
 * own: a renamed entry has to be followable without a release.
 *
 * <p>
 * Matching is on the catalogue entry's name, case- and whitespace-insensitive, and falls back to the first offered
 * value when the name is not in the proposal — an intake must never be blocked by a catalogue that has moved. Each
 * miss is logged as a warning by {@code ActualisationService}, because a silent fallback is exactly the guess this
 * configuration exists to remove. <strong>Whether a miss should instead refuse to create the actualisation is an
 * open question to verksamheten.</strong>
 * </p>
 *
 * <p>
 * The specify-type, working-status, service and investigation selections stay on "first offered": verksamheten has
 * not named them. So is the caseworker — "Handläggare = Rakel" in the regelverk collides with
 * {@code CaseworkerResolver}, which sets the applicant's previous caseworker, and that question is unanswered.
 * </p>
 *
 * <p>
 * A nyansökan takes its own type. The återansökan type links the intake to the person's open EB insats
 * ({@code ServiceTypes} 27/28), which a first-time applicant does not have; FamilyCare refuses that combination with
 * {@code 403 "Aktualisering"}. The nyansökan type links an ekonomiutredning instead.
 * </p>
 *
 * @param type               the actualisation type for an återansökan or tilläggsansökan, e.g.
 *                           {@code Ek Återansökan Digital Ekonomiskt bistånd}
 * @param fromWho            who the actualisation came from, e.g. {@code Den enskilde}
 * @param reason             the actualisation reason, e.g. {@code Ekonomiskt bistånd}
 * @param organisation       the owning organisation, e.g. {@code Ekonomiskt bistånd}
 * @param newApplicationType the actualisation type for a nyansökan, e.g.
 *                           {@code EK Nyansökan Digital Ekonomiskt bistånd}
 */
@Validated
@ConfigurationProperties(prefix = "lifecare.actualisation")
public record ActualisationProperties(

	@NotBlank @DefaultValue("Ek Återansökan Digital Ekonomiskt bistånd") String type,

	@NotBlank @DefaultValue("Den enskilde") String fromWho,

	@NotBlank @DefaultValue("Ekonomiskt bistånd") String reason,

	@NotBlank @DefaultValue("Ekonomiskt bistånd") String organisation,

	@NotBlank @DefaultValue("EK Nyansökan Digital Ekonomiskt bistånd") String newApplicationType) {

	/** The same names with the nyansökan type in place of the återansökan one. */
	public ActualisationProperties forNewApplication() {
		return new ActualisationProperties(newApplicationType, fromWho, reason, organisation, newApplicationType);
	}
}
