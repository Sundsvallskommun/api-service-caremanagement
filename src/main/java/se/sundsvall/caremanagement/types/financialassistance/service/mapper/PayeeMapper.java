package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.util.Locale;
import se.sundsvall.caremanagement.lifecare.service.model.PaymentView;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeOption;
import se.sundsvall.caremanagement.types.financialassistance.api.model.PayeeRequest;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPayeeEntity;

import static java.util.Optional.ofNullable;

/**
 * Maps between the payee option the payment form sees and its two sources — the applicant's Lifecare payment history
 * and the errand's manually added rows.
 */
public final class PayeeMapper {

	public static final String SOURCE_LIFECARE = "LIFECARE";
	public static final String SOURCE_MANUAL = "MANUAL";

	private PayeeMapper() {}

	/** A LIFECARE option: derived from a payment, so it has no id and no Lifecare status — it is in Lifecare already. */
	public static PayeeOption toPayeeOption(final PaymentView view) {
		return PayeeOption.create()
			.withName(view.name())
			.withPaymentMethod(view.paymentMethod())
			.withClearing(view.clearing())
			.withAccountNumber(view.accountNumber())
			.withSource(SOURCE_LIFECARE)
			.withLastPaidOn(view.payDate());
	}

	/** A MANUAL option: a stored row on the errand, carrying the state of its creation in Lifecare. */
	public static PayeeOption toPayeeOption(final FaPayeeEntity entity) {
		return PayeeOption.create()
			.withId(entity.getId())
			.withName(entity.getName())
			.withPaymentMethod(entity.getPaymentMethod())
			.withClearing(entity.getClearing())
			.withAccountNumber(entity.getAccountNumber())
			.withSource(SOURCE_MANUAL)
			.withLifecareStatus(entity.getLifecareStatus())
			.withLifecarePayeeId(entity.getLifecarePayeeId())
			.withLifecareDetail(entity.getLifecareDetail())
			.withCreated(entity.getCreated());
	}

	/** Copy the caseworker-editable fields onto an entity. Provenance and Lifecare state are never touched here. */
	public static FaPayeeEntity applyRequest(final FaPayeeEntity entity, final PayeeRequest request) {
		return entity
			.withName(request.getName())
			.withPaymentMethod(request.getPaymentMethod())
			.withClearing(request.getClearing())
			.withAccountNumber(request.getAccountNumber());
	}

	/**
	 * The identity of a payee for de-duplication: the four fields that make it a distinct account to pay to, trimmed
	 * and case-folded so "ANNA ANDERSSON" and "Anna Andersson " are not offered twice. This is what collapses a manually
	 * added payee into its LIFECARE twin once it has been put into Lifecare and a payment has gone to it.
	 */
	public static PayeeKey key(final PayeeOption option) {
		return key(option.getName(), option.getPaymentMethod(), option.getClearing(), option.getAccountNumber());
	}

	/**
	 * The same identity for the payee a finalize names on a payment, so a decided payment can be matched against the
	 * payees stored on the errand.
	 */
	public static PayeeKey key(final Payee payee) {
		return key(payee.getName(), payee.getPaymentMethod(), payee.getClearing(), payee.getAccountNumber());
	}

	private static PayeeKey key(final String name, final String paymentMethod, final String clearing, final String accountNumber) {
		return new PayeeKey(normalise(name), normalise(paymentMethod), normalise(clearing), normalise(accountNumber));
	}

	/** The de-duplication identity of a payee — see {@link #key(PayeeOption)}. */
	public record PayeeKey(String name, String paymentMethod, String clearing, String accountNumber) {
	}

	private static String normalise(final String value) {
		return ofNullable(value).map(String::trim).map(text -> text.toLowerCase(Locale.ROOT)).orElse("");
	}
}
