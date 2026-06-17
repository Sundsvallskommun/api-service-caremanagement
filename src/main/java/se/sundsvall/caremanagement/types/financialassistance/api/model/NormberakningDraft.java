package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The draft normberäkning for an errand — the FC income rows the EB process prepared (no Lifecare write yet) and that a
 * handläggare may edit in Draken before deciding. {@code edited} marks that a handläggare has touched it, after which
 * the daily refresh preserves the rows and surfaces newly-arrived SSBTEK income as a {@code NEW_INCOME} warning.
 */
@Schema(description = "The (editable) draft normberäkning — FC income rows, not yet created in Lifecare.")
public class NormberakningDraft {

	@Schema(description = "The errand id", accessMode = Schema.AccessMode.READ_ONLY)
	private String errandId;

	@Schema(description = "The application month (ISO yyyy-MM)", examples = "2026-06")
	private String applicationMonth;

	@Schema(description = "Whether a handläggare has edited the draft (the daily refresh then preserves the rows)", examples = "false")
	private boolean edited;

	@Schema(description = "The income rows")
	private List<DraftIncomeRow> rows = new ArrayList<>();

	@Schema(description = "When the draft was created", accessMode = Schema.AccessMode.READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "When the draft was last updated", accessMode = Schema.AccessMode.READ_ONLY)
	private OffsetDateTime updated;

	public static NormberakningDraft create() {
		return new NormberakningDraft();
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public NormberakningDraft withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public NormberakningDraft withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	public boolean isEdited() {
		return edited;
	}

	public void setEdited(final boolean edited) {
		this.edited = edited;
	}

	public NormberakningDraft withEdited(final boolean edited) {
		this.edited = edited;
		return this;
	}

	public List<DraftIncomeRow> getRows() {
		return rows;
	}

	public void setRows(final List<DraftIncomeRow> rows) {
		this.rows = rows;
	}

	public NormberakningDraft withRows(final List<DraftIncomeRow> rows) {
		this.rows = rows;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NormberakningDraft withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public NormberakningDraft withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormberakningDraft that = (NormberakningDraft) o;
		return edited == that.edited && Objects.equals(errandId, that.errandId) && Objects.equals(applicationMonth, that.applicationMonth)
			&& Objects.equals(rows, that.rows) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, applicationMonth, edited, rows, created, updated);
	}

	@Override
	public String toString() {
		return "NormberakningDraft{" +
			"errandId='" + errandId + '\'' +
			", applicationMonth='" + applicationMonth + '\'' +
			", edited=" + edited +
			", rows=" + rows +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
