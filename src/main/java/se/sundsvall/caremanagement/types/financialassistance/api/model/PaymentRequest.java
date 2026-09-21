package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * Request to create or replace a financial assistance payment on an errand. The same body is used for create (POST)
 * and update (PUT). {@code status} is not present here — it is entirely server-managed (always {@code DRAFT} on
 * create; see {@code PaymentService}).
 */
@Schema(description = "Request to create or replace a financial assistance payment on an errand.")
public class PaymentRequest {

	@Schema(description = "Provenance, defaults to CASEWORKER when omitted. RPA POSTs LIFECARE (with lifecareId) to surface a "
		+ "payment read out of Lifecare onto the errand.", examples = "CASEWORKER", allowableValues = {
			"CASEWORKER", "LIFECARE"
	})
	@OneOf(value = {
		"CASEWORKER", "LIFECARE"
	}, nullable = true)
	private String source;

	@Schema(description = "The payment's id in Lifecare. Set by RPA when surfacing a LIFECARE-sourced payment (the idempotency key) "
		+ "or when stamping back the id of a registered caseworker payment.", examples = "987654")
	@Size(max = 64)
	private String lifecareId;

	@Schema(description = "The type of money paid out. Unconstrained — the value set comes from Lifecare and isn't known yet.",
		examples = "FORSORJNINGSSTOD")
	@Size(max = 64)
	private String moneyType;

	@Schema(description = "The date the payment is/was made", examples = "2026-08-25")
	@DateTimeFormat(iso = DATE)
	private LocalDate paymentDate;

	@Schema(description = "The payment amount", examples = "4500.00")
	private BigDecimal amount;

