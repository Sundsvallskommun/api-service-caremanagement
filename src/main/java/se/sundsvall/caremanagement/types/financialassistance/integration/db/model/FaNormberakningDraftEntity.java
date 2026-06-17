package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * The draft normberäkning for an errand — the computed FC income rows (as a JSON array) the EB process prepares each
 * day
 * without writing to Lifecare, and that a handläggare can edit in Draken before deciding. {@code edited} guards the
 * daily refresh: while {@code false} the rows are overwritten, once {@code true} they are preserved. The errand id is
 * the primary key (one draft per errand).
 */
@Entity
@Table(name = "errand_financial_assistance_normberakning_draft")
public class FaNormberakningDraftEntity {

	@Id
	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "application_month")
	private String applicationMonth;

	@Column(name = "edited")
	private boolean edited;

	@Column(name = "rows_json", length = LONG32)
	private String rowsJson;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "updated")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime updated;

	public static FaNormberakningDraftEntity create() {
		return new FaNormberakningDraftEntity();
	}

	@PrePersist
	void prePersist() {
		final var now = OffsetDateTime.now();
		created = now;
		updated = now;
	}

	@PreUpdate
	void preUpdate() {
		updated = OffsetDateTime.now();
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaNormberakningDraftEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public FaNormberakningDraftEntity withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	public boolean isEdited() {
		return edited;
	}

	public void setEdited(final boolean edited) {
		this.edited = edited;
	}

	public FaNormberakningDraftEntity withEdited(final boolean edited) {
		this.edited = edited;
		return this;
	}

	public String getRowsJson() {
		return rowsJson;
	}

	public void setRowsJson(final String rowsJson) {
		this.rowsJson = rowsJson;
	}

	public FaNormberakningDraftEntity withRowsJson(final String rowsJson) {
		this.rowsJson = rowsJson;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaNormberakningDraftEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public FaNormberakningDraftEntity withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaNormberakningDraftEntity that = (FaNormberakningDraftEntity) o;
		return edited == that.edited && Objects.equals(errandId, that.errandId) && Objects.equals(applicationMonth, that.applicationMonth)
			&& Objects.equals(rowsJson, that.rowsJson) && Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, applicationMonth, edited, rowsJson, created, updated);
	}

	@Override
	public String toString() {
		return "FaNormberakningDraftEntity{" +
			"errandId='" + errandId + '\'' +
			", applicationMonth='" + applicationMonth + '\'' +
			", edited=" + edited +
			", rowsJson='" + rowsJson + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
