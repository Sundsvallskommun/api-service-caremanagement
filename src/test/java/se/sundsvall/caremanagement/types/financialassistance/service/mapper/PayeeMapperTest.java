package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.sundsvall.caremanagement.lifecare.service.model.PaymentView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeOption;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPayeeEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.PayeeMapper.SOURCE_LIFECARE;
import static se.sundsvall.caremanagement.types.financialassistance.service.mapper.PayeeMapper.SOURCE_MANUAL;

class PayeeMapperTest {

	private static PaymentView paymentView() {
		return new PaymentView(1, new BigDecimal("7900.00"), "Personkonto", "2026-08-27", "6000", "123456789",
			"Anna Andersson", "Storgatan 1", null, "85230", "Sundsvall", "Hyra", "2026-08");
	}

	@Test
	void toPayeeOptionFromPaymentView() {
		final var result = PayeeMapper.toPayeeOption(paymentView());

		assertThat(result.getId()).isNull();
		assertThat(result.getName()).isEqualTo("Anna Andersson");
		assertThat(result.getPaymentMethod()).isEqualTo("Personkonto");
		assertThat(result.getClearing()).isEqualTo("6000");
		assertThat(result.getAccountNumber()).isEqualTo("123456789");
		assertThat(result.getSource()).isEqualTo(SOURCE_LIFECARE);
		assertThat(result.getLastPaidOn()).isEqualTo("2026-08-27");
		assertThat(result.getLifecareStatus()).isNull();
	}

	@Test
	void toPayeeOptionFromEntity() {
		final var created = OffsetDateTime.parse("2026-09-21T12:00:00Z");
		final var entity = FaPayeeEntity.create()
			.withId("payee-id")
			.withErrandId("errand-id")
			.withName("Sundsvalls Hyresbostäder AB")
			.withPaymentMethod("Bankgiro via Plusgiro")
			.withClearing(null)
			.withAccountNumber("5555-6666")
			.withLifecareStatus("PENDING")
			.withLifecarePayeeId("44213")
			.withLifecareDetail("detail")
			.withCreated(created);

		final var result = PayeeMapper.toPayeeOption(entity);

		assertThat(result.getId()).isEqualTo("payee-id");
		assertThat(result.getName()).isEqualTo("Sundsvalls Hyresbostäder AB");
		assertThat(result.getPaymentMethod()).isEqualTo("Bankgiro via Plusgiro");
		assertThat(result.getClearing()).isNull();
		assertThat(result.getAccountNumber()).isEqualTo("5555-6666");
		assertThat(result.getSource()).isEqualTo(SOURCE_MANUAL);
		assertThat(result.getLifecareStatus()).isEqualTo("PENDING");
		assertThat(result.getLifecarePayeeId()).isEqualTo("44213");
		assertThat(result.getLifecareDetail()).isEqualTo("detail");
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getLastPaidOn()).isNull();
	}

	@Test
	void applyRequestCopiesTheCaseworkerFieldsOnly() {
		final var entity = FaPayeeEntity.create()
			.withId("payee-id")
			.withErrandId("errand-id")
			.withLifecareStatus("SYNCED");

		final var result = PayeeMapper.applyRequest(entity, PayeeRequest.create()
			.withName("Ny Mottagare")
			.withPaymentMethod("Plusgiro")
			.withClearing("1234")
			.withAccountNumber("99 99 99-9"));

		assertThat(result.getName()).isEqualTo("Ny Mottagare");
		assertThat(result.getPaymentMethod()).isEqualTo("Plusgiro");
		assertThat(result.getClearing()).isEqualTo("1234");
		assertThat(result.getAccountNumber()).isEqualTo("99 99 99-9");
		assertThat(result.getId()).isEqualTo("payee-id");
		assertThat(result.getErrandId()).isEqualTo("errand-id");
		assertThat(result.getLifecareStatus()).isEqualTo("SYNCED");
	}

	private static PayeeOption option(final String name, final String method, final String clearing, final String account) {
		return PayeeOption.create().withName(name).withPaymentMethod(method).withClearing(clearing).withAccountNumber(account);
	}

	private static Stream<Arguments> equalKeys() {
		return Stream.of(
			arguments("identical", option("Anna", "Personkonto", "6000", "123"), option("Anna", "Personkonto", "6000", "123")),
			arguments("case differs", option("ANNA", "personkonto", "6000", "123"), option("anna", "Personkonto", "6000", "123")),
			arguments("surrounding whitespace", option(" Anna ", "Personkonto ", " 6000", "123 "), option("Anna", "Personkonto", "6000", "123")),
			arguments("null and blank clearing", option("Anna", "Personkonto", null, "123"), option("Anna", "Personkonto", "", "123")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("equalKeys")
	void keysMatchForTheSameAccount(final String name, final PayeeOption one, final PayeeOption other) {
		assertThat(PayeeMapper.key(one)).isEqualTo(PayeeMapper.key(other));
	}

	private static Stream<Arguments> differentKeys() {
		return Stream.of(
			arguments("name differs", option("Anna", "Personkonto", "6000", "123"), option("Bertil", "Personkonto", "6000", "123")),
			arguments("account differs", option("Anna", "Personkonto", "6000", "123"), option("Anna", "Personkonto", "6000", "456")),
			arguments("method differs", option("Anna", "Personkonto", "6000", "123"), option("Anna", "Plusgiro", "6000", "123")),
			arguments("clearing differs", option("Anna", "Personkonto", "6000", "123"), option("Anna", "Personkonto", "8327", "123")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("differentKeys")
	void keysDifferForDifferentAccounts(final String name, final PayeeOption one, final PayeeOption other) {
		assertThat(PayeeMapper.key(one)).isNotEqualTo(PayeeMapper.key(other));
	}

	@Test
	void aFinalizePayeeKeysTheSameAsTheMatchingOption() {
		final var payee = Payee.create()
			.withName(" Hyresvärden AB ")
			.withPaymentMethod("BANKGIRO")
			.withClearing(null)
			.withAccountNumber("5555-6666");

		assertThat(PayeeMapper.key(payee))
			.isEqualTo(PayeeMapper.key(option("hyresvärden ab", "Bankgiro", "", "5555-6666")));
	}

	@Test
	void aFinalizePayeeForAnotherAccountKeysDifferently() {
		final var payee = Payee.create().withName("Hyresvärden AB").withPaymentMethod("Bankgiro").withAccountNumber("7777-8888");

		assertThat(PayeeMapper.key(payee))
			.isNotEqualTo(PayeeMapper.key(option("Hyresvärden AB", "Bankgiro", null, "5555-6666")));
	}

	@Test
	void keyOfAnEmptyOptionIsAllBlanks() {
		assertThat(PayeeMapper.key(PayeeOption.create()))
			.isEqualTo(new PayeeMapper.PayeeKey("", "", "", ""));
	}
}
