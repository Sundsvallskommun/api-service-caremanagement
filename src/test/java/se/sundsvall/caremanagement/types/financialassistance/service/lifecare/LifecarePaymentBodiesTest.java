package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentRequest;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentBodies.buildPayeeCreate;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentBodies.buildPaymentCreate;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentBodies.findMatchingPayee;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentBodies.findRegisteredPayment;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentBodies.toConcernMonth;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.payment;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.serialised;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.underlag;

class LifecarePaymentBodiesTest {

	/** Capture 2: POST Payment/Create, 2026-09-21, field for field and in order. */
	private static final String CAPTURE_2 = """
		{
		  "paymentId": 0, "serviceId": 1, "amount": 1, "chainId": 2, "investigationExecutionId": 2, "paymentType": 0,
		  "paymentMethod": 14, "paymentMethodText": null, "payDate": "2026-09-21", "concernedMonth": "202609", "concernedMonthText": null,
		  "bookkeepingDate": "", "integrationDate": "", "cancellationDate": "", "updateTimestamp": "", "updateSignature": null,
		  "cancellationSignature": null, "status": 0, "statusText": null, "error": 0, "noIntegration": false, "clearing": "",
		  "accountNumber": "11111111", "memorialAccountNumber": null, "name": "Kontoinnehavare A", "streetAddress": "", "careOfAddress": "",
		  "postalCode": "", "postalAddress": "Sundsvall", "billingNumber": "123", "localNumber": "", "ocrCheck": false,
		  "voucherNumber": null, "susPersonId": "19800101T001", "applicationText": null,
		  "maxAmountForPayment": 1000.0, "notificationOnlyOfMaxAmountForPaymentActivated": true, "message": "     ",
		  "messageRow1": "", "messageRow2": "", "messageRow3": "", "messageRow4": "", "messageRow5": "", "messageRow6": "", "messageRow7": "",
		  "balance": { "serviceId": 1, "ownerType": 1, "ownerId": 1, "paymentType": 1, "approvedAmount": 4.0, "bookedAmount": 1.0,
		    "name": "Ek. Bistånd 3,00 ", "balanceAmount": 3.0, "id": 1 },
		  "paymentPersons": [
		    { "paymentId": 0, "personId": "19800101T001", "name": "Efternamn, Förnamn", "personKey": 0, "included": true, "susPerson": false,
		      "streetAddress": null, "careOfAddress": null, "postalCode": null, "postalAddress": null, "personIdFormatted": "800101-T001",
		      "personIdAndName": "800101-T001 Efternamn, Förnamn" }
		  ],
		  "postings": [
		    { "paymentId": 0, "account": "TEST UTB", "creditAccount": null, "simpleAccount": "", "amount": 1, "simpleCreditAccount": null,
		      "purpose": 1, "purposeText": "Försörjningsstöd exklusive tillfälligt boende" }
		  ],
		  "economicUnit": { "economicalUnitId": 1, "name": "Ekonomiskt bistånd", "startDate": "2026-09-01", "endDate": "", "giroUnit": "IFO",
		    "voucherNumberIn": "0", "voucherNumberOut": "0", "paymentRecieverOblig": true },
		  "serviceOrganizationText": "Mottagning ekonomiskt bistånd", "balanceId": 1, "dontShowOnMyPage": false, "maxPaymentYears": 0,
		  "creditAccount": "", "simpleAccount": ""
		}
		""";

	private static ObjectNode twoPostings() {
		final var underlag = underlag();
		((ObjectNode) underlag.get("payment")).set("postings", json("""
			[
			  { "paymentId": 0, "account": "TEST UTB", "amount": 0, "purpose": 1, "purposeText": "Försörjningsstöd exklusive tillfälligt boende" },
			  { "paymentId": 0, "account": "TEST UTB  TVÅ", "amount": 0, "purpose": 3, "purposeText": "Hälso och sjukvård" }
			]
			"""));
		return underlag;
	}

