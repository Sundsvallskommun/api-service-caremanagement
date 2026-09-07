package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * Result of the application-eligibility check (common entry point). Carries the ordered {@link ApplicationSuggestion}s
 * the
 * citizen can be offered (one flagged {@code recommended}) together with the facts each gate was decided from, so the
 * frontend can explain the routing.
 */
@Schema(description = "Eligibility result: which application(s) the citizen should be offered, plus the supporting facts.")
public class EligibilityResponse {

	@ArraySchema(arraySchema = @Schema(description = "Suggested applications, ordered with the recommended one first."), schema = @Schema(implementation = ApplicationSuggestion.class))
	private List<ApplicationSuggestion> suggestions;

	@Schema(description = "Machine-readable code for the gate that drove the suggestion",
		examples = "EXISTING_CASE",
		allowableValues = {
			"NO_EXISTING_CASE", "MARITAL_STATUS_CHANGED", "RECENTLY_CLOSED", "NO_RECENT_DECISION", "ONGOING_APPLICATION",
			"EXISTING_CASE", "ALL_TYPES_TEST"
		})
	private String reasonCode;

	@Schema(description = "Human-readable Swedish explanation of the suggestion",
		examples = "Befintligt ärende utan beslut för aktuell månad. Föreslår en återansökan för aktuell månad.")
	private String message;

	@Schema(description = "Swedish introduction shown to the citizen above the suggestion list, phrased for one or two applicants. Null when no application can be offered.",
		examples = "Utifrån dina uppgifter kan du göra någon av följande ansökningar:")
	private String introText;

	@Schema(description = "True when the applicant already has a financial assistance errand in caremanagement", examples = "true")
	private boolean existsInCm;

	@Schema(description = "True when the applicant has a financial assistance footprint in Lifecare (actualisation/decision/calculation)", examples = "true")
	private boolean existsInLc;

	@Schema(description = "True when Lifecare shows an actualisation with an open status, false when the statuses were readable but none is open, null when no actualisation carried a readable status (or Lifecare was not reached).", examples = "true")
	private Boolean hasOpenCase;

	@Schema(description = "Whether the requested marital status (alone vs with a partner) matches the previous application. Null when not evaluated (no existing case).", examples = "true")
	private Boolean maritalStatusMatches;

	@Schema(description = "The staleness bound in days applied to ongoing caremanagement applications in the per-month check", examples = "90")
	private int windowDays;

	@Schema(description = "True when the current month is already taken — an ongoing application in caremanagement, or a decision in Lifecare", examples = "false")
	private boolean applicationExistsThisMonth;

	@Schema(description = "True when next month is already taken — an ongoing application in caremanagement, or a decision in Lifecare", examples = "false")
	private boolean applicationExistsNextMonth;

	@Schema(description = "True when Lifecare shows a decision for the current month (the current month is decided/closed)", examples = "false")
	private boolean currentMonthDecided;

	@Schema(description = "True when Lifecare shows a decision for the previous month", examples = "true")
	private boolean previousMonthDecided;

	@Schema(description = "True when Lifecare shows a decision for the month before the previous one", examples = "false")
	private boolean monthBeforePreviousDecided;

	@Schema(description = "Month (1-12) of the most recent Lifecare decision, when one exists", examples = "5")
	private Integer latestDecisionPeriodMonth;

	@Schema(description = "Year of the most recent Lifecare decision, when one exists", examples = "2026")
	private Integer latestDecisionPeriodYear;

	@Schema(description = "True when Lifecare shows a previous calculation", examples = "true")
	private boolean hasPreviousCalculation;

	@Schema(description = "True when the Lifecare lookup succeeded. False means the answer is degraded (CM-only).", examples = "true")
	private boolean lifecareChecked;

	@Schema(description = "True when the request included a co-applicant (co-applicant)", examples = "false")
	private boolean hasCoApplicant;

