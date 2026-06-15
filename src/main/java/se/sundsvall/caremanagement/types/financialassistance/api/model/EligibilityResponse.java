package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Result of the application-eligibility check (gemensam ingång). Carries the ordered {@link ApplicationSuggestion}s the
 * citizen can be offered (one flagged {@code recommended}) together with the facts each gate was decided from, so the
 * frontend can explain the routing.
 */
@Schema(description = "Eligibility result: which application(s) the citizen should be offered, plus the supporting facts.")
public class EligibilityResponse {

	@Schema(description = "Suggested applications, ordered with the recommended one first.")
	private List<ApplicationSuggestion> suggestions;

	@Schema(description = "Machine-readable code for the gate that drove the suggestion",
		examples = "EXISTING_CASE",
		allowableValues = {
			"NO_EXISTING_CASE", "CIVILSTAND_CHANGED", "EXISTING_CASE"
		})
	private String reasonCode;

	@Schema(description = "Human-readable Swedish explanation of the suggestion", examples = "Öppet ärende utan beslut för innevarande månad. Föreslår återansökan.")
	private String message;

	@Schema(description = "True when the applicant already has an EB errand in caremanagement", examples = "true")
	private boolean existsInCm;

	@Schema(description = "True when the applicant has an EB footprint in Lifecare (aktualisering/beslut/normberäkning)", examples = "true")
	private boolean existsInLc;

	@Schema(description = "Whether the requested civilstånd (alone vs with a partner) matches the previous application. Null when not evaluated (no existing case).", examples = "true")
	private Boolean civilstandMatches;

	@Schema(description = "The duplicate-application window in days that was applied to the per-month check", examples = "90")
	private int windowDays;

	@Schema(description = "True when an application/decision already exists for the current month", examples = "false")
	private boolean applicationExistsThisMonth;

	@Schema(description = "True when an application/decision already exists for next month", examples = "false")
	private boolean applicationExistsNextMonth;

	@Schema(description = "True when Lifecare shows a decision for the current month (the current month is decided/closed)", examples = "false")
	private boolean currentMonthDecided;

	@Schema(description = "Month (1-12) of the most recent Lifecare decision, when one exists", examples = "5")
	private Integer latestDecisionPeriodMonth;

	@Schema(description = "Year of the most recent Lifecare decision, when one exists", examples = "2026")
	private Integer latestDecisionPeriodYear;

	@Schema(description = "True when Lifecare shows a previous normberäkning", examples = "true")
	private boolean hasPreviousCalculation;

	@Schema(description = "True when the Lifecare lookup succeeded. False means the answer is degraded (CM-only).", examples = "true")
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

	public boolean isExistsInCm() {
		return existsInCm;
	}

	public void setExistsInCm(final boolean existsInCm) {
		this.existsInCm = existsInCm;
	}

	public EligibilityResponse withExistsInCm(final boolean existsInCm) {
		this.existsInCm = existsInCm;
		return this;
	}

	public boolean isExistsInLc() {
		return existsInLc;
	}

	public void setExistsInLc(final boolean existsInLc) {
		this.existsInLc = existsInLc;
	}

	public EligibilityResponse withExistsInLc(final boolean existsInLc) {
		this.existsInLc = existsInLc;
		return this;
	}

	public Boolean getCivilstandMatches() {
		return civilstandMatches;
	}

	public void setCivilstandMatches(final Boolean civilstandMatches) {
		this.civilstandMatches = civilstandMatches;
	}

	public EligibilityResponse withCivilstandMatches(final Boolean civilstandMatches) {
		this.civilstandMatches = civilstandMatches;
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

	public boolean isApplicationExistsThisMonth() {
		return applicationExistsThisMonth;
	}

	public void setApplicationExistsThisMonth(final boolean applicationExistsThisMonth) {
		this.applicationExistsThisMonth = applicationExistsThisMonth;
	}

	public EligibilityResponse withApplicationExistsThisMonth(final boolean applicationExistsThisMonth) {
		this.applicationExistsThisMonth = applicationExistsThisMonth;
		return this;
	}

	public boolean isApplicationExistsNextMonth() {
		return applicationExistsNextMonth;
	}

	public void setApplicationExistsNextMonth(final boolean applicationExistsNextMonth) {
		this.applicationExistsNextMonth = applicationExistsNextMonth;
	}

	public EligibilityResponse withApplicationExistsNextMonth(final boolean applicationExistsNextMonth) {
		this.applicationExistsNextMonth = applicationExistsNextMonth;
		return this;
	}

	public boolean isCurrentMonthDecided() {
		return currentMonthDecided;
	}

	public void setCurrentMonthDecided(final boolean currentMonthDecided) {
		this.currentMonthDecided = currentMonthDecided;
	}

	public EligibilityResponse withCurrentMonthDecided(final boolean currentMonthDecided) {
		this.currentMonthDecided = currentMonthDecided;
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
		return existsInCm == that.existsInCm && existsInLc == that.existsInLc && windowDays == that.windowDays
			&& applicationExistsThisMonth == that.applicationExistsThisMonth
			&& applicationExistsNextMonth == that.applicationExistsNextMonth && currentMonthDecided == that.currentMonthDecided
			&& hasPreviousCalculation == that.hasPreviousCalculation && lifecareChecked == that.lifecareChecked
			&& hasCoApplicant == that.hasCoApplicant && Objects.equals(suggestions, that.suggestions)
			&& Objects.equals(reasonCode, that.reasonCode) && Objects.equals(message, that.message)
			&& Objects.equals(civilstandMatches, that.civilstandMatches)
			&& Objects.equals(latestDecisionPeriodMonth, that.latestDecisionPeriodMonth)
			&& Objects.equals(latestDecisionPeriodYear, that.latestDecisionPeriodYear);
	}

	@Override
	public int hashCode() {
		return Objects.hash(suggestions, reasonCode, message, existsInCm, existsInLc, civilstandMatches, windowDays,
			applicationExistsThisMonth, applicationExistsNextMonth, currentMonthDecided, latestDecisionPeriodMonth,
			latestDecisionPeriodYear, hasPreviousCalculation, lifecareChecked, hasCoApplicant);
	}

	@Override
	public String toString() {
		return "EligibilityResponse{suggestions=" + suggestions + ", reasonCode='" + reasonCode + "', message='" + message
			+ "', existsInCm=" + existsInCm + ", existsInLc=" + existsInLc + ", civilstandMatches=" + civilstandMatches
			+ ", windowDays=" + windowDays + ", applicationExistsThisMonth=" + applicationExistsThisMonth
			+ ", applicationExistsNextMonth=" + applicationExistsNextMonth + ", currentMonthDecided=" + currentMonthDecided
			+ ", latestDecisionPeriodMonth=" + latestDecisionPeriodMonth + ", latestDecisionPeriodYear="
			+ latestDecisionPeriodYear + ", hasPreviousCalculation=" + hasPreviousCalculation + ", lifecareChecked="
			+ lifecareChecked + ", hasCoApplicant=" + hasCoApplicant + '}';
	}
}
