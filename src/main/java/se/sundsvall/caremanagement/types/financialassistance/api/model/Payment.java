package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * A payment row an errand carries from the time careM held payment drafts — read internally by payment-status for
 * errands decided before Draken registered payments directly in Lifecare. No longer served by any endpoint.
 * {@code status} is {@code DRAFT} for a saved draft, {@code PENDING_REGISTRATION} for one a decision created that was
 * never reported, and {@code REGISTERED}/{@code FAILED} once Draken's BFF reported it.
 */
@Schema(description = "A financial assistance payment (utbetalning) on an errand.")
public class Payment {

	@Schema(description = "The payment id", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@Schema(description = "Provenance: CASEWORKER for one authored in Draken, LIFECARE for one read out of Lifecare and "
		+ "posted onto the errand.", examples = "CASEWORKER", allowableValues = {
			"CASEWORKER", "LIFECARE"
	})
	private String source;

	@Schema(description = "The payment's id in Lifecare once it exists there — null until a caseworker-authored payment has been "
		+ "registered there; always set for a LIFECARE-sourced one.", examples = "987654")
	private String lifecareId;

	@Schema(description = "Lifecare's own message when the lifecare-result report said FAILED — shown to the caseworker as-is",
		examples = "Betalningsmottagaren saknas i Lifecare",
		accessMode = Schema.AccessMode.READ_ONLY)
	private String lifecareDetail;

	@Schema(description = "Server-managed lifecycle status. DRAFT for a caseworker's saved draft; PENDING_REGISTRATION for one a "
		+ "decision created, waiting to be registered in Lifecare; REGISTERED once Draken's BFF has reported it registered "
		+ "(REGISTERED means it exists there, not that it has been paid out — whether it was effectuated is a separate "
		+ "question, asked through POST .../financial-assistance/payment-status); FAILED when it could not be registered, "
		+ "with Lifecare's reason in lifecareDetail.", examples = "DRAFT", allowableValues = {
			"DRAFT", "PENDING_REGISTRATION", "REGISTERED", "FAILED"
	}, accessMode = Schema.AccessMode.READ_ONLY)
	private String status;

	@Schema(description = "The type of money paid out. Unconstrained — the value set comes from Lifecare and isn't known yet.",
		examples = "FORSORJNINGSSTOD")
	private String moneyType;

	@Schema(description = "The date the payment is/was made", examples = "2026-08-25")
	@DateTimeFormat(iso = DATE)
	private LocalDate paymentDate;

	@Schema(description = "The payment amount", examples = "4500.00")
	private BigDecimal amount;

	@Schema(description = "The application month the payment concerns, yyyy-MM", examples = "2026-08")
	private String applicationMonth;

	@Schema(description = "The accounting code (kontering) the bistånd is booked against. Free text: FamilyCare exposes no catalogue of accounting codes over the API.", examples = "4631-1234")
	private String accountingCode;

	@ArraySchema(arraySchema = @Schema(description = "Stakeholder ids the payment is reported on"), schema = @Schema(implementation = String.class, examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479"))
	private List<String> reportedOnStakeholderIds;

	@Schema(description = "The accounting date for the payment", examples = "2026-08-25")
	@DateTimeFormat(iso = DATE)
	private LocalDate accountingDate;

	@Schema(description = "Whether the payment is excluded from being paid out", examples = "false")
	private boolean excludedFromPayment;

	@Schema(description = "The id of the payee row on the errand this payment pays to, as GET .../payees returns it. Null for a payee "
		+ "derived from the Lifecare payment history (no local row) and for a manual payee deleted after the decision — the "
		+ "copied payee fields below are owned by the decision and stay either way.",
		examples = "a1b2c3d4-0000-0000-0000-000000000001")
	private String payeeId;

	@Schema(description = "The payee's id in Lifecare, read from that payee row — set once the payee's creation in Lifecare has been reported "
		+ "back. Lets Draken's BFF pick the payee in Lifecare by id instead of matching on name and account number. Null "
		+ "when the payee has no local row, or when its creation has not been reported yet (lifecareStatus PENDING or "
		+ "FAILED on that payee).", examples = "44213", accessMode = Schema.AccessMode.READ_ONLY)
	private String lifecarePayeeId;

	@Schema(description = "The stakeholder id of the payee", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
	private String payeeStakeholderId;

	@Schema(description = "How the payment is made. Unconstrained — the value set comes from Lifecare and isn't known yet.",
		examples = "BANK_TRANSFER")
	private String paymentMethod;

	@Schema(description = "The payee's name", examples = "Anna Andersson")
	private String payeeName;

	@Schema(description = "The payee's address", examples = "Storgatan 1")
	private String payeeAddress;

	@Schema(description = "The payee's c/o line", examples = "c/o Bertil Bertilsson")
	private String payeeCareOf;

	@Schema(description = "The payee's zip code", examples = "85230")
	private String payeeZipCode;

	@Schema(description = "The payee's city", examples = "Sundsvall")
	private String payeeCity;

	@Schema(description = "The payee's bank clearing number", examples = "8327-9")
	private String clearingNumber;

	@Schema(description = "The payee's bank account number", examples = "123 456 789-0")
	private String accountNumber;

	@Schema(description = "The local payment number, when applicable", examples = "4711")
	private String localPaymentNumber;

	@Schema(description = "The invoice number, when applicable", examples = "2026-00417")
	private String invoiceNumber;

	@Schema(description = "Whether the payment uses OCR", examples = "false")
	private boolean usesOcr;

	@ArraySchema(arraySchema = @Schema(description = "Free-text message lines printed on the payment"), schema = @Schema(implementation = String.class, examples = "Ekonomiskt bistånd augusti 2026"))
	private List<String> messageLines;

	@Schema(description = "When the payment was created", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "When the payment was last modified", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime modified;

	public static Payment create() {
		return new Payment();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Payment withId(final String id) {
		this.id = id;
		return this;
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public Payment withSource(final String source) {
		this.source = source;
		return this;
	}

	public String getLifecareId() {
		return lifecareId;
	}

	public void setLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
	}

	public Payment withLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
		return this;
	}

	public String getLifecareDetail() {
		return lifecareDetail;
	}

	public void setLifecareDetail(final String lifecareDetail) {
		this.lifecareDetail = lifecareDetail;
	}

	public Payment withLifecareDetail(final String lifecareDetail) {
		this.lifecareDetail = lifecareDetail;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Payment withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getMoneyType() {
		return moneyType;
	}

	public void setMoneyType(final String moneyType) {
		this.moneyType = moneyType;
	}

	public Payment withMoneyType(final String moneyType) {
		this.moneyType = moneyType;
		return this;
	}

	public LocalDate getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(final LocalDate paymentDate) {
		this.paymentDate = paymentDate;
	}

	public Payment withPaymentDate(final LocalDate paymentDate) {
		this.paymentDate = paymentDate;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public Payment withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public Payment withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	public String getAccountingCode() {
		return accountingCode;
	}

	public void setAccountingCode(final String accountingCode) {
		this.accountingCode = accountingCode;
	}

	public Payment withAccountingCode(final String accountingCode) {
		this.accountingCode = accountingCode;
		return this;
	}

	public List<String> getReportedOnStakeholderIds() {
		return reportedOnStakeholderIds;
	}

	public void setReportedOnStakeholderIds(final List<String> reportedOnStakeholderIds) {
		this.reportedOnStakeholderIds = reportedOnStakeholderIds;
	}

	public Payment withReportedOnStakeholderIds(final List<String> reportedOnStakeholderIds) {
		this.reportedOnStakeholderIds = reportedOnStakeholderIds;
		return this;
	}

	public LocalDate getAccountingDate() {
		return accountingDate;
	}

	public void setAccountingDate(final LocalDate accountingDate) {
		this.accountingDate = accountingDate;
	}

	public Payment withAccountingDate(final LocalDate accountingDate) {
		this.accountingDate = accountingDate;
		return this;
	}

	public boolean isExcludedFromPayment() {
		return excludedFromPayment;
	}

	public void setExcludedFromPayment(final boolean excludedFromPayment) {
		this.excludedFromPayment = excludedFromPayment;
	}

	public Payment withExcludedFromPayment(final boolean excludedFromPayment) {
		this.excludedFromPayment = excludedFromPayment;
		return this;
	}

	public String getPayeeId() {
		return payeeId;
	}

	public void setPayeeId(final String payeeId) {
		this.payeeId = payeeId;
	}

	public Payment withPayeeId(final String payeeId) {
		this.payeeId = payeeId;
		return this;
	}

	public String getLifecarePayeeId() {
		return lifecarePayeeId;
	}

	public void setLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
	}

	public Payment withLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
		return this;
	}

	public String getPayeeStakeholderId() {
		return payeeStakeholderId;
	}

	public void setPayeeStakeholderId(final String payeeStakeholderId) {
		this.payeeStakeholderId = payeeStakeholderId;
	}

	public Payment withPayeeStakeholderId(final String payeeStakeholderId) {
		this.payeeStakeholderId = payeeStakeholderId;
		return this;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public Payment withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public String getPayeeName() {
		return payeeName;
	}

	public void setPayeeName(final String payeeName) {
		this.payeeName = payeeName;
	}

	public Payment withPayeeName(final String payeeName) {
		this.payeeName = payeeName;
		return this;
	}

	public String getPayeeAddress() {
		return payeeAddress;
	}

	public void setPayeeAddress(final String payeeAddress) {
		this.payeeAddress = payeeAddress;
	}

	public Payment withPayeeAddress(final String payeeAddress) {
		this.payeeAddress = payeeAddress;
		return this;
	}

	public String getPayeeCareOf() {
		return payeeCareOf;
	}

	public void setPayeeCareOf(final String payeeCareOf) {
		this.payeeCareOf = payeeCareOf;
	}

	public Payment withPayeeCareOf(final String payeeCareOf) {
		this.payeeCareOf = payeeCareOf;
		return this;
	}

	public String getPayeeZipCode() {
		return payeeZipCode;
	}

	public void setPayeeZipCode(final String payeeZipCode) {
		this.payeeZipCode = payeeZipCode;
	}

	public Payment withPayeeZipCode(final String payeeZipCode) {
		this.payeeZipCode = payeeZipCode;
		return this;
	}

	public String getPayeeCity() {
		return payeeCity;
	}

	public void setPayeeCity(final String payeeCity) {
		this.payeeCity = payeeCity;
	}

	public Payment withPayeeCity(final String payeeCity) {
		this.payeeCity = payeeCity;
		return this;
	}

	public String getClearingNumber() {
		return clearingNumber;
	}

	public void setClearingNumber(final String clearingNumber) {
		this.clearingNumber = clearingNumber;
	}

	public Payment withClearingNumber(final String clearingNumber) {
		this.clearingNumber = clearingNumber;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public Payment withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public String getLocalPaymentNumber() {
		return localPaymentNumber;
	}

	public void setLocalPaymentNumber(final String localPaymentNumber) {
		this.localPaymentNumber = localPaymentNumber;
	}

	public Payment withLocalPaymentNumber(final String localPaymentNumber) {
		this.localPaymentNumber = localPaymentNumber;
		return this;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(final String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public Payment withInvoiceNumber(final String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
		return this;
	}

	public boolean isUsesOcr() {
		return usesOcr;
	}

	public void setUsesOcr(final boolean usesOcr) {
		this.usesOcr = usesOcr;
	}

	public Payment withUsesOcr(final boolean usesOcr) {
		this.usesOcr = usesOcr;
		return this;
	}

	public List<String> getMessageLines() {
		return messageLines;
	}

	public void setMessageLines(final List<String> messageLines) {
		this.messageLines = messageLines;
	}

	public Payment withMessageLines(final List<String> messageLines) {
		this.messageLines = messageLines;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Payment withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Payment withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Payment that = (Payment) o;
		return excludedFromPayment == that.excludedFromPayment && usesOcr == that.usesOcr && Objects.equals(id, that.id)
			&& Objects.equals(source, that.source) && Objects.equals(lifecareId, that.lifecareId)
			&& Objects.equals(lifecareDetail, that.lifecareDetail) && Objects.equals(status, that.status)
			&& Objects.equals(moneyType, that.moneyType) && Objects.equals(paymentDate, that.paymentDate) && Objects.equals(amount, that.amount)
			&& Objects.equals(applicationMonth, that.applicationMonth) && Objects.equals(accountingCode, that.accountingCode) && Objects.equals(reportedOnStakeholderIds, that.reportedOnStakeholderIds)
			&& Objects.equals(accountingDate, that.accountingDate) && Objects.equals(payeeStakeholderId, that.payeeStakeholderId)
			&& Objects.equals(payeeId, that.payeeId) && Objects.equals(lifecarePayeeId, that.lifecarePayeeId)
			&& Objects.equals(paymentMethod, that.paymentMethod) && Objects.equals(payeeName, that.payeeName)
			&& Objects.equals(payeeAddress, that.payeeAddress) && Objects.equals(payeeCareOf, that.payeeCareOf)
			&& Objects.equals(payeeZipCode, that.payeeZipCode) && Objects.equals(payeeCity, that.payeeCity)
			&& Objects.equals(clearingNumber, that.clearingNumber) && Objects.equals(accountNumber, that.accountNumber)
			&& Objects.equals(localPaymentNumber, that.localPaymentNumber) && Objects.equals(invoiceNumber, that.invoiceNumber)
			&& Objects.equals(messageLines, that.messageLines) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, source, lifecareId, lifecareDetail, status, moneyType, paymentDate, amount, applicationMonth, accountingCode, reportedOnStakeholderIds,
			accountingDate, excludedFromPayment, payeeId, lifecarePayeeId, payeeStakeholderId, paymentMethod, payeeName, payeeAddress, payeeCareOf, payeeZipCode,
			payeeCity, clearingNumber, accountNumber, localPaymentNumber, invoiceNumber, usesOcr, messageLines, created, modified);
	}

	@Override
	public String toString() {
		return "Payment{" +
			"id='" + id + '\'' +
			", source='" + source + '\'' +
			", lifecareId='" + lifecareId + '\'' +
			", lifecareDetail='" + lifecareDetail + '\'' +
			", status='" + status + '\'' +
			", moneyType='" + moneyType + '\'' +
			", paymentDate=" + paymentDate +
			", amount=" + amount +
			", applicationMonth='" + applicationMonth + '\'' +
			", accountingCode='" + accountingCode + '\'' +
			", reportedOnStakeholderIds=" + reportedOnStakeholderIds +
			", accountingDate=" + accountingDate +
			", excludedFromPayment=" + excludedFromPayment +
			", payeeId='" + payeeId + '\'' +
			", lifecarePayeeId='" + lifecarePayeeId + '\'' +
			", payeeStakeholderId='" + payeeStakeholderId + '\'' +
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
			", messageLines=" + messageLines +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
