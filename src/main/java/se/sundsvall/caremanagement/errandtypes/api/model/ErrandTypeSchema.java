package se.sundsvall.caremanagement.errandtypes.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;

/**
 * The form descriptor for one errand type slug: identity, the optional application-type variant, the allowed statuses,
 * the stakeholder roles and the per-type field specification. Lets a frontend discover what a given model (e.g. a
 * financial-assistance renewal) looks like without hard-coding it.
 */
@Schema(description = "Form descriptor for an errand type slug — statuses, roles and the fields its data payload should carry.")
public class ErrandTypeSchema {

	@Schema(description = "The errand type slug", examples = "financial-assistance-renewal")
	private String typeSlug;

	@Schema(description = "The application-type variant the slug maps to, when the type exposes one; null otherwise", examples = "RENEWAL")
	private String applicationType;

	@Schema(description = "Human-readable display name of the type", examples = "Financial assistance – återansökan")
	private String displayName;

	@Schema(description = "Allowed statuses for the type — code plus human-readable display name, in lifecycle order")
	private List<StatusDefinition> statuses;

	@Schema(description = "Stakeholder roles valid for the type")
	private List<RoleDefinition> roles;

	@Schema(description = "The fields the type's data payload should carry, as form guidance")
	private List<FieldDescriptor> fields;

	@Schema(description = "The allowed decision outcomes (decision alternatives) a caseworker may record on the type; empty when the type defines none")
	private List<DecisionOption> decisionOptions;

	public static ErrandTypeSchema create() {
		return new ErrandTypeSchema();
	}

	public String getTypeSlug() {
		return typeSlug;
	}

	public void setTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
	}

	public ErrandTypeSchema withTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
		return this;
	}

	public String getApplicationType() {
		return applicationType;
	}

	public void setApplicationType(final String applicationType) {
		this.applicationType = applicationType;
	}

	public ErrandTypeSchema withApplicationType(final String applicationType) {
		this.applicationType = applicationType;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public ErrandTypeSchema withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public List<StatusDefinition> getStatuses() {
		return statuses;
	}

	public void setStatuses(final List<StatusDefinition> statuses) {
		this.statuses = statuses;
	}

	public ErrandTypeSchema withStatuses(final List<StatusDefinition> statuses) {
		this.statuses = statuses;
		return this;
	}

	public List<RoleDefinition> getRoles() {
		return roles;
	}

	public void setRoles(final List<RoleDefinition> roles) {
		this.roles = roles;
	}

	public ErrandTypeSchema withRoles(final List<RoleDefinition> roles) {
		this.roles = roles;
		return this;
	}

	public List<FieldDescriptor> getFields() {
		return fields;
	}

	public void setFields(final List<FieldDescriptor> fields) {
		this.fields = fields;
	}

	public ErrandTypeSchema withFields(final List<FieldDescriptor> fields) {
		this.fields = fields;
		return this;
	}

	public List<DecisionOption> getDecisionOptions() {
		return decisionOptions;
	}

	public void setDecisionOptions(final List<DecisionOption> decisionOptions) {
		this.decisionOptions = decisionOptions;
	}

	public ErrandTypeSchema withDecisionOptions(final List<DecisionOption> decisionOptions) {
		this.decisionOptions = decisionOptions;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ErrandTypeSchema that = (ErrandTypeSchema) o;
		return Objects.equals(typeSlug, that.typeSlug) && Objects.equals(applicationType, that.applicationType)
			&& Objects.equals(displayName, that.displayName) && Objects.equals(statuses, that.statuses)
			&& Objects.equals(roles, that.roles) && Objects.equals(fields, that.fields)
			&& Objects.equals(decisionOptions, that.decisionOptions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(typeSlug, applicationType, displayName, statuses, roles, fields, decisionOptions);
	}

	@Override
	public String toString() {
		return "ErrandTypeSchema{typeSlug='" + typeSlug + "', applicationType='" + applicationType + "', displayName='"
			+ displayName + "', statuses=" + statuses + ", roles=" + roles + ", fields=" + fields
			+ ", decisionOptions=" + decisionOptions + '}';
	}
}