	private static void assertRefused(final ObjectNode underlag, final LifecarePaymentRequest request, final String reason) {
		assertThatThrownBy(() -> buildPaymentCreate(underlag, request))
			.isInstanceOfSatisfying(ThrowableProblem.class, problem -> {
				assertThat(problem.getStatus()).isEqualTo(UNPROCESSABLE_CONTENT);
				assertThat(problem.getDetail()).contains(reason);
			});
	}

	@Test
	void turnsCapture1IntoExactlyTheBodyOfCapture2() {
		final var body = buildPaymentCreate(underlag(), payment());

		assertThat(serialised(body)).isEqualTo(serialised(json(CAPTURE_2)));
	}

	@Test
	void leavesTheUnderlagUntouched() {
		final var underlag = underlag();

		buildPaymentCreate(underlag, payment());

		assertThat(serialised(underlag)).isEqualTo(serialised(underlag()));
	}

	@Test
	void fillsTheOptionalFieldsTheCaseworkerGave() {
		final var request = new LifecarePaymentRequest(null, BigDecimal.ONE, "2026-09", "bankgiro via plusgiro ", "Kontoinnehavare A", "Storgatan 1", "c/o B", "85230",
			"Sundsvall", null, "1111-1111", null, "L1", null, true, List.of("Rad 1", "Rad 2"));

		final var body = buildPaymentCreate(underlag(), request);

		assertThat(body.get("payDate").asString()).isEqualTo("2026-09-21");
		assertThat(body.get("streetAddress").asString()).isEqualTo("Storgatan 1");
		assertThat(body.get("careOfAddress").asString()).isEqualTo("c/o B");
		assertThat(body.get("postalCode").asString()).isEqualTo("85230");
		assertThat(body.get("localNumber").asString()).isEqualTo("L1");
		assertThat(body.get("billingNumber").isNull()).isTrue();
		assertThat(body.get("ocrCheck").asBoolean()).isTrue();
		assertThat(body.get("messageRow1").asString()).isEqualTo("Rad 1");
		assertThat(body.get("messageRow2").asString()).isEqualTo("Rad 2");
		assertThat(body.get("messageRow3").asString()).isEmpty();
	}

	@Test
	void holdsAnUtbetalningWithoutAmount() {
		assertRefused(underlag(), payment(null, "Bankgiro via Plusgiro", "2026-09", "11111111", null), "saknar belopp");
		assertRefused(underlag(), payment(BigDecimal.ZERO, "Bankgiro via Plusgiro", "2026-09", "11111111", null), "saknar belopp");
	}

	@Test
	void holdsAnUtbetalningTheBalanceDoesNotCover() {
		assertRefused(underlag(), payment(BigDecimal.valueOf(5), "Bankgiro via Plusgiro", "2026-09", "11111111", null), "Saldot i Lifecare (3 kr) räcker inte till 5 kr");
	}

	@Test
	void holdsAnUtbetalningToAPayeeLifecareDoesNotHave() {
		assertRefused(underlag(), payment(BigDecimal.ONE, "Bankgiro via Plusgiro", "2026-09", "99999999", null), "Mottagaren");
	}

	@Test
	void paysTheAdressEntryWithoutAnAccount() {
		final var request = new LifecarePaymentRequest(null, BigDecimal.ONE, "2026-09", "Bankgiro via Plusgiro", "adress", null, null, null, null, null, null, null, null,
			null, null, null);

		assertThat(buildPaymentCreate(underlag(), request).get("accountNumber").asString()).isEmpty();
		assertRefused(underlag(), new LifecarePaymentRequest(null, BigDecimal.ONE, "2026-09", "Bankgiro via Plusgiro", "Någon annan", null, null, null, null, null, null,
			null, null, null, null, null), "Mottagaren");
	}

	@Test
	void holdsABetalsattOrAMonthLifecareDoesNotOffer() {
		assertRefused(underlag(), payment(BigDecimal.ONE, "Plusgiro", "2026-09", "11111111", null), "Betalsättet \"Plusgiro\"");
		assertRefused(underlag(), payment(BigDecimal.ONE, "Memorial", "2026-09", "11111111", null), "Betalsättet");
		assertRefused(underlag(), payment(BigDecimal.ONE, "Bankgiro via Plusgiro", "2026-10", "11111111", null), "månaden 2026-10");
	}

