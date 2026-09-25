package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.ADDRESS_PAYEE_ID;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.NODES;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.array;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.cancelled;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.copyOf;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.decimal;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.digitsOnly;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.field;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.flag;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.plain;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.refuse;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.textOrEmpty;

/**
 * Builds the bodies careM sends to Lifecare's Payment/Create and Payee/Create, and recognises what Lifecare already
 * holds so neither is sent twice.
 *
 * <p>
 * Payment/Create is not idempotent and moves money, so anything that cannot be decided safely stops the utbetalning
 * with a 422 and a reason instead of being guessed.
 * </p>
 */
final class LifecarePaymentBodies {

	/** How many message rows a Lifecare utbetalning has (messageRow1 to messageRow7). */
	private static final int MESSAGE_ROWS = 7;

	private LifecarePaymentBodies() {}

	/**
	 * Builds the Payment/Create body from Lifecare's underlag and the caseworker's utbetalning, the way Lifecare's web
	 * app does it (capture 2026-09-21): the underlag's own payment object in the same field order, with the choices
	 * filled in, the chosen payee's details copied flat, aktualiseringId taken out and creditAccount and simpleAccount
	 * added. With several konteringsrader every one of them goes back, the amount on the chosen ändamål and 0 on the
	 * others (capture 2026-09-23).
	 *
	 * <p>
	 * Refused (422) when: no amount, a betalsätt or month Lifecare does not offer, no ändamål picked among several
	 * konteringsrader, other than exactly one saldo, a saldo that does not cover the amount, a blocking maximum amount,
	 * or a payee Lifecare does not have.
	 * </p>
	 *
	 * @param  underlag Lifecare's GetPaymentForCreate answer
	 * @param  request  the caseworker's utbetalning
	 * @return          the body to post
	 */
	static ObjectNode buildPaymentCreate(final JsonNode underlag, final LifecarePaymentRequest request) {
		final var amount = request.amount();
		if (amount == null || amount.signum() <= 0) {
			throw refuse("Utbetalningen saknar belopp.");
		}

		final var method = array(underlag, "paymentMethods").stream()
			.filter(candidate -> flag(candidate, "inUse") && sameName(text(candidate, "payment"), request.paymentMethod()))
			.findFirst()
			.orElseThrow(() -> refuse("Betalsättet \"%s\" finns inte på insatsen i Lifecare.".formatted(Objects.toString(request.paymentMethod(), ""))));

		final var concernedMonth = toConcernMonth(request.applicationMonth());
		if (array(underlag, "paymentConcernMonths").stream().noneMatch(month -> concernedMonth.equals(text(month, "concernMonth")))) {
			throw refuse("Lifecare tar inte emot utbetalningar för månaden %s.".formatted(Objects.toString(request.applicationMonth(), "")));
		}

		final var payment = field(underlag, "payment");
		final var postings = array(payment, "postings");
		final var purpose = choosePurpose(postings, request.accountingCode());

		final var balance = pickBalance(array(underlag, "balances"));
		final var balanceAmount = Optional.ofNullable(decimal(balance, "balanceAmount")).orElse(BigDecimal.ZERO);
		if (balanceAmount.compareTo(amount) < 0) {
			throw refuse("Saldot i Lifecare (%s kr) räcker inte till %s kr. Finns beslutet registrerat där?".formatted(plain(balanceAmount), plain(amount)));
		}

		final var maxAmount = decimal(payment, "maxAmountForPayment");
		if (maxAmount != null && amount.compareTo(maxAmount) > 0 && !flag(payment, "notificationOnlyOfMaxAmountForPaymentActivated")) {
			throw refuse("Beloppet är över Lifecares maxbelopp för en utbetalning (%s kr).".formatted(plain(maxAmount)));
		}

		final var paymentCode = integer(method, "paymentCode");
		if (!payeeIsInLifecare(underlag, request, paymentCode)) {
			throw refuse("Mottagaren finns inte i Lifecare. Lägg till den där först.");
		}
		// A payment that is not an object has no postings, so it has been refused above.
		return fill(copyOf(payment), request, amount, paymentCode, concernedMonth, purpose, balance, postings);
	}

