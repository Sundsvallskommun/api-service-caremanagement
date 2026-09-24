package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A single financial assistance payment (utbetalning) on an errand, from the time careM held payment drafts and
 * "Besluta och utbetala" created a row per decided payment. <strong>Retired and read-only:</strong> nothing writes
 * these
 * rows any more — Draken registers payments directly in Lifecare. They are kept, data included, because payment-status
 * still verifies errands decided before the change against them, and as audit history. Modelled after
 * {@link FaMonitoringEntity}.
 *
 * <p>
 * {@code source} records provenance — {@code CASEWORKER} for one authored in Draken, {@code LIFECARE} for one read out
 * of Lifecare and posted onto the errand. {@code lifecareId} is the payment's id in Lifecare once it exists there —
 * null for a caseworker row not yet registered, the idempotency key LIFECARE rows are upserted on.
 * </p>
 *
 * <p>
 * {@code status} was server-managed: a caseworker's saved row was {@code DRAFT}, a row a decision created
 * {@code PENDING_REGISTRATION}, and the BFF's Lifecare report moved it on to {@code REGISTERED} or to {@code FAILED}
 * with Lifecare's own message in {@code lifecareDetail}. A row still {@code PENDING_REGISTRATION} stays so.
 * </p>
 *
 * <p>
 * Unlike {@link FaMonitoringEntity}, several payments per (errand, applicationMonth) are allowed — there is
 * deliberately no unique constraint on that pair, only on {@code (errand_id, lifecare_id)}.
 * </p>
 */