	@Test
	void booksTheAmountOnTheChosenPurposeAndSendsEveryPostingTheOthersAtZero() {
		final var body = buildPaymentCreate(twoPostings(), payment(BigDecimal.ONE, "Bankgiro via Plusgiro", "2026-09", "11111111", " 1 "));

		assertThat(serialised(body.get("postings"))).isEqualTo(serialised(json("""
			[
			  { "paymentId": 0, "account": "TEST UTB", "amount": 1, "purpose": 1, "purposeText": "Försörjningsstöd exklusive tillfälligt boende" },
			  { "paymentId": 0, "account": "TEST UTB  TVÅ", "amount": 0, "purpose": 3, "purposeText": "Hälso och sjukvård" }
			]
			""")));
	}

	@Test
	void holdsSeveralPostingsWithoutAPurposeOrWithOneTheInsatsLacks() {
		assertRefused(twoPostings(), payment(), "Välj ändamål");
		assertRefused(twoPostings(), payment(BigDecimal.ONE, "Bankgiro via Plusgiro", "2026-09", "11111111", "9"), "Ändamålet \"9\" finns inte");
	}

	@Test
	void holdsAnInsatsWithoutPostingsOrWithOtherThanOneBalance() {
		final var noPostings = underlag();
		((ObjectNode) noPostings.get("payment")).putArray("postings");
		final var noBalance = underlag();
		noBalance.putArray("balances");
		final var twoBalances = underlag();
		twoBalances.withArray("balances").add(twoBalances.get("balances").get(0).deepCopy());

		assertRefused(noPostings, payment(), "ingen konteringsrad");
		assertRefused(noBalance, payment(), "inget saldo");
		assertRefused(twoBalances, payment(), "flera saldon");
	}

	@Test
	void holdsAnAmountOverABlockingMaximumButLetsANotifyOnlyOneThrough() {
		final var blocking = underlag();
		((ObjectNode) blocking.get("payment")).put("maxAmountForPayment", 0.5).put("notificationOnlyOfMaxAmountForPaymentActivated", false);
		final var notifying = underlag();
		((ObjectNode) notifying.get("payment")).put("maxAmountForPayment", 0.5);

		assertRefused(blocking, payment(), "maxbelopp");
		assertThat(buildPaymentCreate(notifying, payment())).isNotNull();
	}

	@Test
	void refusesAnUnderlagWhosePaymentIsNotAnObject() {
		final var underlag = underlag();
		underlag.putArray("payment");

		assertRefused(underlag, payment(), "ingen konteringsrad");
	}

	@Test
	void findsTheRegisteredPaymentTheBodyWouldDuplicate() {
		final var body = buildPaymentCreate(underlag(), payment());
		final var registered = json("""
			[
			  { "paymentId": 3, "amount": 1.0, "payDate": "2026-09-21", "concernedMonth": "202609", "accountNumber": "1111-1111", "cancellationDate": "2026-09-22" },
			  { "paymentId": 5, "amount": 2.0, "payDate": "2026-09-21", "concernedMonth": "202609", "accountNumber": "11111111", "cancellationDate": "" },
			  { "paymentId": 6, "amount": 1.0, "payDate": "2026-09-08", "concernedMonth": "202609", "accountNumber": "11111111", "cancellationDate": "" },
			  { "paymentId": 7, "amount": 1.0, "payDate": "2026-09-21", "concernedMonth": "202608", "accountNumber": "11111111", "cancellationDate": "" },
			  { "paymentId": 8, "amount": 1.0, "payDate": "2026-09-21", "concernedMonth": "202609", "accountNumber": "22222222", "cancellationDate": "" },
			  { "paymentId": 9, "payDate": "2026-09-21", "concernedMonth": "202609", "accountNumber": "11111111", "cancellationDate": "" },
			  { "paymentId": 4, "amount": 1.0, "payDate": "2026-09-21", "concernedMonth": "202609", "accountNumber": "1111-1111", "cancellationDate": "" }
			]
			""");

		assertThat(findRegisteredPayment(registered, body)).hasValueSatisfying(found -> assertThat(found.get("paymentId").asInt()).isEqualTo(4));
		assertThat(findRegisteredPayment(json("[]"), body)).isEmpty();
	}