	private static ObjectNode fill(final ObjectNode body, final LifecarePaymentRequest request, final BigDecimal amount, final Integer paymentCode,
		final String concernedMonth, final String purpose, final JsonNode balance, final List<JsonNode> postings) {

		// Setting a field the underlag has keeps it where the underlag had it; only the added fields go last.
		body.put("amount", amount);
		body.put("paymentMethod", paymentCode);
		if (request.paymentDate() != null) {
			body.put("payDate", request.paymentDate());
		}
		body.put("concernedMonth", concernedMonth);
		body.put("clearing", Objects.toString(request.clearingNumber(), ""));
		body.put("accountNumber", Objects.toString(request.accountNumber(), ""));
		body.put("name", Objects.toString(request.payeeName(), ""));
		body.put("streetAddress", Objects.toString(request.payeeAddress(), ""));
		body.put("careOfAddress", Objects.toString(request.payeeCareOf(), ""));
		body.put("postalCode", Objects.toString(request.payeeZipCode(), ""));
		body.put("postalAddress", Objects.toString(request.payeeCity(), ""));
		body.put("billingNumber", request.invoiceNumber());
		body.put("localNumber", Objects.toString(request.localPaymentNumber(), ""));
		body.put("ocrCheck", Boolean.TRUE.equals(request.usesOcr()));
		final var messageLines = Optional.ofNullable(request.messageLines()).orElse(List.of());
		for (var row = 1; row <= MESSAGE_ROWS; row++) {
			body.put("messageRow" + row, messageLine(messageLines, row - 1));
		}

		final var balanceId = Optional.ofNullable(field(balance, "ownerId")).map(JsonNode::deepCopy).orElse(NODES.nullNode());
		final var balanceCopy = copyOf(balance);
		balanceCopy.set("id", balanceId);
		body.set("balance", balanceCopy);

		final var persons = NODES.arrayNode();
		array(body, "paymentPersons").forEach(person -> {
			final var copy = copyOf(person);
			copy.put("personIdAndName", textOrEmpty(person, "personIdFormatted") + " " + textOrEmpty(person, "name"));
			persons.add(copy);
		});
		body.set("paymentPersons", persons);

		// Every konteringsrad goes back, in the underlag's order: the whole amount on the chosen ändamål, 0 on the rest.
		final var postingRows = NODES.arrayNode();
		postings.forEach(posting -> {
			final var copy = copyOf(posting);
			if (purpose.equals(text(posting, "purpose"))) {
				copy.put("amount", amount);
			} else {
				copy.put("amount", 0);
			}
			postingRows.add(copy);
		});
		body.set("postings", postingRows);

		body.set("balanceId", balanceId.deepCopy());
		body.remove("aktualiseringId");
		body.put("creditAccount", "");
		body.put("simpleAccount", "");
		return body;
	}

	/**
	 * The utbetalning already on the insats that the body would duplicate: not makulerad, and the same amount, month,
	 * payment date and account. This is what makes a retry after an unanswered call safe.
	 *
	 * @param  registered Lifecare's GetLatestPayments answer
	 * @param  body       the Payment/Create body about to be sent
	 * @return            the duplicate, if there is one
	 */
	static Optional<JsonNode> findRegisteredPayment(final JsonNode registered, final JsonNode body) {
		final var amount = decimal(body, "amount");
		final var account = digitsOnly(text(body, "accountNumber"));
		return LifecarePaymentNodes.elements(registered).stream()
			.filter(payment -> !cancelled(payment))
			.filter(payment -> sameAmount(decimal(payment, "amount"), amount))
			.filter(payment -> Objects.equals(text(payment, "concernedMonth"), text(body, "concernedMonth")))
			.filter(payment -> Objects.equals(text(payment, "payDate"), text(body, "payDate")))
			.filter(payment -> digitsOnly(text(payment, "accountNumber")).equals(account))
			.findFirst();
	}

	/**
	 * An active payee already paying to the same account and clearing with the same betalsätt. Payee/Create is not
	 * idempotent, so a payee the caseworker re-enters is looked up here instead of being created twice. Account numbers
	 * are compared on their digits, since the same account is written with and without dashes.
	 *
	 * @param  payees    the payees in Lifecare's underlag
	 * @param  candidate the payee to add
	 * @return           the existing payee, if there is one
	 */
	static Optional<JsonNode> findMatchingPayee(final List<JsonNode> payees, final LifecarePayeeRequest candidate) {
		final var accountNumber = digitsOnly(candidate.accountNumber());
		if (accountNumber.isEmpty()) {
			return Optional.empty();
		}
		return payees.stream()
			.filter(payee -> flag(payee, "isActive"))
			.filter(payee -> !Objects.equals(integer(payee, "payeeId"), ADDRESS_PAYEE_ID))
			.filter(payee -> Objects.equals(integer(payee, "paymentMethod"), candidate.paymentMethod()))
			.filter(payee -> digitsOnly(text(payee, "accountNumber")).equals(accountNumber))
			.filter(payee -> digitsOnly(text(payee, "clearing")).equals(digitsOnly(candidate.clearing())))
			.findFirst();
	}