	@Schema(description = "The application month the payment concerns", examples = "2026-08", pattern = "^\\d{4}-(0[1-9]|1[0-2])$")
	@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "must be in the format yyyy-MM")
	private String applicationMonth;

	@Schema(description = "The accounting code (kontering) the bistånd is booked against. Free text: FamilyCare exposes no catalogue of accounting codes over the API.", examples = "4631-1234")
	@Size(max = 64)
	private String accountingCode;

	@ArraySchema(arraySchema = @Schema(description = "Stakeholder ids the payment is reported on"), schema = @Schema(implementation = String.class, examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479"))
	private List<@Size(max = 64) String> reportedOnStakeholderIds;

	@Schema(description = "The accounting date for the payment", examples = "2026-08-25")
	@DateTimeFormat(iso = DATE)
	private LocalDate accountingDate;

	@Schema(description = "Whether the payment is excluded from being paid out", examples = "false")
	private boolean excludedFromPayment;

	@Schema(description = "The stakeholder id of the payee", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
	@Size(max = 64)
	private String payeeStakeholderId;

	@Schema(description = "How the payment is made. Unconstrained — the value set comes from Lifecare and isn't known yet.",
		examples = "BANK_TRANSFER")
	@Size(max = 64)
	private String paymentMethod;

	@Schema(description = "The payee's name", examples = "Anna Andersson")
	@Size(max = 255)
	private String payeeName;

	@Schema(description = "The payee's address", examples = "Storgatan 1")
	@Size(max = 255)
	private String payeeAddress;

	@Schema(description = "The payee's c/o line", examples = "c/o Bertil Bertilsson")
	@Size(max = 255)
	private String payeeCareOf;

	@Schema(description = "The payee's zip code", examples = "85230")
	@Size(max = 16)
	private String payeeZipCode;

	@Schema(description = "The payee's city", examples = "Sundsvall")
	@Size(max = 255)
	private String payeeCity;

	@Schema(description = "The payee's bank clearing number", examples = "8327-9")
	@Size(max = 64)
	private String clearingNumber;

	@Schema(description = "The payee's bank account number", examples = "123 456 789-0")
	@Size(max = 64)
	private String accountNumber;

	@Schema(description = "The local payment number, when applicable", examples = "4711")
	@Size(max = 64)
	private String localPaymentNumber;

	@Schema(description = "The invoice number, when applicable", examples = "2026-00417")
	@Size(max = 64)
	private String invoiceNumber;

	@Schema(description = "Whether the payment uses OCR", examples = "false")
	private boolean usesOcr;

	@ArraySchema(arraySchema = @Schema(description = "Free-text message lines printed on the payment"), schema = @Schema(implementation = String.class, examples = "Ekonomiskt bistånd augusti 2026"))
	private List<@Size(max = 255) String> messageLines;

	public static PaymentRequest create() {
		return new PaymentRequest();
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public PaymentRequest withSource(final String source) {
		this.source = source;
		return this;
	}

	public String getLifecareId() {
		return lifecareId;
	}

	public void setLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
	}

	public PaymentRequest withLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
		return this;
	}

	public String getMoneyType() {
		return moneyType;
	}

	public void setMoneyType(final String moneyType) {
		this.moneyType = moneyType;
	}

	public PaymentRequest withMoneyType(final String moneyType) {
		this.moneyType = moneyType;
		return this;
	}

	public LocalDate getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(final LocalDate paymentDate) {
		this.paymentDate = paymentDate;
	}

	public PaymentRequest withPaymentDate(final LocalDate paymentDate) {
		this.paymentDate = paymentDate;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public PaymentRequest withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public PaymentRequest withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	public String getAccountingCode() {
		return accountingCode;
	}

	public void setAccountingCode(final String accountingCode) {
		this.accountingCode = accountingCode;
	}

	public PaymentRequest withAccountingCode(final String accountingCode) {
		this.accountingCode = accountingCode;
		return this;
	}

	public List<String> getReportedOnStakeholderIds() {
		return reportedOnStakeholderIds;
	}

	public void setReportedOnStakeholderIds(final List<String> reportedOnStakeholderIds) {
		this.reportedOnStakeholderIds = reportedOnStakeholderIds;
	}

	public PaymentRequest withReportedOnStakeholderIds(final List<String> reportedOnStakeholderIds) {
		this.reportedOnStakeholderIds = reportedOnStakeholderIds;
		return this;
	}

	public LocalDate getAccountingDate() {
		return accountingDate;
	}

	public void setAccountingDate(final LocalDate accountingDate) {
		this.accountingDate = accountingDate;
	}

	public PaymentRequest withAccountingDate(final LocalDate accountingDate) {
		this.accountingDate = accountingDate;
		return this;
	}

	public boolean isExcludedFromPayment() {
		return excludedFromPayment;
	}

	public void setExcludedFromPayment(final boolean excludedFromPayment) {
		this.excludedFromPayment = excludedFromPayment;
	}

	public PaymentRequest withExcludedFromPayment(final boolean excludedFromPayment) {
		this.excludedFromPayment = excludedFromPayment;
		return this;
	}

	public String getPayeeStakeholderId() {
		return payeeStakeholderId;
	}

	public void setPayeeStakeholderId(final String payeeStakeholderId) {
		this.payeeStakeholderId = payeeStakeholderId;
	}

	public PaymentRequest withPayeeStakeholderId(final String payeeStakeholderId) {
		this.payeeStakeholderId = payeeStakeholderId;
		return this;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public PaymentRequest withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public String getPayeeName() {
		return payeeName;
	}

	public void setPayeeName(final String payeeName) {
		this.payeeName = payeeName;
	}

	public PaymentRequest withPayeeName(final String payeeName) {
		this.payeeName = payeeName;
		return this;
	}

	public String getPayeeAddress() {
		return payeeAddress;
	}

	public void setPayeeAddress(final String payeeAddress) {
		this.payeeAddress = payeeAddress;
	}

	public PaymentRequest withPayeeAddress(final String payeeAddress) {
		this.payeeAddress = payeeAddress;
		return this;
	}

	public String getPayeeCareOf() {
		return payeeCareOf;
	}

	public void setPayeeCareOf(final String payeeCareOf) {
		this.payeeCareOf = payeeCareOf;
	}

	public PaymentRequest withPayeeCareOf(final String payeeCareOf) {
		this.payeeCareOf = payeeCareOf;
		return this;
	}

	public String getPayeeZipCode() {
		return payeeZipCode;
	}

	public void setPayeeZipCode(final String payeeZipCode) {
		this.payeeZipCode = payeeZipCode;
	}

	public PaymentRequest withPayeeZipCode(final String payeeZipCode) {
		this.payeeZipCode = payeeZipCode;
		return this;
	}

	public String getPayeeCity() {
		return payeeCity;
	}

	public void setPayeeCity(final String payeeCity) {
		this.payeeCity = payeeCity;
	}

	public PaymentRequest withPayeeCity(final String payeeCity) {
		this.payeeCity = payeeCity;
		return this;
	}

	public String getClearingNumber() {
		return clearingNumber;
	}

	public void setClearingNumber(final String clearingNumber) {
		this.clearingNumber = clearingNumber;
	}

	public PaymentRequest withClearingNumber(final String clearingNumber) {
		this.clearingNumber = clearingNumber;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public PaymentRequest withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public String getLocalPaymentNumber() {
		return localPaymentNumber;
	}

	public void setLocalPaymentNumber(final String localPaymentNumber) {
		this.localPaymentNumber = localPaymentNumber;
	}

	public PaymentRequest withLocalPaymentNumber(final String localPaymentNumber) {
		this.localPaymentNumber = localPaymentNumber;
		return this;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public void setInvoiceNumber(final String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	public PaymentRequest withInvoiceNumber(final String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
		return this;
	}

	public boolean isUsesOcr() {
		return usesOcr;
	}

	public void setUsesOcr(final boolean usesOcr) {
		this.usesOcr = usesOcr;
	}

	public PaymentRequest withUsesOcr(final boolean usesOcr) {
		this.usesOcr = usesOcr;
		return this;
	}

	public List<String> getMessageLines() {
		return messageLines;
	}

	public void setMessageLines(final List<String> messageLines) {
		this.messageLines = messageLines;
	}

	public PaymentRequest withMessageLines(final List<String> messageLines) {
		this.messageLines = messageLines;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PaymentRequest that = (PaymentRequest) o;
		return excludedFromPayment == that.excludedFromPayment && usesOcr == that.usesOcr && Objects.equals(source, that.source)
			&& Objects.equals(lifecareId, that.lifecareId) && Objects.equals(moneyType, that.moneyType)
			&& Objects.equals(paymentDate, that.paymentDate) && Objects.equals(amount, that.amount)
			&& Objects.equals(applicationMonth, that.applicationMonth) && Objects.equals(accountingCode, that.accountingCode) && Objects.equals(reportedOnStakeholderIds, that.reportedOnStakeholderIds)
			&& Objects.equals(accountingDate, that.accountingDate) && Objects.equals(payeeStakeholderId, that.payeeStakeholderId)
			&& Objects.equals(paymentMethod, that.paymentMethod) && Objects.equals(payeeName, that.payeeName)
			&& Objects.equals(payeeAddress, that.payeeAddress) && Objects.equals(payeeCareOf, that.payeeCareOf)
			&& Objects.equals(payeeZipCode, that.payeeZipCode) && Objects.equals(payeeCity, that.payeeCity)
			&& Objects.equals(clearingNumber, that.clearingNumber) && Objects.equals(accountNumber, that.accountNumber)
			&& Objects.equals(localPaymentNumber, that.localPaymentNumber) && Objects.equals(invoiceNumber, that.invoiceNumber)
			&& Objects.equals(messageLines, that.messageLines);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, lifecareId, moneyType, paymentDate, amount, applicationMonth, accountingCode, reportedOnStakeholderIds, accountingDate,
			excludedFromPayment, payeeStakeholderId, paymentMethod, payeeName, payeeAddress, payeeCareOf, payeeZipCode, payeeCity,
			clearingNumber, accountNumber, localPaymentNumber, invoiceNumber, usesOcr, messageLines);
	}

	@Override
	public String toString() {
		return "PaymentRequest{" +
			"source='" + source + '\'' +
			", lifecareId='" + lifecareId + '\'' +
			", moneyType='" + moneyType + '\'' +
			", paymentDate=" + paymentDate +
			", amount=" + amount +
			", applicationMonth='" + applicationMonth + '\'' +
			", accountingCode='" + accountingCode + '\'' +
			", reportedOnStakeholderIds=" + reportedOnStakeholderIds +
			", accountingDate=" + accountingDate +
			", excludedFromPayment=" + excludedFromPayment +
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
			'}';
	}
}
