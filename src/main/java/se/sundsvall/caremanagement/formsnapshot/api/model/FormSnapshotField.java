package se.sundsvall.caremanagement.formsnapshot.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * A single field as it was rendered to the applicant: its label, any help / info / notice text shown alongside it, the
 * full set of options as presented, and the answer given. For a {@code REPEATING_GROUP} (e.g. incomes, children) the
 * repeated instances are carried in {@code items}, each a list of nested fields.
 */
@Schema(description = "A single form field as it was rendered and answered.")
public class FormSnapshotField {

	@Schema(description = "The field name (matches the typed application data field)", examples = "maritalStatus")
	private String name;

	@Schema(description = "The field label the applicant saw", examples = "Civilstånd")
	private String label;

	@Schema(description = "The input kind as rendered", examples = "RADIO", allowableValues = {
		"RADIO", "CHECKBOX", "SELECT", "TEXT", "TEXTAREA", "NUMBER", "DATE", "BOOLEAN_TOGGLE", "REPEATING_GROUP", "STATIC"
	})
	private String inputType;

	@Schema(description = "The help text shown for the field, if any", examples = "Ange ditt civilstånd vid ansökningstillfället.")
	private String helpText;

	@Schema(description = "Info texts shown for the field, in order")
	private List<String> infoTexts;

	@ArraySchema(arraySchema = @Schema(description = "Info / warning / error notices rendered for the field, in order"), schema = @Schema(implementation = FormSnapshotNotice.class))
	private List<FormSnapshotNotice> notices;

	@ArraySchema(arraySchema = @Schema(description = "All options as presented (for RADIO/CHECKBOX/SELECT), in order"), schema = @Schema(implementation = FormSnapshotOption.class))
	private List<FormSnapshotOption> options;

	@Schema(description = "The answer given, when the field has a single answer")
	private FormSnapshotAnswer answer;

	// TODO (future, breaking change): replace the anonymous List<List<FormSnapshotField>> with a named
	// List<FormSnapshotGroup> wrapper — easier to consume and room for per-group metadata. Deferred because it changes
	// the citizen-authored / Draken wire contract (PR #12 review).
	@Schema(description = "For REPEATING_GROUP fields, one entry per repeated instance; each is the list of nested fields")
	private List<List<FormSnapshotField>> items;

	@Schema(description = "Whether the field was required as rendered", examples = "true")
	private boolean required;

	@Schema(description = "Whether the field was visible to the applicant", examples = "true")
	private boolean visible;

	@Schema(description = "The visibility rule that was active, human-readable", examples = "always")
	private String condition;

	public static FormSnapshotField create() {
		return new FormSnapshotField();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public FormSnapshotField withName(final String name) {
		this.name = name;
		return this;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public FormSnapshotField withLabel(final String label) {
		this.label = label;
		return this;
	}

	public String getInputType() {
		return inputType;
	}

	public void setInputType(final String inputType) {
		this.inputType = inputType;
	}

	public FormSnapshotField withInputType(final String inputType) {
		this.inputType = inputType;
		return this;
	}

	public String getHelpText() {
		return helpText;
	}

	public void setHelpText(final String helpText) {
		this.helpText = helpText;
	}

	public FormSnapshotField withHelpText(final String helpText) {
		this.helpText = helpText;
		return this;
	}

	public List<String> getInfoTexts() {
		return infoTexts;
	}

	public void setInfoTexts(final List<String> infoTexts) {
		this.infoTexts = infoTexts;
	}

	public FormSnapshotField withInfoTexts(final List<String> infoTexts) {
		this.infoTexts = infoTexts;
		return this;
	}

	public List<FormSnapshotNotice> getNotices() {
		return notices;
	}

	public void setNotices(final List<FormSnapshotNotice> notices) {
		this.notices = notices;
	}

	public FormSnapshotField withNotices(final List<FormSnapshotNotice> notices) {
		this.notices = notices;
		return this;
	}

	public List<FormSnapshotOption> getOptions() {
		return options;
	}

	public void setOptions(final List<FormSnapshotOption> options) {
		this.options = options;
	}

	public FormSnapshotField withOptions(final List<FormSnapshotOption> options) {
		this.options = options;
		return this;
	}

	public FormSnapshotAnswer getAnswer() {
		return answer;
	}

	public void setAnswer(final FormSnapshotAnswer answer) {
		this.answer = answer;
	}

	public FormSnapshotField withAnswer(final FormSnapshotAnswer answer) {
		this.answer = answer;
		return this;
	}

	public List<List<FormSnapshotField>> getItems() {
		return items;
	}

	public void setItems(final List<List<FormSnapshotField>> items) {
		this.items = items;
	}

	public FormSnapshotField withItems(final List<List<FormSnapshotField>> items) {
		this.items = items;
		return this;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(final boolean required) {
		this.required = required;
	}

	public FormSnapshotField withRequired(final boolean required) {
		this.required = required;
		return this;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(final boolean visible) {
		this.visible = visible;
	}

	public FormSnapshotField withVisible(final boolean visible) {
		this.visible = visible;
		return this;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(final String condition) {
		this.condition = condition;
	}

	public FormSnapshotField withCondition(final String condition) {
		this.condition = condition;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FormSnapshotField that = (FormSnapshotField) o;
		return required == that.required && visible == that.visible && Objects.equals(name, that.name)
			&& Objects.equals(label, that.label) && Objects.equals(inputType, that.inputType) && Objects.equals(helpText, that.helpText)
			&& Objects.equals(infoTexts, that.infoTexts) && Objects.equals(notices, that.notices) && Objects.equals(options, that.options)
			&& Objects.equals(answer, that.answer) && Objects.equals(items, that.items) && Objects.equals(condition, that.condition);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, label, inputType, helpText, infoTexts, notices, options, answer, items, required, visible, condition);
	}

	@Override
	public String toString() {
		return "FormSnapshotField{name='" + name + "', label='" + label + "', inputType='" + inputType + "', helpText='" + helpText
			+ "', infoTexts=" + infoTexts + ", notices=" + notices + ", options=" + options + ", answer=" + answer + ", items=" + items
			+ ", required=" + required + ", visible=" + visible + ", condition='" + condition + "'}";
	}
}
