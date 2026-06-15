package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Result of the application-eligibility check. Carries the ordered {@link ApplicationSuggestion}s the citizen can be
 * offered (one flagged {@code recommended}) together with the facts the decision was built from, so the frontend can
 * render the "kontakta socialsekreterare" path and explain the constellation handling.
 */
@Schema(description = "Eligibility result: which application(s) the citizen should be offered, plus the supporting facts.")
public class EligibilityResponse {

	@Schema(description = "Suggested applications, ordered with the recommended one first. Empty only when nothing can be offered automatically.")
	private List<ApplicationSuggestion> suggestions;

	@Schema(description = "True when a caseworker must handle the case (e.g. the constellation differs from a previous application).", examples = "false")
	private boolean requiresCaseworker;

	@Schema(description = "Machine-readable code for the situation that drove the suggestion",
		examples = "NO_DECISION_FOR_CURRENT_MONTH",
		allowableValues = {
			"NO_OPEN_CASE", "DECISION_FOR_CURRENT_MONTH", "NO_DECISION_FOR_CURRENT_MONTH", "RECENT_APPLICATION", "CONSTELLATION_MISMATCH"
		})
	private String reasonCode;

	@Schema(description = "Human-readable Swedish explanation of the suggestion", examples = "Öppet ärende utan beslut för innevarande månad. Föreslår återansökan.")
	private String message;

	@Schema(description = "True when an application by either applicant was already submitted within the window in this system", examples = "false")
	private boolean hasRecentApplication;

	@Schema(description = "The duplicate-application window in days that was applied", examples = "90")
	private int windowDays;

	@Schema(description = "True when Lifecare shows an open EB case (for both applicants when applying together)", examples = "true")
	private boolean hasOpenCase;

	@Schema(description = "True when Lifecare shows a decision covering the current month", examples = "false")
	private boolean hasDecisionForCurrentMonth;

	@Schema(description = "Month (1-12) of the most recent Lifecare decision, when one exists", examples = "5")
	private Integer latestDecisionPeriodMonth;

	@Schema(description = "Year of the most recent Lifecare decision, when one exists", examples = "2026")
	private Integer latestDecisionPeriodYear;

	@Schema(description = "True when Lifecare shows a previous normberäkning", examples = "true")
	private boolean hasPreviousCalculation;

	@Schema(description = "Whether the requested constellation (alone vs with the given partner) matches the previous application/decision. Null when there is nothing to compare against.", examples = "true")
	private Boolean constellationMatchesPrevious;

	@Schema(description = "True when the Lifecare lookup succeeded. False means the answer is degraded (DB-only).", examples = "true")
	private boolean lifecareChecked;

	@Schema(description = "True when the request included a co-applicant (medsökande)", examples = "false")
	private boolean hasCoApplicant;

	public static EligibilityResponse create() {
		return new EligibilityResponse();
	}

	public List<ApplicationSuggestion> getSuggestions() {
		return suggestions;
	}

	public void setSuggestions(final List<ApplicationSuggestion> suggestions) {
		this.suggestions = suggestions;
	}

	public EligibilityResponse withSuggestions(final List<ApplicationSuggestion> suggestions) {
		this.suggestions = suggestions;
		return this;
	}

	public boolean isRequiresCaseworker() {
		return requiresCaseworker;
	}

	public void setRequiresCaseworker(final boolean requiresCaseworker) {
		this.requiresCaseworker = requiresCaseworker;
	}

