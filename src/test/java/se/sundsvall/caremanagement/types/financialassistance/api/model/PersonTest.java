package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class PersonTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(Person.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var role = "APPLICANT";
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		final var needsInterpreter = true;
		final var interpreterLanguage = "Arabiska";
		final var hadWorkLast12Months = false;
		final var hadWorkDescription = "Substitute in home care services";
		final var paymentMethod = "BANK_ACCOUNT";
		final var clearingNumber = "8327-9";
		final var accountNumber = "123456789";
		final var otherPaymentDescription = "Paymentskort";
		final var paymentSameAsPrevious = true;
		final var email = "anna.andersson@example.com";
		final var phone = "+46701234567";
		final var notifyByEmail = true;
		final var notifyBySms = true;

		final var result = Person.create()
			.withRole(role)
			.withPartyId(partyId)
			.withNeedsInterpreter(needsInterpreter)
			.withInterpreterLanguage(interpreterLanguage)
			.withHadWorkLast12Months(hadWorkLast12Months)
			.withHadWorkDescription(hadWorkDescription)
			.withPaymentMethod(paymentMethod)
			.withClearingNumber(clearingNumber)
			.withAccountNumber(accountNumber)
			.withOtherPaymentDescription(otherPaymentDescription)
			.withPaymentSameAsPrevious(paymentSameAsPrevious)
			.withEmail(email)
			.withPhone(phone)
			.withNotifyByEmail(notifyByEmail)
			.withNotifyBySms(notifyBySms);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getRole()).isEqualTo(role);
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getNeedsInterpreter()).isEqualTo(needsInterpreter);
		assertThat(result.getInterpreterLanguage()).isEqualTo(interpreterLanguage);
		assertThat(result.getHadWorkLast12Months()).isEqualTo(hadWorkLast12Months);
		assertThat(result.getHadWorkDescription()).isEqualTo(hadWorkDescription);
		assertThat(result.getPaymentMethod()).isEqualTo(paymentMethod);
		assertThat(result.getClearingNumber()).isEqualTo(clearingNumber);
		assertThat(result.getAccountNumber()).isEqualTo(accountNumber);
		assertThat(result.getOtherPaymentDescription()).isEqualTo(otherPaymentDescription);
		assertThat(result.getPaymentSameAsPrevious()).isEqualTo(paymentSameAsPrevious);
		assertThat(result.getEmail()).isEqualTo(email);
		assertThat(result.getPhone()).isEqualTo(phone);
		assertThat(result.getNotifyByEmail()).isEqualTo(notifyByEmail);
		assertThat(result.getNotifyBySms()).isEqualTo(notifyBySms);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Person.create()).hasAllNullFieldsOrProperties();
	}
}
