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

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * The draft normberäkning header for an errand — one row per errand, holding the application month and the selected
 * norm. The section rows (personer, inkomster, utgifter) live in their own tables ({@code errand_fa_norm_person},
 * {@code errand_fa_norm_income}, {@code errand_fa_norm_expense}), each row owning its process and handläggare values
 * separately. The EB process prepares the draft each day without writing to Lifecare; on a beslut the effective values
 * are posted. The errand id is the primary key.
 */
@Entity
@Table(name = "errand_financial_assistance_normberakning_draft")
public class FaNormberakningDraftEntity {

	@Id
	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "application_month")
	private String applicationMonth;

	@Column(name = "norm_id")
	private Integer normId;

	@Column(name = "norm_type")
	private String normType;

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

	public Integer getNormId() {
		return normId;
	}

	public void setNormId(final Integer normId) {
		this.normId = normId;
	}

	public FaNormberakningDraftEntity withNormId(final Integer normId) {
		this.normId = normId;
		return this;
	}

	public String getNormType() {
		return normType;
	}

	public void setNormType(final String normType) {
		this.normType = normType;
	}

	public FaNormberakningDraftEntity withNormType(final String normType) {
		this.normType = normType;
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
		return Objects.equals(errandId, that.errandId) && Objects.equals(applicationMonth, that.applicationMonth)
			&& Objects.equals(normId, that.normId) && Objects.equals(normType, that.normType)
			&& Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, applicationMonth, normId, normType, created, updated);
	}

	@Override
	public String toString() {
		return "FaNormberakningDraftEntity{" +
			"errandId='" + errandId + '\'' +
			", applicationMonth='" + applicationMonth + '\'' +
			", normId=" + normId +
			", normType='" + normType + '\'' +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