	public EligibilityResponse withRequiresCaseworker(final boolean requiresCaseworker) {
		this.requiresCaseworker = requiresCaseworker;
		return this;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public void setReasonCode(final String reasonCode) {
		this.reasonCode = reasonCode;
	}

	public EligibilityResponse withReasonCode(final String reasonCode) {
		this.reasonCode = reasonCode;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public EligibilityResponse withMessage(final String message) {
		this.message = message;
		return this;
	}

	public boolean isHasRecentApplication() {
		return hasRecentApplication;
	}

	public void setHasRecentApplication(final boolean hasRecentApplication) {
		this.hasRecentApplication = hasRecentApplication;
	}

	public EligibilityResponse withHasRecentApplication(final boolean hasRecentApplication) {
		this.hasRecentApplication = hasRecentApplication;
		return this;
	}

	public int getWindowDays() {
		return windowDays;
	}

	public void setWindowDays(final int windowDays) {
		this.windowDays = windowDays;
	}

	public EligibilityResponse withWindowDays(final int windowDays) {
		this.windowDays = windowDays;
		return this;
	}

	public boolean isHasOpenCase() {
		return hasOpenCase;
	}

	public void setHasOpenCase(final boolean hasOpenCase) {
		this.hasOpenCase = hasOpenCase;
	}

	public EligibilityResponse withHasOpenCase(final boolean hasOpenCase) {
		this.hasOpenCase = hasOpenCase;
		return this;
	}

	public boolean isHasDecisionForCurrentMonth() {
		return hasDecisionForCurrentMonth;
	}

	public void setHasDecisionForCurrentMonth(final boolean hasDecisionForCurrentMonth) {
		this.hasDecisionForCurrentMonth = hasDecisionForCurrentMonth;
	}

	public EligibilityResponse withHasDecisionForCurrentMonth(final boolean hasDecisionForCurrentMonth) {
		this.hasDecisionForCurrentMonth = hasDecisionForCurrentMonth;
		return this;
	}

	public Integer getLatestDecisionPeriodMonth() {
		return latestDecisionPeriodMonth;
	}

	public void setLatestDecisionPeriodMonth(final Integer latestDecisionPeriodMonth) {
		this.latestDecisionPeriodMonth = latestDecisionPeriodMonth;
	}

	public EligibilityResponse withLatestDecisionPeriodMonth(final Integer latestDecisionPeriodMonth) {
		this.latestDecisionPeriodMonth = latestDecisionPeriodMonth;
		return this;
	}

	public Integer getLatestDecisionPeriodYear() {
		return latestDecisionPeriodYear;
	}

	public void setLatestDecisionPeriodYear(final Integer latestDecisionPeriodYear) {
		this.latestDecisionPeriodYear = latestDecisionPeriodYear;
	}

	public EligibilityResponse withLatestDecisionPeriodYear(final Integer latestDecisionPeriodYear) {
		this.latestDecisionPeriodYear = latestDecisionPeriodYear;
		return this;
	}

	public boolean isHasPreviousCalculation() {
		return hasPreviousCalculation;
	}

	public void setHasPreviousCalculation(final boolean hasPreviousCalculation) {
		this.hasPreviousCalculation = hasPreviousCalculation;
	}

	public EligibilityResponse withHasPreviousCalculation(final boolean hasPreviousCalculation) {
		this.hasPreviousCalculation = hasPreviousCalculation;
		return this;
	}

	public Boolean getConstellationMatchesPrevious() {
		return constellationMatchesPrevious;
	}

	public void setConstellationMatchesPrevious(final Boolean constellationMatchesPrevious) {
		this.constellationMatchesPrevious = constellationMatchesPrevious;
	}

	public EligibilityResponse withConstellationMatchesPrevious(final Boolean constellationMatchesPrevious) {
		this.constellationMatchesPrevious = constellationMatchesPrevious;
		return this;
	}

	public boolean isLifecareChecked() {
		return lifecareChecked;
	}

	public void setLifecareChecked(final boolean lifecareChecked) {
		this.lifecareChecked = lifecareChecked;
	}

	public EligibilityResponse withLifecareChecked(final boolean lifecareChecked) {
		this.lifecareChecked = lifecareChecked;
		return this;
	}

	public boolean isHasCoApplicant() {
		return hasCoApplicant;
	}

	public void setHasCoApplicant(final boolean hasCoApplicant) {
		this.hasCoApplicant = hasCoApplicant;
	}

	public EligibilityResponse withHasCoApplicant(final boolean hasCoApplicant) {
		this.hasCoApplicant = hasCoApplicant;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final EligibilityResponse that = (EligibilityResponse) o;
		return requiresCaseworker == that.requiresCaseworker && hasRecentApplication == that.hasRecentApplication
			&& windowDays == that.windowDays && hasOpenCase == that.hasOpenCase
			&& hasDecisionForCurrentMonth == that.hasDecisionForCurrentMonth
			&& hasPreviousCalculation == that.hasPreviousCalculation && lifecareChecked == that.lifecareChecked
			&& hasCoApplicant == that.hasCoApplicant && Objects.equals(suggestions, that.suggestions)
			&& Objects.equals(reasonCode, that.reasonCode) && Objects.equals(message, that.message)
			&& Objects.equals(latestDecisionPeriodMonth, that.latestDecisionPeriodMonth)
			&& Objects.equals(latestDecisionPeriodYear, that.latestDecisionPeriodYear)
			&& Objects.equals(constellationMatchesPrevious, that.constellationMatchesPrevious);
	}

	@Override
	public int hashCode() {
		return Objects.hash(suggestions, requiresCaseworker, reasonCode, message, hasRecentApplication, windowDays,
			hasOpenCase, hasDecisionForCurrentMonth, latestDecisionPeriodMonth, latestDecisionPeriodYear,
			hasPreviousCalculation, constellationMatchesPrevious, lifecareChecked, hasCoApplicant);
	}

	@Override
	public String toString() {
		return "EligibilityResponse{suggestions=" + suggestions + ", requiresCaseworker=" + requiresCaseworker
			+ ", reasonCode='" + reasonCode + "', message='" + message + "', hasRecentApplication=" + hasRecentApplication
			+ ", windowDays=" + windowDays + ", hasOpenCase=" + hasOpenCase + ", hasDecisionForCurrentMonth="
			+ hasDecisionForCurrentMonth + ", latestDecisionPeriodMonth=" + latestDecisionPeriodMonth
			+ ", latestDecisionPeriodYear=" + latestDecisionPeriodYear + ", hasPreviousCalculation=" + hasPreviousCalculation
			+ ", constellationMatchesPrevious=" + constellationMatchesPrevious + ", lifecareChecked=" + lifecareChecked
			+ ", hasCoApplicant=" + hasCoApplicant + '}';
	}
}
