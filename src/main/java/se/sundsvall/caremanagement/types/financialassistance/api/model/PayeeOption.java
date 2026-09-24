package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * One selectable betalningsmottagare for an errand's payment form — flat on purpose, because it is a dropdown row.
 *
 * <p>
 * Two provenances share the shape. A {@code LIFECARE} option is derived from the applicant's actual Lifecare payments
 * in the last 12 months and has no {@code id}: FamilyCare exposes no payee register, so a past payment is the only
 * evidence a payee exists. A {@code MANUAL} option is a row the caseworker added on this errand, and carries the
 * {@code lifecareStatus} of its creation in Lifecare.
 * </p>
 */
@Schema(description = "A selectable betalningsmottagare — either derived from the applicant's Lifecare payment history or added by hand on the errand.")
public class PayeeOption {

	@Schema(description = "The payee's id on the errand. Null for a LIFECARE-derived option, which is not a stored row", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@Schema(description = "Name of the payee as registered in Lifecare", examples = "Anna Andersson")
	private String name;

	@Schema(description = "The Lifecare payment method, e.g. bank account, bankgiro, plusgiro or utbetalningskort", examples = "Personkonto")
	private String paymentMethod;

	@Schema(description = "Clearing number, when the payment method needs one", examples = "6000")
	private String clearing;

	@Schema(description = "Account, bankgiro or plusgiro number, when the payment method needs one", examples = "123456789")
	private String accountNumber;

	@Schema(description = "Where the option comes from: LIFECARE (seen on a payment in the last 12 months) or MANUAL (added by hand on this errand)", examples = "LIFECARE", allowableValues = {
		"LIFECARE", "MANUAL"
	}, accessMode = Schema.AccessMode.READ_ONLY)
	private String source;

	@Schema(description = "For a MANUAL option, how far its creation in Lifecare has got: PENDING until it is reported back, then SYNCED or FAILED. Null for a LIFECARE option, which is in Lifecare by definition",
		examples = "PENDING",
		allowableValues = {
			"PENDING", "SYNCED", "FAILED"
		},
		accessMode = Schema.AccessMode.READ_ONLY)
	private String lifecareStatus;

	@Schema(description = "The payee id Lifecare gave, when the lifecare-result report carried one", examples = "44213", accessMode = Schema.AccessMode.READ_ONLY)
	private String lifecarePayeeId;

	@Schema(description = "Lifecare's own message when lifecareStatus is FAILED — shown to the caseworker as-is", examples = "Kontonummer har fel format", accessMode = Schema.AccessMode.READ_ONLY)
	private String lifecareDetail;

	@Schema(description = "For a LIFECARE option, the date of the most recent payment to this payee — passed through as the raw Lifecare string, like every other date read out of Lifecare", examples = "2026-08-27", accessMode = Schema.AccessMode.READ_ONLY)
	private String lastPaidOn;

	@Schema(description = "When the MANUAL option was added", accessMode = Schema.AccessMode.READ_ONLY)
	private OffsetDateTime created;

	public static PayeeOption create() {
		return new PayeeOption();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public PayeeOption withId(final String id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public PayeeOption withName(final String name) {
		this.name = name;
		return this;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public PayeeOption withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public String getClearing() {
		return clearing;
	}

	public void setClearing(final String clearing) {
		this.clearing = clearing;
	}

	public PayeeOption withClearing(final String clearing) {
		this.clearing = clearing;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public PayeeOption withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public PayeeOption withSource(final String source) {
		this.source = source;
		return this;
	}

	public String getLifecareStatus() {
		return lifecareStatus;
	}

	public void setLifecareStatus(final String lifecareStatus) {
		this.lifecareStatus = lifecareStatus;
	}

	public PayeeOption withLifecareStatus(final String lifecareStatus) {
		this.lifecareStatus = lifecareStatus;
		return this;
	}

	public String getLifecarePayeeId() {
		return lifecarePayeeId;
	}

	public void setLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
	}

	public PayeeOption withLifecarePayeeId(final String lifecarePayeeId) {
		this.lifecarePayeeId = lifecarePayeeId;
		return this;
	}

	public String getLifecareDetail() {
		return lifecareDetail;
	}

	public void setLifecareDetail(final String lifecareDetail) {
		this.lifecareDetail = lifecareDetail;
	}

	public PayeeOption withLifecareDetail(final String lifecareDetail) {
		this.lifecareDetail = lifecareDetail;
		return this;
	}

	public String getLastPaidOn() {
		return lastPaidOn;
	}

	public void setLastPaidOn(final String lastPaidOn) {
		this.lastPaidOn = lastPaidOn;
	}

	public PayeeOption withLastPaidOn(final String lastPaidOn) {
		this.lastPaidOn = lastPaidOn;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public PayeeOption withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PayeeOption that = (PayeeOption) o;
		return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(paymentMethod, that.paymentMethod)
			&& Objects.equals(clearing, that.clearing) && Objects.equals(accountNumber, that.accountNumber)
			&& Objects.equals(source, that.source) && Objects.equals(lifecareStatus, that.lifecareStatus)
			&& Objects.equals(lifecarePayeeId, that.lifecarePayeeId) && Objects.equals(lifecareDetail, that.lifecareDetail)
			&& Objects.equals(lastPaidOn, that.lastPaidOn) && Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, paymentMethod, clearing, accountNumber, source, lifecareStatus, lifecarePayeeId, lifecareDetail, lastPaidOn, created);
	}

	@Override
	public String toString() {
		return "PayeeOption{" +
			"id='" + id + '\'' +
			", name='" + name + '\'' +
			", paymentMethod='" + paymentMethod + '\'' +
			", clearing='" + clearing + '\'' +
			", accountNumber='" + accountNumber + '\'' +
			", source='" + source + '\'' +
			", lifecareStatus='" + lifecareStatus + '\'' +
			", lifecarePayeeId='" + lifecarePayeeId + '\'' +
			", lifecareDetail='" + lifecareDetail + '\'' +
			", lastPaidOn='" + lastPaidOn + '\'' +
			", created=" + created +
			'}';
	}
}