	@Test
	void findsAnActivePayeeOnTheSameAccountAndBetalsattDashesOrNot() {
		final var payees = List.of(json("""
			{ "payeeId": 2, "paymentMethod": 14, "accountNumber": "1111-1111", "clearing": "", "isActive": true }
			"""));

		assertThat(findMatchingPayee(payees, new LifecarePayeeRequest("Ny", null, 14, null, "11111111")))
			.hasValueSatisfying(payee -> assertThat(payee.get("payeeId").asInt()).isEqualTo(2));
	}

	@Test
	void doesNotMatchAnotherBetalsattAnInactivePayeeTheAdressEntryOrAnotherClearing() {
		final var payees = List.of(
			json("{ \"payeeId\": 2, \"paymentMethod\": 13, \"accountNumber\": \"11111111\", \"clearing\": \"\", \"isActive\": true }"),
			json("{ \"payeeId\": 5, \"paymentMethod\": 14, \"accountNumber\": \"11111111\", \"clearing\": \"\", \"isActive\": false }"),
			json("{ \"payeeId\": 2147483646, \"paymentMethod\": 14, \"accountNumber\": \"11111111\", \"clearing\": \"\", \"isActive\": true }"),
			json("{ \"payeeId\": 6, \"paymentMethod\": 14, \"accountNumber\": \"11111111\", \"clearing\": \"6000\", \"isActive\": true }"));

		assertThat(findMatchingPayee(payees, new LifecarePayeeRequest("Ny", null, 14, null, "11111111"))).isEmpty();
	}

	@Test
	void neverMatchesWithoutAnAccountNumberToCompare() {
		final var payees = List.of(json("{ \"payeeId\": 2, \"paymentMethod\": 14, \"accountNumber\": \"\", \"isActive\": true }"));

		assertThat(findMatchingPayee(payees, new LifecarePayeeRequest("Ny", null, 14, null, null))).isEmpty();
	}

	@Test
	void buildsThePayeeBodyFieldForFieldAsTheLifecareWebAppSendsIt() {
		final var body = buildPayeeCreate("19800101T001", new LifecarePayeeRequest(" Kontoinnehavare B ", "Konto B", 14, null, "11111111"));

		assertThat(serialised(body)).isEqualTo("""
			{"payeeId":0,"payeeName":"Konto B","personId":"19800101T001","paymentMethod":14,"paymentMethodText":null,"accountNumber":"11111111",\
			"memorialAccountNumber":null,"clearing":"","name":"Kontoinnehavare B","streetAddress":null,"careOfAddress":null,"postalCode":"",\
			"postalAddress":"","ocrCheck":false,"addressFromPerson":false,"updateTimestamp":"","updateSignature":null,"isActive":true,"statusText":null}""");
	}

	@Test
	void labelsThePayeeByTheAccountHolderWhenNoLabelIsGiven() {
		final var body = buildPayeeCreate("19800101T001", new LifecarePayeeRequest("Kontoinnehavare B", " ", 14, "6000 ", null));

		assertThat(body.get("payeeName").asString()).isEqualTo("Kontoinnehavare B");
		assertThat(body.get("clearing").asString()).isEqualTo("6000");
		assertThat(body.get("accountNumber").asString()).isEmpty();
	}

	@Test
	void turnsTheApplicationMonthIntoLifecaresConcernMonth() {
		assertThat(toConcernMonth("2026-09")).isEqualTo("202609");
		assertThat(toConcernMonth(null)).isEmpty();
		assertThat(toConcernMonth("2026-09-01")).isEqualTo("202609");
	}
}
