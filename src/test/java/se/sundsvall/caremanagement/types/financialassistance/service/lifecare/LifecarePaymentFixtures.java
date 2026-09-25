package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.util.List;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Lifecare captures (made-up persons and accounts) shared by the payment tests.
 */
final class LifecarePaymentFixtures {

	static final JsonMapper JSON = JsonMapper.builder().build();

	/** Capture 1: GET Payment/GetPaymentForCreate, 2026-09-21, with the payment's fields in their own order. */
	static final String UNDERLAG = """
		{
		  "payment": {
		    "paymentId": 0, "serviceId": 1, "amount": 0.0, "chainId": 2, "investigationExecutionId": 2, "paymentType": 0,
		    "paymentMethod": 0, "paymentMethodText": null, "payDate": "2026-09-21", "concernedMonth": null, "concernedMonthText": null,
		    "bookkeepingDate": "", "integrationDate": "", "cancellationDate": "", "updateTimestamp": "", "updateSignature": null,
		    "cancellationSignature": null, "status": 0, "statusText": null, "error": 0, "noIntegration": false, "clearing": null,
		    "accountNumber": null, "memorialAccountNumber": null, "name": null, "streetAddress": null, "careOfAddress": null,
		    "postalCode": null, "postalAddress": null, "billingNumber": null, "localNumber": null, "ocrCheck": false,
		    "voucherNumber": null, "susPersonId": "19800101T001", "aktualiseringId": null, "applicationText": null,
		    "maxAmountForPayment": 1000.0, "notificationOnlyOfMaxAmountForPaymentActivated": true, "message": "     ",
		    "messageRow1": null, "messageRow2": null, "messageRow3": null, "messageRow4": null, "messageRow5": null,
		    "messageRow6": null, "messageRow7": null, "balance": null,
		    "paymentPersons": [
		      { "paymentId": 0, "personId": "19800101T001", "name": "Efternamn, Förnamn", "personKey": 0, "included": true, "susPerson": false,
		        "streetAddress": null, "careOfAddress": null, "postalCode": null, "postalAddress": null, "personIdFormatted": "800101-T001" }
		    ],
		    "postings": [
		      { "paymentId": 0, "account": "TEST UTB", "creditAccount": null, "simpleAccount": "", "amount": 0.0, "simpleCreditAccount": null,
		        "purpose": 1, "purposeText": "Försörjningsstöd exklusive tillfälligt boende" }
		    ],
		    "economicUnit": { "economicalUnitId": 1, "name": "Ekonomiskt bistånd", "startDate": "2026-09-01", "endDate": "", "giroUnit": "IFO",
		      "voucherNumberIn": "0", "voucherNumberOut": "0", "paymentRecieverOblig": true },
		    "serviceOrganizationText": "Mottagning ekonomiskt bistånd", "balanceId": 0, "dontShowOnMyPage": false, "maxPaymentYears": 0
		  },
		  "payees": [
		    { "payeeId": 2147483646, "payeeName": "Adress", "personId": "19800101T001", "paymentMethod": 0, "paymentMethodText": null,
		      "accountNumber": "", "memorialAccountNumber": null, "clearing": "", "name": "Kontoinnehavare A", "streetAddress": "",
		      "careOfAddress": "", "postalCode": "", "postalAddress": "Sundsvall", "ocrCheck": false, "addressFromPerson": false,
		      "updateTimestamp": "", "updateSignature": null, "isActive": true, "statusText": null },
		    { "payeeId": 2, "payeeName": "Konto A", "personId": "19800101T001", "paymentMethod": 14, "paymentMethodText": "Bankgiro via Plusgiro",
		      "accountNumber": "11111111", "memorialAccountNumber": null, "clearing": "", "name": "Kontoinnehavare A", "streetAddress": "",
		      "careOfAddress": "", "postalCode": "", "postalAddress": "Sundsvall", "ocrCheck": false, "addressFromPerson": false,
		      "updateTimestamp": "2026-09-11", "updateSignature": "handlaggare1", "isActive": true, "statusText": null },
		    { "payeeId": 9, "payeeName": "Konto A", "personId": "19800101T001", "paymentMethod": 14, "paymentMethodText": "Bankgiro via Plusgiro",
		      "accountNumber": "11111111", "clearing": "", "name": "Kontoinnehavare A", "isActive": false }
		  ],
		  "paymentMethods": [
		    { "paymentCode": 14, "payment": "Bankgiro via Plusgiro", "inUse": true, "localNumberEnabled": false, "localNumberMandatory": false },
		    { "paymentCode": 21, "payment": "Memorial", "inUse": false, "localNumberEnabled": false, "localNumberMandatory": false }
		  ],
		  "paymentConcernMonths": [ { "concernMonth": "202609", "displayMonth": "September 2026" } ],
		  "balances": [
		    { "serviceId": 1, "ownerType": 1, "ownerId": 1, "paymentType": 1, "approvedAmount": 4.0, "bookedAmount": 1.0,
		      "name": "Ek. Bistånd 3,00 ", "balanceAmount": 3.0 }
		  ]
		}
		""";

	private LifecarePaymentFixtures() {}

	static JsonNode json(final String text) {
		return JSON.readTree(text);
	}

	static ObjectNode underlag() {
		return (ObjectNode) json(UNDERLAG);
	}

	/** The caseworker's utbetalning for the same payment as capture 2. */
	static LifecarePaymentRequest payment() {
		return new LifecarePaymentRequest("2026-09-21", BigDecimal.ONE, "2026-09", "Bankgiro via Plusgiro", "Kontoinnehavare A", null, null, null, "Sundsvall", "",
			"11111111", null, null, "123", null, null);
	}

	static LifecarePaymentRequest payment(final BigDecimal amount, final String paymentMethod, final String applicationMonth, final String accountNumber,
		final String accountingCode) {
		return new LifecarePaymentRequest("2026-09-21", amount, applicationMonth, paymentMethod, "Kontoinnehavare A", null, null, null, "Sundsvall", "",
			accountNumber, accountingCode, null, "123", null, List.of());
	}

	static String serialised(final JsonNode node) {
		return JSON.writeValueAsString(node);
	}
}
