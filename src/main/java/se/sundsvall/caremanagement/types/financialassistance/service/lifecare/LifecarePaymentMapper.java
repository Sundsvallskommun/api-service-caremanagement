package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentBalance;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentConcernMonth;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentMethod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentOptions;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentPosting;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentProposal;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentStatus;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRegisteredPayment;
import tools.jackson.databind.JsonNode;

import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.ADDRESS_PAYEE_ID;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.array;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.cancelled;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.decimal;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.digitsOnly;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.elements;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.field;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.flag;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.text;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.textOrEmpty;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentNodes.toMonth;

/**
 * Maps Lifecare's utbetalning answers onto careM's API models. The personnummer on Lifecare's rows is left behind.
 */
final class LifecarePaymentMapper {

	private static final Comparator<JsonNode> NEWEST_PAY_DATE_FIRST = Comparator.comparing((final JsonNode payment) -> textOrEmpty(payment, "payDate")).reversed();

	private LifecarePaymentMapper() {}

	/**
	 * Everything the utbetalning form needs, out of Lifecare's underlag for a new utbetalning.
	 *
	 * @param  underlag   Lifecare's GetPaymentForCreate answer
	 * @param  registered Lifecare's GetLatestPayments answer
	 * @return            the options
	 */
	static LifecarePaymentOptions toPaymentOptions(final JsonNode underlag, final JsonNode registered) {
		final var methods = array(underlag, "paymentMethods");
		return new LifecarePaymentOptions(
			methods.stream().filter(method -> flag(method, "inUse")).map(LifecarePaymentMapper::toPaymentMethod).toList(),
			array(underlag, "payees").stream().filter(payee -> flag(payee, "isActive")).map(payee -> toPayee(payee, methods)).toList(),
			array(field(underlag, "payment"), "postings").stream().map(LifecarePaymentMapper::toPosting).toList(),
			array(underlag, "balances").stream().map(LifecarePaymentMapper::toBalance).toList(),
			array(underlag, "paymentConcernMonths").stream()
				.map(month -> new LifecarePaymentConcernMonth(toMonth(text(month, "concernMonth")), text(month, "displayMonth")))
				.toList(),
			toPaymentProposal(underlag, registered));
	}

	/**
	 * A Lifecare payee row as the caseworker sees it. The Adress entry has no betalsätt text of its own, so the list of
	 * betalsätt names the code when it can.
	 *
	 * @param  payee   the payee row
	 * @param  methods the betalsätt on the insats
	 * @return         the payee
	 */
	static LifecarePayee toPayee(final JsonNode payee, final List<JsonNode> methods) {
		final var paymentMethodCode = integer(payee, "paymentMethod");
		final var paymentMethod = Optional.ofNullable(text(payee, "paymentMethodText"))
			.or(() -> methods.stream()
				.filter(method -> Objects.equals(integer(method, "paymentCode"), paymentMethodCode))
				.map(method -> text(method, "payment"))
				.filter(Objects::nonNull)
				.findFirst())
			.orElse("");
		final var id = integer(payee, "payeeId");
		return new LifecarePayee(
			id,
			Optional.ofNullable(text(payee, "payeeName")).or(() -> Optional.ofNullable(text(payee, "name"))).orElse(""),
			textOrEmpty(payee, "name"),
			paymentMethodCode,
			paymentMethod,
			textOrEmpty(payee, "clearing"),
			textOrEmpty(payee, "accountNumber"),
			textOrEmpty(payee, "streetAddress"),
			textOrEmpty(payee, "careOfAddress"),
			textOrEmpty(payee, "postalCode"),
			textOrEmpty(payee, "postalAddress"),
			Objects.equals(id, ADDRESS_PAYEE_ID));
	}

