package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

import static org.hibernate.Length.LONG32;

@Embeddable
public class FaPerson {

	@Column(name = "role")
	private String role;

	@Column(name = "party_id")
	private String partyId;

	@Column(name = "needs_interpreter")
	private Boolean needsInterpreter;

	@Column(name = "interpreter_language")
	private String interpreterLanguage;

	@Column(name = "had_work_last12_months")
	private Boolean hadWorkLast12Months;

	@Column(name = "had_work_description", length = LONG32)
	private String hadWorkDescription;

	@Column(name = "payment_method")
	private String paymentMethod;

	@Column(name = "clearing_number")
	private String clearingNumber;

	@Column(name = "account_number")
	private String accountNumber;

	@Column(name = "other_payment_description", length = LONG32)
	private String otherPaymentDescription;

	@Column(name = "payment_same_as_previous")
	private Boolean paymentSameAsPrevious;

	@Column(name = "email")
	private String email;

	@Column(name = "phone")
	private String phone;

	@Column(name = "notify_by_email")
	private Boolean notifyByEmail;

	@Column(name = "notify_by_sms")
	private Boolean notifyBySms;

	public static FaPerson create() {
		return new FaPerson();
	}

	public String getRole() {
		return role;
	}

	public String getPartyId() {
		return partyId;
	}

	public Boolean getNeedsInterpreter() {
		return needsInterpreter;
	}

	public String getInterpreterLanguage() {
		return interpreterLanguage;
	}

	public Boolean getHadWorkLast12Months() {
		return hadWorkLast12Months;
	}

	public String getHadWorkDescription() {
		return hadWorkDescription;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public String getClearingNumber() {
		return clearingNumber;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public String getOtherPaymentDescription() {
		return otherPaymentDescription;
	}

	public Boolean getPaymentSameAsPrevious() {
		return paymentSameAsPrevious;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public Boolean getNotifyByEmail() {
		return notifyByEmail;
	}

	public Boolean getNotifyBySms() {
		return notifyBySms;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public void setPartyId(final String partyId) {
		this.partyId = partyId;
	}

	public void setNeedsInterpreter(final Boolean needsInterpreter) {
		this.needsInterpreter = needsInterpreter;
	}

	public void setInterpreterLanguage(final String interpreterLanguage) {
		this.interpreterLanguage = interpreterLanguage;
	}

	public void setHadWorkLast12Months(final Boolean hadWorkLast12Months) {
		this.hadWorkLast12Months = hadWorkLast12Months;
	}

	public void setHadWorkDescription(final String hadWorkDescription) {
		this.hadWorkDescription = hadWorkDescription;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public void setClearingNumber(final String clearingNumber) {
		this.clearingNumber = clearingNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public void setOtherPaymentDescription(final String otherPaymentDescription) {
		this.otherPaymentDescription = otherPaymentDescription;
	}

	public void setPaymentSameAsPrevious(final Boolean paymentSameAsPrevious) {
		this.paymentSameAsPrevious = paymentSameAsPrevious;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public void setPhone(final String phone) {
		this.phone = phone;
	}

	public void setNotifyByEmail(final Boolean notifyByEmail) {
		this.notifyByEmail = notifyByEmail;
	}

	public void setNotifyBySms(final Boolean notifyBySms) {
		this.notifyBySms = notifyBySms;
	}

	public FaPerson withRole(final String role) {
		this.role = role;
		return this;
	}

	public FaPerson withPartyId(final String partyId) {
		this.partyId = partyId;
		return this;
	}

	public FaPerson withNeedsInterpreter(final Boolean needsInterpreter) {
		this.needsInterpreter = needsInterpreter;
		return this;
	}

	public FaPerson withInterpreterLanguage(final String interpreterLanguage) {
		this.interpreterLanguage = interpreterLanguage;
		return this;
	}

	public FaPerson withHadWorkLast12Months(final Boolean hadWorkLast12Months) {
		this.hadWorkLast12Months = hadWorkLast12Months;
		return this;
	}

	public FaPerson withHadWorkDescription(final String hadWorkDescription) {
		this.hadWorkDescription = hadWorkDescription;
		return this;
	}

	public FaPerson withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public FaPerson withClearingNumber(final String clearingNumber) {
		this.clearingNumber = clearingNumber;
		return this;
	}

	public FaPerson withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public FaPerson withOtherPaymentDescription(final String otherPaymentDescription) {
		this.otherPaymentDescription = otherPaymentDescription;
		return this;
	}

	public FaPerson withPaymentSameAsPrevious(final Boolean paymentSameAsPrevious) {
		this.paymentSameAsPrevious = paymentSameAsPrevious;
		return this;
	}

	public FaPerson withEmail(final String email) {
		this.email = email;
		return this;
	}

	public FaPerson withPhone(final String phone) {
		this.phone = phone;
		return this;
	}

	public FaPerson withNotifyByEmail(final Boolean notifyByEmail) {
		this.notifyByEmail = notifyByEmail;
		return this;
	}

	public FaPerson withNotifyBySms(final Boolean notifyBySms) {
		this.notifyBySms = notifyBySms;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaPerson that = (FaPerson) o;
		return Objects.equals(role, that.role) && Objects.equals(partyId, that.partyId)
			&& Objects.equals(needsInterpreter, that.needsInterpreter) && Objects.equals(interpreterLanguage, that.interpreterLanguage)
			&& Objects.equals(hadWorkLast12Months, that.hadWorkLast12Months) && Objects.equals(hadWorkDescription, that.hadWorkDescription)
			&& Objects.equals(paymentMethod, that.paymentMethod) && Objects.equals(clearingNumber, that.clearingNumber)
			&& Objects.equals(accountNumber, that.accountNumber) && Objects.equals(otherPaymentDescription, that.otherPaymentDescription)
			&& Objects.equals(paymentSameAsPrevious, that.paymentSameAsPrevious) && Objects.equals(email, that.email)
			&& Objects.equals(phone, that.phone) && Objects.equals(notifyByEmail, that.notifyByEmail)
			&& Objects.equals(notifyBySms, that.notifyBySms);
	}

	@Override
	public int hashCode() {
		return Objects.hash(role, partyId, needsInterpreter, interpreterLanguage, hadWorkLast12Months, hadWorkDescription,
			paymentMethod, clearingNumber, accountNumber, otherPaymentDescription, paymentSameAsPrevious, email, phone,
			notifyByEmail, notifyBySms);
	}

	@Override
	public String toString() {
		return "FaPerson{role='" + role + "', partyId='" + partyId + "', needsInterpreter=" + needsInterpreter
			+ ", interpreterLanguage='" + interpreterLanguage + "', hadWorkLast12Months=" + hadWorkLast12Months
			+ ", hadWorkDescription='" + hadWorkDescription + "', paymentMethod='" + paymentMethod + "', clearingNumber='"
			+ clearingNumber + "', accountNumber='" + accountNumber + "', otherPaymentDescription='" + otherPaymentDescription
			+ "', paymentSameAsPrevious=" + paymentSameAsPrevious + ", email='" + email + "', phone='" + phone
			+ "', notifyByEmail=" + notifyByEmail + ", notifyBySms=" + notifyBySms + '}';
	}
}
