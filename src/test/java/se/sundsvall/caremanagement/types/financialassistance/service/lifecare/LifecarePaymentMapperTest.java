package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePayee;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentBalance;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentConcernMonth;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentMethod;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentPosting;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentProposal;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecarePaymentStatus;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareRegisteredPayment;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.JSON;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentFixtures.underlag;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentMapper.toPayee;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentMapper.toPaymentOptions;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentMapper.toPaymentProposal;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentMapper.toPaymentStatus;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecarePaymentMapper.toRegisteredPayments;

class LifecarePaymentMapperTest {

	/** The insats's utbetalningar: the latest standing one went to Konto A, a newer one to Konto B is makulerad. */
	private static final String REGISTERED = """
		[
		  { "paymentId": 4, "amount": 1.0, "payDate": "2026-09-21", "concernedMonth": "202609", "paymentMethodText": "Bankgiro via Plusgiro",
		    "name": "Kontoinnehavare A", "accountNumber": "11111111", "statusText": "Utbetald", "cancellationDate": "" },
		  { "paymentId": 5, "amount": 2.0, "payDate": "2026-09-22", "concernedMonth": "202609", "accountNumber": "22222222", "cancellationDate": "2026-09-22" },
		  { "paymentId": 3, "amount": 3.0, "payDate": "2026-08-21", "concernedMonth": "202608", "accountNumber": "11111111", "cancellationDate": "" }
		]
		""";

	@Test
	void listsTheBetalsattInUseAndTheActivePayeesWithoutThePersonnummer() throws Exception {
		final var options = toPaymentOptions(underlag(), json(REGISTERED));

		assertThat(options.paymentMethods()).containsExactly(new LifecarePaymentMethod(14, "Bankgiro via Plusgiro", false, false));
		assertThat(options.payees()).extracting(LifecarePayee::id).containsExactly(2147483646, 2);
		assertThat(options.payees().getFirst().toRegisteredAddress()).isTrue();
		assertThat(options.payees().getFirst().paymentMethod()).isEmpty();
		assertThat(options.payees().get(1)).isEqualTo(new LifecarePayee(2, "Konto A", "Kontoinnehavare A", 14, "Bankgiro via Plusgiro", "", "11111111", "", "", "",
			"Sundsvall", false));
		assertThat(options.postings()).containsExactly(new LifecarePaymentPosting(1, "Försörjningsstöd exklusive tillfälligt boende"));
		assertThat(options.balances()).containsExactly(new LifecarePaymentBalance("Ek. Bistånd 3,00", new BigDecimal("4.0"), new BigDecimal("1.0"), new BigDecimal("3.0")));
		assertThat(options.concernMonths()).containsExactly(new LifecarePaymentConcernMonth("2026-09", "September 2026"));
		assertThat(JSON.writeValueAsString(options)).doesNotContain("19800101T001").doesNotContain("800101-T001");
	}

	@Test
	void proposesTheNextUtbetalningFromLifecareAlone() {
		final var proposal = toPaymentProposal(underlag(), json(REGISTERED));

		// The makulerad utbetalning to Konto B is newer but no longer stands, so Konto A is proposed.
		assertThat(proposal).isEqualTo(new LifecarePaymentProposal("2026-09-21", "2026-09", new BigDecimal("3.0"), 2));
	}

	@Test
	void proposesNothingLifecareDoesNotHave() {
		final var bare = json("""
			{ "payment": { "payDate": null }, "payees": [], "paymentMethods": [], "balances": [ { "balanceAmount": 0 } ] }
			""");

		assertThat(toPaymentProposal(bare, json("[]"))).isEqualTo(new LifecarePaymentProposal(null, null, null, null));
	}

	@Test
	void proposesNoPayeeWhenTheLatestAccountIsNoLongerAnActivePayee() {
		final var registered = json("""
			[ { "paymentId": 4, "amount": 1.0, "payDate": "2026-09-21", "concernedMonth": "202609", "accountNumber": "99999999", "cancellationDate": "" } ]
			""");

		assertThat(toPaymentProposal(underlag(), registered).payeeId()).isNull();
	}

	@Test
	void namesTheBetalsattFromTheListWhenThePayeeHasNoText() {
		final var payee = json("""
			{ "payeeId": 7, "payeeName": null, "name": "Kontoinnehavare C", "paymentMethod": 14, "paymentMethodText": null, "isActive": true }
			""");
		final var methods = List.of(json("{ \"paymentCode\": 14, \"payment\": \"Bankgiro via Plusgiro\" }"));

		final var result = toPayee(payee, methods);

		assertThat(result.paymentMethod()).isEqualTo("Bankgiro via Plusgiro");
		assertThat(result.label()).isEqualTo("Kontoinnehavare C");
		assertThat(result.accountNumber()).isEmpty();
		assertThat(toPayee(json("{ \"payeeId\": 8 }"), methods).label()).isEmpty();
	}

	@Test
	void listsTheRegisteredPaymentsNewestFirstWithoutTheAccount() throws Exception {
		final var payments = toRegisteredPayments(json(REGISTERED));

		assertThat(payments).containsExactly(
			new LifecareRegisteredPayment(5, "2026-09-22", "2026-09", new BigDecimal("2.0"), "", "", "", true),
			new LifecareRegisteredPayment(4, "2026-09-21", "2026-09", new BigDecimal("1.0"), "Bankgiro via Plusgiro", "Kontoinnehavare A", "Utbetald", false),
			new LifecareRegisteredPayment(3, "2026-08-21", "2026-08", new BigDecimal("3.0"), "", "", "", false));
		assertThat(JSON.writeValueAsString(payments)).doesNotContain("11111111");
		assertThat(toRegisteredPayments(json("{}"))).isEmpty();
	}

	@Test
	void readsTheStatusForTheApplicationMonth() {
		assertThat(toPaymentStatus(json(REGISTERED), "2026-09"))
			.isEqualTo(new LifecarePaymentStatus("2026-09", true, "2026-09-21", new BigDecimal("1.0"), "Utbetald", false));
		assertThat(toPaymentStatus(json(REGISTERED), "2026-07"))
			.isEqualTo(new LifecarePaymentStatus("2026-07", false, null, null, null, false));
	}

	@Test
	void labelsAPostingWithoutTextByItsPurpose() {
		final var underlag = underlag();
		((ObjectNode) underlag.get("payment").get("postings").get(0)).putNull("purposeText");

		assertThat(toPaymentOptions(underlag, json("[]")).postings()).containsExactly(new LifecarePaymentPosting(1, "1"));
	}
}