	/**
	 * The insats's utbetalningar, newest payment date first. The account number is left behind.
	 *
	 * @param  registered Lifecare's GetLatestPayments answer
	 * @return            the utbetalningar
	 */
	static List<LifecareRegisteredPayment> toRegisteredPayments(final JsonNode registered) {
		return elements(registered).stream()
			.sorted(NEWEST_PAY_DATE_FIRST)
			.map(payment -> new LifecareRegisteredPayment(
				integer(payment, "paymentId"),
				text(payment, "payDate"),
				toMonth(text(payment, "concernedMonth")),
				decimal(payment, "amount"),
				textOrEmpty(payment, "paymentMethodText"),
				textOrEmpty(payment, "name"),
				textOrEmpty(payment, "statusText"),
				cancelled(payment)))
			.toList();
	}

	/**
	 * The status for the application month: effectuated when a standing utbetalning on the insats concerns the month.
	 *
	 * @param  registered       Lifecare's GetLatestPayments answer
	 * @param  applicationMonth the month, yyyy-MM
	 * @return                  the status
	 */
	static LifecarePaymentStatus toPaymentStatus(final JsonNode registered, final String applicationMonth) {
		final var month = applicationMonth.replaceFirst("-", "");
		return latestStanding(elements(registered).stream().filter(payment -> month.equals(text(payment, "concernedMonth"))).toList())
			.map(payment -> new LifecarePaymentStatus(applicationMonth, true, text(payment, "payDate"), decimal(payment, "amount"), text(payment, "statusText"), false))
			.orElseGet(() -> new LifecarePaymentStatus(applicationMonth, false, null, null, null, false));
	}

	/**
	 * The form's starting point, out of Lifecare alone: its proposed date, its first open month, what is left on the
	 * saldon, and the payee the insats was last paid to.
	 */
	static LifecarePaymentProposal toPaymentProposal(final JsonNode underlag, final JsonNode registered) {
		final var remaining = array(underlag, "balances").stream()
			.map(balance -> decimal(balance, "balanceAmount"))
			.filter(Objects::nonNull)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		final var latestAccount = latestStanding(elements(registered))
			.map(payment -> digitsOnly(text(payment, "accountNumber")))
			.orElse("");
		Integer payeeId = null;
		if (!latestAccount.isEmpty()) {
			payeeId = array(underlag, "payees").stream()
				.filter(payee -> flag(payee, "isActive") && digitsOnly(text(payee, "accountNumber")).equals(latestAccount))
				.map(payee -> integer(payee, "payeeId"))
				.findFirst()
				.orElse(null);
		}
		final var payDate = field(field(underlag, "payment"), "payDate");
		String paymentDate = null;
		if (payDate != null && payDate.isString()) {
			paymentDate = payDate.asString();
		}
		final var concernedMonth = array(underlag, "paymentConcernMonths").stream()
			.findFirst()
			.map(month -> toMonth(text(month, "concernMonth")))
			.orElse(null);
		BigDecimal amount = null;
		if (remaining.signum() > 0) {
			amount = remaining;
		}
		return new LifecarePaymentProposal(paymentDate, concernedMonth, amount, payeeId);
	}

	/** The most recent utbetalning that still stands (not makulerad). */
	private static Optional<JsonNode> latestStanding(final List<JsonNode> registered) {
		return registered.stream()
			.filter(payment -> !cancelled(payment))
			.sorted(NEWEST_PAY_DATE_FIRST)
			.findFirst();
	}

	private static LifecarePaymentMethod toPaymentMethod(final JsonNode method) {
		return new LifecarePaymentMethod(integer(method, "paymentCode"), text(method, "payment"), flag(method, "localNumberEnabled"), flag(method, "localNumberMandatory"));
	}

	private static LifecarePaymentPosting toPosting(final JsonNode posting) {
		return new LifecarePaymentPosting(integer(posting, "purpose"), Optional.ofNullable(text(posting, "purposeText")).orElseGet(() -> textOrEmpty(posting, "purpose")));
	}

	private static LifecarePaymentBalance toBalance(final JsonNode balance) {
		return new LifecarePaymentBalance(textOrEmpty(balance, "name").trim(), decimal(balance, "approvedAmount"), decimal(balance, "bookedAmount"), decimal(balance, "balanceAmount"));
	}
}