	@Schema(description = "When reasonCode is RECENTLY_CLOSED: the id of the recently closed errand a caseworker can reopen (in Lifecare) and release. Null otherwise.", examples = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
	private String reopenableErrandId;

	@Schema(description = "When reasonCode is RECENTLY_CLOSED: when the reopenable errand was closed. Null otherwise.", examples = "2026-06-20T10:15:30Z")
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime closedAt;

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

	public String getIntroText() {
		return introText;
	}

	public void setIntroText(final String introText) {
		this.introText = introText;
	}

	public EligibilityResponse withIntroText(final String introText) {
		this.introText = introText;
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

	public Boolean getHasOpenCase() {
		return hasOpenCase;
	}

	public void setHasOpenCase(final Boolean hasOpenCase) {
		this.hasOpenCase = hasOpenCase;
	}

	public EligibilityResponse withHasOpenCase(final Boolean hasOpenCase) {
		this.hasOpenCase = hasOpenCase;
		return this;
	}

	public Boolean getMaritalStatusMatches() {
		return maritalStatusMatches;
	}

	public void setMaritalStatusMatches(final Boolean maritalStatusMatches) {
		this.maritalStatusMatches = maritalStatusMatches;
	}

	public EligibilityResponse withMaritalStatusMatches(final Boolean maritalStatusMatches) {
		this.maritalStatusMatches = maritalStatusMatches;
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

	public boolean isPreviousMonthDecided() {
		return previousMonthDecided;
	}

	public void setPreviousMonthDecided(final boolean previousMonthDecided) {
		this.previousMonthDecided = previousMonthDecided;
	}

	public EligibilityResponse withPreviousMonthDecided(final boolean previousMonthDecided) {
		this.previousMonthDecided = previousMonthDecided;
		return this;
	}

	public boolean isMonthBeforePreviousDecided() {
		return monthBeforePreviousDecided;
	}

	public void setMonthBeforePreviousDecided(final boolean monthBeforePreviousDecided) {
		this.monthBeforePreviousDecided = monthBeforePreviousDecided;
	}

	public EligibilityResponse withMonthBeforePreviousDecided(final boolean monthBeforePreviousDecided) {
		this.monthBeforePreviousDecided = monthBeforePreviousDecided;
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

	public String getReopenableErrandId() {
		return reopenableErrandId;
	}

	public void setReopenableErrandId(final String reopenableErrandId) {
		this.reopenableErrandId = reopenableErrandId;
	}

	public EligibilityResponse withReopenableErrandId(final String reopenableErrandId) {
		this.reopenableErrandId = reopenableErrandId;
		return this;
	}

	public OffsetDateTime getClosedAt() {
		return closedAt;
	}

	public void setClosedAt(final OffsetDateTime closedAt) {
		this.closedAt = closedAt;
	}

	public EligibilityResponse withClosedAt(final OffsetDateTime closedAt) {
		this.closedAt = closedAt;
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
			&& previousMonthDecided == that.previousMonthDecided
			&& monthBeforePreviousDecided == that.monthBeforePreviousDecided
			&& hasPreviousCalculation == that.hasPreviousCalculation && lifecareChecked == that.lifecareChecked
			&& hasCoApplicant == that.hasCoApplicant && Objects.equals(suggestions, that.suggestions)
			&& Objects.equals(reasonCode, that.reasonCode) && Objects.equals(message, that.message)
			&& Objects.equals(introText, that.introText) && Objects.equals(hasOpenCase, that.hasOpenCase)
			&& Objects.equals(maritalStatusMatches, that.maritalStatusMatches)
			&& Objects.equals(latestDecisionPeriodMonth, that.latestDecisionPeriodMonth)
			&& Objects.equals(latestDecisionPeriodYear, that.latestDecisionPeriodYear)
			&& Objects.equals(reopenableErrandId, that.reopenableErrandId) && Objects.equals(closedAt, that.closedAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(suggestions, reasonCode, message, introText, existsInCm, existsInLc, hasOpenCase,
			maritalStatusMatches, windowDays, applicationExistsThisMonth, applicationExistsNextMonth, currentMonthDecided,
			previousMonthDecided, monthBeforePreviousDecided, latestDecisionPeriodMonth, latestDecisionPeriodYear,
			hasPreviousCalculation, lifecareChecked, hasCoApplicant, reopenableErrandId, closedAt);
	}

	@Override
	public String toString() {
		return "EligibilityResponse{suggestions=" + suggestions + ", reasonCode='" + reasonCode + "', message='" + message
			+ "', introText='" + introText + "', existsInCm=" + existsInCm + ", existsInLc=" + existsInLc
			+ ", hasOpenCase=" + hasOpenCase + ", maritalStatusMatches=" + maritalStatusMatches
			+ ", windowDays=" + windowDays + ", applicationExistsThisMonth=" + applicationExistsThisMonth
			+ ", applicationExistsNextMonth=" + applicationExistsNextMonth + ", currentMonthDecided=" + currentMonthDecided
			+ ", previousMonthDecided=" + previousMonthDecided + ", monthBeforePreviousDecided=" + monthBeforePreviousDecided
			+ ", latestDecisionPeriodMonth=" + latestDecisionPeriodMonth + ", latestDecisionPeriodYear="
			+ latestDecisionPeriodYear + ", hasPreviousCalculation=" + hasPreviousCalculation + ", lifecareChecked="
			+ lifecareChecked + ", hasCoApplicant=" + hasCoApplicant + ", reopenableErrandId='" + reopenableErrandId
			+ "', closedAt=" + closedAt + '}';
	}
}
