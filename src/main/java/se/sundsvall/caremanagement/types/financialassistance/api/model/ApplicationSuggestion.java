package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A single application the citizen can be offered, with the period it concerns. The eligibility check returns one or
 * more of these (e.g. "renewal for next month" or "supplementary application for the current month"); exactly one is
 * flagged
 * {@code recommended} as the primary suggestion.
 */
@Schema(description = "A suggested application the citizen can submit, with its target period.")
public class ApplicationSuggestion {

	@Schema(description = "The errand type slug to create the application against",
		examples = "financial-assistance-renewal",
		allowableValues = {
			"financial-assistance-new", "financial-assistance-renewal", "financial-assistance-supplementary"
		})
	private String typeSlug;

	@Schema(description = "The application type the slug maps to",
		examples = "RENEWAL",
		allowableValues = {
			"NEW", "RENEWAL", "SUPPLEMENTARY"
		})
	private String applicationType;

	@Schema(description = "Month (1-12) the suggested application concerns. Null for a new application, which has no prior period.", examples = "7")
	private Integer periodMonth;

	@Schema(description = "Year the suggested application concerns. Null for a new application.", examples = "2026")
	private Integer periodYear;

	@Schema(description = "True for the primary suggestion the citizen should be guided towards", examples = "true")
	private boolean recommended;

	@Schema(description = "Human-readable Swedish label for the suggestion", examples = "Återansökan för juli 2026")
	private String label;

	@Schema(description = "Swedish explanation of when this application type applies, shown to the citizen next to the label. Null when no wording has been agreed for the type.",
		examples = "du har ansökt tidigare och inte haft ett längre uppehåll")
	private String description;

	public static ApplicationSuggestion create() {
		return new ApplicationSuggestion();
	}

	public String getTypeSlug() {
		return typeSlug;
	}

	public void setTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
	}

	public ApplicationSuggestion withTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
		return this;
	}

	public String getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(final String applicationType) {
		this.applicationType = applicationType;
	}

	public ApplicationSuggestion withApplicationType(final String applicationType) {
		this.applicationType = applicationType;
		return this;
	}

	public Integer getPeriodMonth() {
		return periodMonth;
	}

	public void setPeriodMonth(final Integer periodMonth) {
		this.periodMonth = periodMonth;
	}

	public ApplicationSuggestion withPeriodMonth(final Integer periodMonth) {
		this.periodMonth = periodMonth;
		return this;
	}

	public Integer getPeriodYear() {
		return periodYear;
	}

	public void setPeriodYear(final Integer periodYear) {
		this.periodYear = periodYear;
	}

	public ApplicationSuggestion withPeriodYear(final Integer periodYear) {
		this.periodYear = periodYear;
		return this;
	}

	public boolean isRecommended() {
		return recommended;
	}

	public void setRecommended(final boolean recommended) {
		this.recommended = recommended;
	}

	public ApplicationSuggestion withRecommended(final boolean recommended) {
		this.recommended = recommended;
		return this;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public ApplicationSuggestion withLabel(final String label) {
		this.label = label;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public ApplicationSuggestion withDescription(final String description) {
		this.description = description;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ApplicationSuggestion that = (ApplicationSuggestion) o;
		return recommended == that.recommended && Objects.equals(typeSlug, that.typeSlug)
			&& Objects.equals(applicationType, that.applicationType) && Objects.equals(periodMonth, that.periodMonth)
			&& Objects.equals(periodYear, that.periodYear) && Objects.equals(label, that.label)
			&& Objects.equals(description, that.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeSlug, applicationType, periodMonth, periodYear, recommended, label, description);
	}

	@Override
	public String toString() {
		return "ApplicationSuggestion{typeSlug='" + typeSlug + "', applicationType='" + applicationType + "', periodMonth="
			+ periodMonth + ", periodYear=" + periodYear + ", recommended=" + recommended + ", label='" + label
			+ "', description='" + description + "'}";
	}
}