	/**
	 * The Payee/Create body, field for field as Lifecare's web app sends it (capture 2026-09-22), including its mix of
	 * null and empty strings, which Lifecare tells apart.
	 *
	 * @param  personId the personnummer the payee is filed under, from Lifecare's own underlag
	 * @param  payee    the payee to add
	 * @return          the body to post
	 */
	static ObjectNode buildPayeeCreate(final String personId, final LifecarePayeeRequest payee) {
		final var name = payee.name().trim();
		final var body = NODES.objectNode();
		body.put("payeeId", 0);
		body.put("payeeName", Optional.ofNullable(payee.payeeName()).map(String::trim).filter(StringUtils::hasText).orElse(name));
		body.put("personId", personId);
		body.put("paymentMethod", payee.paymentMethod());
		body.putNull("paymentMethodText");
		body.put("accountNumber", Optional.ofNullable(payee.accountNumber()).map(String::trim).orElse(""));
		body.putNull("memorialAccountNumber");
		body.put("clearing", Optional.ofNullable(payee.clearing()).map(String::trim).orElse(""));
		body.put("name", name);
		body.putNull("streetAddress");
		body.putNull("careOfAddress");
		body.put("postalCode", "");
		body.put("postalAddress", "");
		body.put("ocrCheck", false);
		body.put("addressFromPerson", false);
		body.put("updateTimestamp", "");
		body.putNull("updateSignature");
		body.put("isActive", true);
		body.putNull("statusText");
		return body;
	}

	/** careM's yyyy-MM month as Lifecare's yyyyMM. */
	static String toConcernMonth(final String applicationMonth) {
		final var month = Objects.toString(applicationMonth, "").replaceFirst("-", "");
		return month.substring(0, Math.min(6, month.length()));
	}

	/**
	 * The payee must already be in Lifecare: Payment/Create copies the account across rather than pointing at a payee,
	 * so a payee Lifecare lacks would be invented on the spot. Without an account only Lifecare's Adress entry pays.
	 */
	private static boolean payeeIsInLifecare(final JsonNode underlag, final LifecarePaymentRequest request, final Integer paymentCode) {
		final var payees = array(underlag, "payees");
		final var accountNumber = digitsOnly(request.accountNumber());
		if (accountNumber.isEmpty()) {
			return payees.stream()
				.anyMatch(payee -> Objects.equals(integer(payee, "payeeId"), ADDRESS_PAYEE_ID) && sameName(text(payee, "payeeName"), request.payeeName()));
		}
		return payees.stream()
			.filter(payee -> flag(payee, "isActive"))
			.filter(payee -> Objects.equals(integer(payee, "paymentMethod"), paymentCode))
			.filter(payee -> digitsOnly(text(payee, "accountNumber")).equals(accountNumber))
			.anyMatch(payee -> digitsOnly(text(payee, "clearing")).equals(digitsOnly(request.clearingNumber())));
	}

	/**
	 * The ändamål (Lifecare purpose) the amount is booked on, as its text. An insats with a single konteringsrad needs
	 * no pick; several and no pick, or a pick the insats does not have, stops the utbetalning.
	 */
	private static String choosePurpose(final List<JsonNode> postings, final String accountingCode) {
		if (postings.isEmpty()) {
			throw refuse("Insatsen har ingen konteringsrad i Lifecare.");
		}
		if (!StringUtils.hasLength(accountingCode)) {
			if (postings.size() == 1) {
				return textOrEmpty(postings.getFirst(), "purpose");
			}
			throw refuse("Välj ändamål (kontering) för utbetalningen.");
		}
		final var code = accountingCode.trim();
		return postings.stream()
			.map(posting -> textOrEmpty(posting, "purpose"))
			.filter(code::equals)
			.findFirst()
			.orElseThrow(() -> refuse("Ändamålet \"%s\" finns inte bland insatsens konteringsrader i Lifecare.".formatted(accountingCode)));
	}

	/**
	 * The saldo the utbetalning is booked against. Its id goes back as the saldo's ownerId; with one saldo in the capture
	 * both its row and its ownerId were 1, so an insats with several saldon is refused until that is verified.
	 */
	private static JsonNode pickBalance(final List<JsonNode> balances) {
		if (balances.isEmpty()) {
			throw refuse("Insatsen har inget saldo i Lifecare. Finns beslutet registrerat där?");
		}
		if (balances.size() > 1) {
			throw refuse("Insatsen har flera saldon i Lifecare, och det stöds inte än.");
		}
		return balances.getFirst();
	}

	private static String messageLine(final List<String> messageLines, final int index) {
		if (index < messageLines.size()) {
			return Objects.toString(messageLines.get(index), "");
		}
		return "";
	}

	private static boolean sameName(final String first, final String second) {
		return Objects.toString(first, "").trim().equalsIgnoreCase(Objects.toString(second, "").trim());
	}

	private static boolean sameAmount(final BigDecimal first, final BigDecimal second) {
		if (first == null || second == null) {
			return false;
		}
		return first.compareTo(second) == 0;
	}
}
