package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

@Schema(description = "A person (applicant or co-applicant) on the financial assistance application.")
public class Person {

	@Schema(description = "Role of the person", examples = "APPLICANT", allowableValues = {
		"APPLICANT", "CO_APPLICANT"
	})
	@OneOf(value = {
		"APPLICANT", "CO_APPLICANT"
	}, nullable = true)
	private String role;

	@Schema(description = "Personal number", examples = "198001012389")
	private String personalNumber;

	@Schema(description = "Whether the person needs an interpreter", examples = "true")
	private Boolean needsInterpreter;

	@Schema(description = "Language the interpreter should use", examples = "Arabiska")
	private String interpreterLanguage;

	@Schema(description = "Whether the person had work during the last 12 months", examples = "false")
	private Boolean hadWorkLast12Months;

	@Schema(description = "Description of the work the person had", examples = "Vikarie inom hemtjänsten")
	private String hadWorkDescription;

	@Schema(description = "Payment method", examples = "BANK_ACCOUNT", allowableValues = {
		"BANK_ACCOUNT", "OTHER"
	})
	@OneOf(value = {
		"BANK_ACCOUNT", "OTHER"
	}, nullable = true)
	private String paymentMethod;

	@Schema(description = "Clearing number of the bank account", examples = "8327-9")
	private String clearingNumber;

	@Schema(description = "Bank account number", examples = "123456789")
	private String accountNumber;

	@Schema(description = "Description of the payment method when OTHER", examples = "Utbetalningskort")
	private String otherPaymentDescription;

	@Schema(description = "Whether the payment details are the same as previously used", examples = "true")
	private Boolean paymentSameAsPrevious;

	public static Person create() {
		return new Person();
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public Person withRole(final String role) {
		this.role = role;
		return this;
	}

	public String getPersonalNumber() {
		return personalNumber;
	}

	public void setPersonalNumber(final String personalNumber) {
		this.personalNumber = personalNumber;
	}

	public Person withPersonalNumber(final String personalNumber) {
		this.personalNumber = personalNumber;
		return this;
	}

	public Boolean getNeedsInterpreter() {
		return needsInterpreter;
	}

	public void setNeedsInterpreter(final Boolean needsInterpreter) {
		this.needsInterpreter = needsInterpreter;
	}

	public Person withNeedsInterpreter(final Boolean needsInterpreter) {
		this.needsInterpreter = needsInterpreter;
		return this;
	}

	public String getInterpreterLanguage() {
		return interpreterLanguage;
	}

	public void setInterpreterLanguage(final String interpreterLanguage) {
		this.interpreterLanguage = interpreterLanguage;
	}

	public Person withInterpreterLanguage(final String interpreterLanguage) {
		this.interpreterLanguage = interpreterLanguage;
		return this;
	}

	public Boolean getHadWorkLast12Months() {
		return hadWorkLast12Months;
	}

	public void setHadWorkLast12Months(final Boolean hadWorkLast12Months) {
		this.hadWorkLast12Months = hadWorkLast12Months;
	}

	public Person withHadWorkLast12Months(final Boolean hadWorkLast12Months) {
		this.hadWorkLast12Months = hadWorkLast12Months;
		return this;
	}

	public String getHadWorkDescription() {
		return hadWorkDescription;
	}

	public void setHadWorkDescription(final String hadWorkDescription) {
		this.hadWorkDescription = hadWorkDescription;
	}

	public Person withHadWorkDescription(final String hadWorkDescription) {
		this.hadWorkDescription = hadWorkDescription;
		return this;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public Person withPaymentMethod(final String paymentMethod) {
		this.paymentMethod = paymentMethod;
		return this;
	}

	public String getClearingNumber() {
		return clearingNumber;
	}

	public void setClearingNumber(final String clearingNumber) {
		this.clearingNumber = clearingNumber;
	}

	public Person withClearingNumber(final String clearingNumber) {
		this.clearingNumber = clearingNumber;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public Person withAccountNumber(final String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public String getOtherPaymentDescription() {
		return otherPaymentDescription;
	}

	public void setOtherPaymentDescription(final String otherPaymentDescription) {
		this.otherPaymentDescription = otherPaymentDescription;
	}

	public Person withOtherPaymentDescription(final String otherPaymentDescription) {
		this.otherPaymentDescription = otherPaymentDescription;
		return this;
	}

	public Boolean getPaymentSameAsPrevious() {
		return paymentSameAsPrevious;
	}

	public void setPaymentSameAsPrevious(final Boolean paymentSameAsPrevious) {
		this.paymentSameAsPrevious = paymentSameAsPrevious;
	}

	public Person withPaymentSameAsPrevious(final Boolean paymentSameAsPrevious) {
		this.paymentSameAsPrevious = paymentSameAsPrevious;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Person that = (Person) o;
		return Objects.equals(role, that.role) && Objects.equals(personalNumber, that.personalNumber)
			&& Objects.equals(needsInterpreter, that.needsInterpreter) && Objects.equals(interpreterLanguage, that.interpreterLanguage)
			&& Objects.equals(hadWorkLast12Months, that.hadWorkLast12Months) && Objects.equals(hadWorkDescription, that.hadWorkDescription)
			&& Objects.equals(paymentMethod, that.paymentMethod) && Objects.equals(clearingNumber, that.clearingNumber)
			&& Objects.equals(accountNumber, that.accountNumber) && Objects.equals(otherPaymentDescription, that.otherPaymentDescription)
			&& Objects.equals(paymentSameAsPrevious, that.paymentSameAsPrevious);
	}

	@Override
	public int hashCode() {
		return Objects.hash(role, personalNumber, needsInterpreter, interpreterLanguage, hadWorkLast12Months,
			hadWorkDescription, paymentMethod, clearingNumber, accountNumber, otherPaymentDescription, paymentSameAsPrevious);
	}

	@Override
	public String toString() {
		return "Person{role='" + role + "', personalNumber='" + personalNumber + "', needsInterpreter=" + needsInterpreter
			+ ", interpreterLanguage='" + interpreterLanguage + "', hadWorkLast12Months=" + hadWorkLast12Months
			+ ", hadWorkDescription='" + hadWorkDescription + "', paymentMethod='" + paymentMethod + "', clearingNumber='"
			+ clearingNumber + "', accountNumber='" + accountNumber + "', otherPaymentDescription='" + otherPaymentDescription
			+ "', paymentSameAsPrevious=" + paymentSameAsPrevious + '}';
	}
}