@Entity
@Table(name = "errand_financial_assistance_payment", indexes = {
	// Unique: lifecareId is the idempotency key LIFECARE rows are upserted on — a duplicate pair would break the lookup.
	@Index(name = "uq_fa_payment_errand_id_lifecare_id", columnList = "errand_id, lifecare_id", unique = true)
})
public class FaPaymentEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "source", length = 16)
	private String source;

	@Column(name = "lifecare_id", length = 64)
	private String lifecareId;

	/** Lifecare's own message when the lifecare-result report said FAILED — shown to the caseworker as-is. */
	@Column(name = "lifecare_detail", length = 1024)
	private String lifecareDetail;

	@Column(name = "status", length = 32)
	private String status;

	@Column(name = "money_type", length = 64)
	private String moneyType;

	@Column(name = "payment_date")
	private LocalDate paymentDate;

	@Column(name = "amount", precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(name = "application_month", length = 7)
	private String applicationMonth;

	@Column(name = "accounting_code", length = 64)
	private String accountingCode;

	@ElementCollection
	@CollectionTable(name = "errand_financial_assistance_payment_stakeholder", joinColumns = @JoinColumn(name = "payment_id"))
	@Column(name = "stakeholder_id", length = 64)
	private List<String> reportedOnStakeholderIds;

	@Column(name = "accounting_date")
	private LocalDate accountingDate;

	@Column(name = "excluded_from_payment")
	private boolean excludedFromPayment;

	@Column(name = "payee_stakeholder_id", length = 64)
	private String payeeStakeholderId;

	/**
	 * The {@link FaPayeeEntity} row this payment pays to, when the payee came from the errand's payee list. Null for a
	 * payee derived from the Lifecare payment history (those have no local row) and for a manual payee the caseworker
	 * deleted afterwards — the copied name/method/clearing/account fields are owned by the decision and stay either
	 * way. It exists so Draken's BFF can pick the payee's Lifecare id when it registers the payment, instead of
	 * matching on name and account number.
	 */
	@Column(name = "payee_id", length = 36)
	private String payeeId;

	/**
	 * The payee's id in Lifecare, when the caseworker picked a payee Lifecare already has. Draken reads the payees
	 * straight from Lifecare, so such a payee has no {@link FaPayeeEntity} row to carry the id — it is stored here
	 * instead. Null when the payee came from a local row, whose own lifecarePayeeId is then used.
	 */
	@Column(name = "lifecare_payee_id", length = 64)
	private String lifecarePayeeId;

	@Column(name = "payment_method", length = 64)
	private String paymentMethod;

	@Column(name = "payee_name", length = 255)
	private String payeeName;

	@Column(name = "payee_address", length = 255)
	private String payeeAddress;

	@Column(name = "payee_care_of", length = 255)
	private String payeeCareOf;

	@Column(name = "payee_zip_code", length = 16)
	private String payeeZipCode;

	@Column(name = "payee_city", length = 255)
	private String payeeCity;

	@Column(name = "clearing_number", length = 64)
	private String clearingNumber;

	@Column(name = "account_number", length = 64)
	private String accountNumber;

	@Column(name = "local_payment_number", length = 64)
	private String localPaymentNumber;

	@Column(name = "invoice_number", length = 64)
	private String invoiceNumber;

	@Column(name = "uses_ocr")
	private boolean usesOcr;

	@ElementCollection
	@CollectionTable(name = "errand_financial_assistance_payment_message_line", joinColumns = @JoinColumn(name = "payment_id"))
	@Column(name = "message_line", length = 255)
	private List<String> messageLines;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static FaPaymentEntity create() {
		return new FaPaymentEntity();
	}

	@PrePersist
	void prePersist() {
		final var now = OffsetDateTime.now(ZoneId.systemDefault());
		created = now;
		modified = now;
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

	public FaPaymentEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaPaymentEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public FaPaymentEntity withSource(final String source) {
		this.source = source;
		return this;
	}

	public String getLifecareId() {
		return lifecareId;
	}

	public void setLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
	}

	public FaPaymentEntity withLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
		return this;
	}

	public String getLifecareDetail() {
		return lifecareDetail;
	}

	public void setLifecareDetail(final String lifecareDetail) {
		this.lifecareDetail = lifecareDetail;
	}

	public FaPaymentEntity withLifecareDetail(final String lifecareDetail) {
		this.lifecareDetail = lifecareDetail;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public FaPaymentEntity withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getMoneyType() {
		return moneyType;
	}

	public void setMoneyType(final String moneyType) {
		this.moneyType = moneyType;
	}

	public FaPaymentEntity withMoneyType(final String moneyType) {
		this.moneyType = moneyType;
		return this;
	}

	public LocalDate getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(final LocalDate paymentDate) {
		this.paymentDate = paymentDate;
	}

	public FaPaymentEntity withPaymentDate(final LocalDate paymentDate) {
		this.paymentDate = paymentDate;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public FaPaymentEntity withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public FaPaymentEntity withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	public String getAccountingCode() {
		return accountingCode;
	}

	public void setAccountingCode(final String accountingCode) {
		this.accountingCode = accountingCode;
	}

	public FaPaymentEntity withAccountingCode(final String accountingCode) {
		this.accountingCode = accountingCode;
		return this;
	}

	public List<String> getReportedOnStakeholderIds() {
		return reportedOnStakeholderIds;
	}

	public void setReportedOnStakeholderIds(final List<String> reportedOnStakeholderIds) {
		this.reportedOnStakeholderIds = reportedOnStakeholderIds;
	}

	public FaPaymentEntity withReportedOnStakeholderIds(final List<String> reportedOnStakeholderIds) {
		this.reportedOnStakeholderIds = reportedOnStakeholderIds;
		return this;
	}

	public LocalDate getAccountingDate() {
		return accountingDate;
	}

	public void setAccountingDate(final LocalDate accountingDate) {
		this.accountingDate = accountingDate;
	}

	public FaPaymentEntity withAccountingDate(final LocalDate accountingDate) {
		this.accountingDate = accountingDate;
		return this;
	}

	public boolean isExcludedFromPayment() {
		return excludedFromPayment;
	}

	public void setExcludedFromPayment(final boolean excludedFromPayment) {
		this.excludedFromPayment = excludedFromPayment;
	}

	public FaPaymentEntity withExcludedFromPayment(final boolean excludedFromPayment) {
		this.excludedFromPayment = excludedFromPayment;
		return this;
	}

	public String getPayeeStakeholderId() {
		return payeeStakeholderId;
	}

	public void setPayeeStakeholderId(final String payeeStakeholderId) {
		this.payeeStakeholderId = payeeStakeholderId;
	}

	public FaPaymentEntity withPayeeStakeholderId(final String payeeStakeholderId) {
		this.payeeStakeholderId = payeeStakeholderId;
		return this;
	}

	public String getPayeeId() {
		return payeeId;
	}

	public void setPayeeId(final String payeeId) {
		this.payeeId = payeeId;
	}

	public FaPaymentEntity withPayeeId(final String payeeId) {
		this.payeeId = payeeId;
		return this;
	}

	public String getLifecarePayeeId() {
		return lifecarePayeeId;
	}

	public void setLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
	}

	public FaPaymentEntity withLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
		return this;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public FaPaymentEntity withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public String getPayeeName() {
		return payeeName;
	}

	public void setPayeeName(final String payeeName) {
		this.payeeName = payeeName;
	}

	public FaPaymentEntity withPayeeName(final String payeeName) {
		this.payeeName = payeeName;
		return this;
	}

	public String getPayeeAddress() {
		return payeeAddress;
	}

	public void setPayeeAddress(final String payeeAddress) {
		this.payeeAddress = payeeAddress;
	}

	public FaPaymentEntity withPayeeAddress(final String payeeAddress) {
		this.payeeAddress = payeeAddress;
		return this;
	}

	public String getPayeeCareOf() {
		return payeeCareOf;
	}

	public void setPayeeCareOf(final String payeeCareOf) {
		this.payeeCareOf = payeeCareOf;
	}

	public FaPaymentEntity withPayeeCareOf(final String payeeCareOf) {
		this.payeeCareOf = payeeCareOf;
		return this;
	}

	public String getPayeeZipCode() {
		return payeeZipCode;
	}

	public void setPayeeZipCode(final String payeeZipCode) {
		this.payeeZipCode = payeeZipCode;
	}

	public FaPaymentEntity withPayeeZipCode(final String payeeZipCode) {
		this.payeeZipCode = payeeZipCode;
		return this;
	}

	public String getPayeeCity() {
		return payeeCity;
	}

	public void setPayeeCity(final String payeeCity) {
		this.payeeCity = payeeCity;
	}

	public FaPaymentEntity withPayeeCity(final String payeeCity) {
		this.payeeCity = payeeCity;
		return this;
	}

	public String getClearingNumber() {
		return clearingNumber;
	}

	public void setClearingNumber(final String clearingNumber) {
		this.clearingNumber = clearingNumber;
	}

	public FaPaymentEntity withClearingNumber(final String clearingNumber) {
		this.clearingNumber = clearingNumber;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public FaPaymentEntity withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public String getLocalPaymentNumber() {
		return localPaymentNumber;
	}

	public void setLocalPaymentNumber(final String localPaymentNumber) {
		this.localPaymentNumber = localPaymentNumber;
	}

	public FaPaymentEntity withLocalPaymentNumber(final String localPaymentNumber) {
		this.localPaymentNumber = localPaymentNumber;
		return this;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(final String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public FaPaymentEntity withInvoiceNumber(final String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
		return this;
	}

	public boolean isUsesOcr() {
		return usesOcr;
	}

	public void setUsesOcr(final boolean usesOcr) {
		this.usesOcr = usesOcr;
	}

	public FaPaymentEntity withUsesOcr(final boolean usesOcr) {
		this.usesOcr = usesOcr;
		return this;
	}

	public List<String> getMessageLines() {
		return messageLines;
	}

	public void setMessageLines(final List<String> messageLines) {
		this.messageLines = messageLines;
	}

	public FaPaymentEntity withMessageLines(final List<String> messageLines) {
		this.messageLines = messageLines;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaPaymentEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public FaPaymentEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	// 'reportedOnStakeholderIds' and 'messageLines' (collection tables) are deliberately excluded from
	// equals/hashCode/toString — they are not part of the entity's identity and keep the comparison cheap.
	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaPaymentEntity that = (FaPaymentEntity) o;
		return excludedFromPayment == that.excludedFromPayment && usesOcr == that.usesOcr && Objects.equals(id, that.id)
			&& Objects.equals(errandId, that.errandId) && Objects.equals(source, that.source) && Objects.equals(lifecareId, that.lifecareId)
			&& Objects.equals(lifecareDetail, that.lifecareDetail)
			&& Objects.equals(status, that.status) && Objects.equals(moneyType, that.moneyType) && Objects.equals(paymentDate, that.paymentDate)
			&& Objects.equals(amount, that.amount) && Objects.equals(applicationMonth, that.applicationMonth) && Objects.equals(accountingCode, that.accountingCode)
			&& Objects.equals(accountingDate, that.accountingDate) && Objects.equals(payeeStakeholderId, that.payeeStakeholderId)
			&& Objects.equals(payeeId, that.payeeId) && Objects.equals(lifecarePayeeId, that.lifecarePayeeId)
			&& Objects.equals(paymentMethod, that.paymentMethod) && Objects.equals(payeeName, that.payeeName)
			&& Objects.equals(payeeAddress, that.payeeAddress) && Objects.equals(payeeCareOf, that.payeeCareOf)
			&& Objects.equals(payeeZipCode, that.payeeZipCode) && Objects.equals(payeeCity, that.payeeCity)
			&& Objects.equals(clearingNumber, that.clearingNumber) && Objects.equals(accountNumber, that.accountNumber)
			&& Objects.equals(localPaymentNumber, that.localPaymentNumber) && Objects.equals(invoiceNumber, that.invoiceNumber)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, source, lifecareId, lifecareDetail, status, moneyType, paymentDate, amount, applicationMonth, accountingCode, accountingDate,
			excludedFromPayment, payeeStakeholderId, payeeId, lifecarePayeeId, paymentMethod, payeeName, payeeAddress, payeeCareOf, payeeZipCode, payeeCity,
			clearingNumber, accountNumber, localPaymentNumber, invoiceNumber, usesOcr, created, modified);
	}

	@Override
	public String toString() {
		return "FaPaymentEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", source='" + source + '\'' +
			", lifecareId='" + lifecareId + '\'' +
			", lifecareDetail='" + lifecareDetail + '\'' +
			", status='" + status + '\'' +
			", moneyType='" + moneyType + '\'' +
			", paymentDate=" + paymentDate +
			", amount=" + amount +
			", applicationMonth='" + applicationMonth + '\'' +
			", accountingCode='" + accountingCode + '\'' +
			", accountingDate=" + accountingDate +
			", excludedFromPayment=" + excludedFromPayment +
			", payeeStakeholderId='" + payeeStakeholderId + '\'' +
			", payeeId='" + payeeId + '\'' +
			", lifecarePayeeId='" + lifecarePayeeId + '\'' +
			", paymentMethod='" + paymentMethod + '\'' +
			", payeeName='" + payeeName + '\'' +
			", payeeAddress='" + payeeAddress + '\'' +
			", payeeCareOf='" + payeeCareOf + '\'' +
			", payeeZipCode='" + payeeZipCode + '\'' +
			", payeeCity='" + payeeCity + '\'' +
			", clearingNumber='" + clearingNumber + '\'' +
			", accountNumber='" + accountNumber + '\'' +
			", localPaymentNumber='" + localPaymentNumber + '\'' +
			", invoiceNumber='" + invoiceNumber + '\'' +
			", usesOcr=" + usesOcr +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
