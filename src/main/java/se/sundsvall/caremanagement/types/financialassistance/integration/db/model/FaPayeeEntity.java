package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A betalningsmottagare the caseworker added by hand on an errand, because the applicant's Lifecare payment history
 * did not already contain it.
 *
 * <p>
 * Deliberately errand-scoped rather than person-scoped, and deliberately not a payee register: the row is a bridging
 * state. The caseworker needs the payee in the dropdown now; the {@code ADD_PAYEE} robot writes it into Lifecare; from
 * the next återansökan onwards it arrives through the ordinary 12-month payment history like any other payee. Keying it
 * to the person would turn it into a second, competing register that nothing reconciles.
 * </p>
 *
 * <p>
 * {@code lifecareStatus} tracks that hand-over — {@code PENDING} until the robot reports back, then {@code SYNCED} or
 * {@code FAILED} with Lifecare's own message in {@code lifecareDetail}. A payment must not be registered against a
 * payee that is not in Lifecare, so the status is what finalize warns on.
 * </p>
 */
@Entity
@Table(name = "errand_financial_assistance_payee", indexes = {
	@Index(name = "idx_fa_payee_errand_id", columnList = "errand_id")
})
public class FaPayeeEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "name", length = 255)
	private String name;

	@Column(name = "payment_method", length = 64)
	private String paymentMethod;

	@Column(name = "clearing", length = 16)
	private String clearing;

	@Column(name = "account_number", length = 64)
	private String accountNumber;

	@Column(name = "lifecare_status", length = 16)
	private String lifecareStatus;

	@Column(name = "lifecare_payee_id", length = 64)
	private String lifecarePayeeId;

	@Column(name = "lifecare_detail", length = 1024)
	private String lifecareDetail;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static FaPayeeEntity create() {
		return new FaPayeeEntity();
	}

	@PrePersist
	void prePersist() {
		created = OffsetDateTime.now(ZoneId.systemDefault());
	}

	@PreUpdate
	void preUpdate() {
		modified = OffsetDateTime.now(ZoneId.systemDefault());
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public FaPayeeEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaPayeeEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public FaPayeeEntity withName(final String name) {
		this.name = name;
		return this;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public FaPayeeEntity withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public String getClearing() {
		return clearing;
	}

	public void setClearing(final String clearing) {
		this.clearing = clearing;
	}

	public FaPayeeEntity withClearing(final String clearing) {
		this.clearing = clearing;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public FaPayeeEntity withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public String getLifecareStatus() {
		return lifecareStatus;
	}

	public void setLifecareStatus(final String lifecareStatus) {
		this.lifecareStatus = lifecareStatus;
	}

	public FaPayeeEntity withLifecareStatus(final String lifecareStatus) {
		this.lifecareStatus = lifecareStatus;
		return this;
	}

	public String getLifecarePayeeId() {
		return lifecarePayeeId;
	}

	public void setLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
	}

	public FaPayeeEntity withLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
		return this;
	}

	public String getLifecareDetail() {
		return lifecareDetail;
	}

	public void setLifecareDetail(final String lifecareDetail) {
		this.lifecareDetail = lifecareDetail;
	}

	public FaPayeeEntity withLifecareDetail(final String lifecareDetail) {
		this.lifecareDetail = lifecareDetail;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaPayeeEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public FaPayeeEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (!(o instanceof final FaPayeeEntity that))
			return false;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(name, that.name)
			&& Objects.equals(paymentMethod, that.paymentMethod) && Objects.equals(clearing, that.clearing)
			&& Objects.equals(accountNumber, that.accountNumber) && Objects.equals(lifecareStatus, that.lifecareStatus)
			&& Objects.equals(lifecarePayeeId, that.lifecarePayeeId) && Objects.equals(lifecareDetail, that.lifecareDetail)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, name, paymentMethod, clearing, accountNumber, lifecareStatus, lifecarePayeeId, lifecareDetail, created, modified);
	}

	@Override
	public String toString() {
		return "FaPayeeEntity{id='" + id + "', errandId='" + errandId + "', name='" + name + "', paymentMethod='" + paymentMethod
			+ "', clearing='" + clearing + "', accountNumber='" + accountNumber + "', lifecareStatus='" + lifecareStatus
			+ "', lifecarePayeeId='" + lifecarePayeeId + "', lifecareDetail='" + lifecareDetail + "', created=" + created
			+ ", modified=" + modified + "}";
	}
}
