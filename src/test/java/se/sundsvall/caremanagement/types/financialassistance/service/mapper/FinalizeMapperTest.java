package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.sundsvall.caremanagement.rpa.service.RpaAction;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CommunicationChannels;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizePayment;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
import se.sundsvall.caremanagement.types.financialassistance.api.model.Payee;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FinalizeMapperTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 6, 18);

	private static FinalizeRequest grantingRequest() {
		return FinalizeRequest.create()
			.withDecision(FinalizeDecision.create()
				.withOutcome("BIFALL")
				.withReason("Inkomster enligt SSBTEK")
				.withCoApplicantReason("Beviljad")
				.withPeriodFrom(LocalDate.of(2026, 6, 1))
				.withPeriodTo(LocalDate.of(2026, 6, 30))
				.withAmount(new BigDecimal("7900.00"))
				.withDecisionMessage("Du beviljas ekonomiskt bistånd"))
			.withCommunication(CommunicationChannels.create().withMinaSidor(true).withDigitalMailbox(true).withLetter(false))
			.withHouseholdSizeChanged(true);
	}

	@Test
	void toPaymentDecisionMapsEverything() {
		final var decision = FinalizeMapper.toPaymentDecision(grantingRequest(), "jane02doe", TODAY);

		assertThat(decision).hasNoNullFieldsOrPropertiesExcept("id", "created", "lifecareId", "lifecareDetail");
		assertThat(decision.getDecisionType()).isEqualTo("PAYMENT");
		assertThat(decision.getValue()).isEqualTo("BIFALL");
		assertThat(decision.getDescription()).isEqualTo("Inkomster enligt SSBTEK");
		assertThat(decision.getAmount()).isEqualByComparingTo("7900.00");
		assertThat(decision.getDecisionMessage()).isEqualTo("Du beviljas ekonomiskt bistånd");
		assertThat(decision.getDecisionDate()).isEqualTo(TODAY);
		assertThat(decision.getPeriodFrom()).isEqualTo(LocalDate.of(2026, 6, 1));
		assertThat(decision.getPeriodTo()).isEqualTo(LocalDate.of(2026, 6, 30));
		assertThat(decision.getCreatedBy()).isEqualTo("jane02doe");
		assertThat(decision.getLifecareStatus()).isEqualTo("PENDING");
		assertThat(decision.getCoApplicantReason()).isEqualTo("Beviljad");
	}

	@ParameterizedTest
	@MethodSource("effectiveAmountArguments")
	void effectiveAmount(final String outcome, final BigDecimal requested, final BigDecimal expected) {
		final var decision = FinalizeDecision.create().withOutcome(outcome).withAmount(requested);

		assertThat(FinalizeMapper.effectiveAmount(decision)).isEqualByComparingTo(expected);
		assertThat(FinalizeMapper.toPaymentDecision(FinalizeRequest.create().withDecision(decision), "x", TODAY).getAmount()).isEqualByComparingTo(expected);
	}

	private static Stream<Arguments> effectiveAmountArguments() {
		return Stream.of(
			Arguments.of("BIFALL", new BigDecimal("7900"), new BigDecimal("7900")),
			Arguments.of("DELAVSLAG", new BigDecimal("3000"), new BigDecimal("3000")),
			Arguments.of("BIFALL", null, BigDecimal.ZERO),
			Arguments.of("AVSLAG", new BigDecimal("500"), BigDecimal.ZERO),
			Arguments.of("AVSLAG", null, BigDecimal.ZERO));
	}

	@Test
	void toPaymentDecisionIsNullSafe() {
		assertThat(FinalizeMapper.toPaymentDecision(null, "x", TODAY)).isNull();
		assertThat(FinalizeMapper.toPaymentDecision(FinalizeRequest.create(), "x", TODAY)).isNull();
	}

	@Test
	void updateEntityStampsChannelsAndHouseholdFlag() {
		final var entity = FinancialAssistanceEntity.create().withErrandId("errand-1");

		final var result = FinalizeMapper.updateEntity(entity, grantingRequest());

		assertThat(result).isSameAs(entity)
			.returns(true, FinancialAssistanceEntity::getHouseholdSizeChanged)
			.returns(true, FinancialAssistanceEntity::getNotifyMinaSidor)
			.returns(true, FinancialAssistanceEntity::getNotifyDigitalMailbox)
			.returns(false, FinancialAssistanceEntity::getNotifyLetter);
	}

	@Test
	void updateEntityDefaultsMissingFlagsToFalse() {
		final var result = FinalizeMapper.updateEntity(FinancialAssistanceEntity.create(), FinalizeRequest.create());

		assertThat(result)
			.returns(false, FinancialAssistanceEntity::getHouseholdSizeChanged)
			.returns(false, FinancialAssistanceEntity::getNotifyMinaSidor)
			.returns(false, FinancialAssistanceEntity::getNotifyDigitalMailbox)
			.returns(false, FinancialAssistanceEntity::getNotifyLetter);
		assertThat(FinalizeMapper.updateEntity(null, grantingRequest())).isNull();
	}

	@Test
	void toDecisionContentCarriesTheDecisionAndTheChoices() {
		final var content = FinalizeMapper.toDecisionContent(grantingRequest(), "decision-1");

		assertThat(content).containsOnly(
			java.util.Map.entry("decisionId", "decision-1"),
			java.util.Map.entry("outcome", "BIFALL"),
			java.util.Map.entry("reason", "Inkomster enligt SSBTEK"),
			java.util.Map.entry("coApplicantReason", "Beviljad"),
			java.util.Map.entry("periodFrom", "2026-06-01"),
			java.util.Map.entry("periodTo", "2026-06-30"),
			java.util.Map.entry("amount", "7900.00"),
			java.util.Map.entry("communicationChannels", "MINA_SIDOR,DIGITAL_MAILBOX"),
			java.util.Map.entry("householdSizeChanged", "true"));
	}

	@Test
	void toDecisionContentLeavesNullsOut() {
		final var request = FinalizeRequest.create().withDecision(FinalizeDecision.create().withOutcome("AVSLAG"));

		assertThat(FinalizeMapper.toDecisionContent(request, null)).containsOnly(
			java.util.Map.entry("outcome", "AVSLAG"),
			java.util.Map.entry("amount", "0"),
			java.util.Map.entry("communicationChannels", ""),
			java.util.Map.entry("householdSizeChanged", "false"));
		assertThat(FinalizeMapper.toDecisionContent(null, "decision-1")).containsOnly(java.util.Map.entry("decisionId", "decision-1"));
	}

	@Test
	void toPaymentIdContentCarriesNothingButTheId() {
		// The whole point of the change: the payee's name, clearing and account number must not reach the Orchestrator
		// queue store. The robot reads them through GET .../payments/{paymentId}.
		assertThat(FinalizeMapper.toPaymentIdContent("p-1")).containsOnly(java.util.Map.entry("paymentId", "p-1"));
		assertThat(FinalizeMapper.toPaymentIdContent(null)).isEmpty();
	}

	@Test
	void toLegacyPaymentContentCarriesTheOldFieldsBesideThePaymentId() {
		// The bridge form. It writes the payee's bank details to the queue - that is the cost of keeping an
		// unreleased robot working, and why the flag defaults off once the robot is over.
		final var payment = FinalizePayment.create()
			.withPaymentDate(LocalDate.of(2026, 6, 25))
			.withAmount(new BigDecimal("6000.00"))
			.withConcernedMonth("2026-06")
			.withAccountingCode("5011")
			.withPayee(Payee.create().withName("Hyresvärden AB").withPaymentMethod("BANKGIRO").withClearing("6000").withAccountNumber("123-4567"));

		assertThat(FinalizeMapper.toLegacyPaymentContent("p-1", payment, 2)).containsOnly(
			java.util.Map.entry("paymentId", "p-1"),
			java.util.Map.entry("sequence", "2"),
			java.util.Map.entry("paymentDate", "2026-06-25"),
			java.util.Map.entry("amount", "6000.00"),
			java.util.Map.entry("concernedMonth", "2026-06"),
			java.util.Map.entry("accountingCode", "5011"),
			java.util.Map.entry("payeeName", "Hyresvärden AB"),
			java.util.Map.entry("paymentMethod", "BANKGIRO"),
			java.util.Map.entry("clearing", "6000"),
			java.util.Map.entry("accountNumber", "123-4567"));
	}

	@Test
	void toLegacyPaymentContentStillCarriesThePaymentIdWithNoPayment() {
		assertThat(FinalizeMapper.toLegacyPaymentContent("p-1", null, 1))
			.containsOnly(java.util.Map.entry("paymentId", "p-1"), java.util.Map.entry("sequence", "1"));
	}

	@Test
	void toPaymentRequestBridgesTheTwoModelsNaming() {
		final var payment = FinalizePayment.create()
			.withPaymentDate(LocalDate.of(2026, 6, 25))
			.withAmount(new BigDecimal("6000.00"))
			.withConcernedMonth("2026-06")
			.withAccountingCode("5011")
			.withPayee(Payee.create().withId("a1b2c3d4-0000-0000-0000-000000000001").withName("Hyresvärden AB").withPaymentMethod("BANKGIRO").withClearing("6000").withAccountNumber("123-4567"));

		final var request = FinalizeMapper.toPaymentRequest(payment);

		assertThat(request.getPaymentDate()).isEqualTo(LocalDate.of(2026, 6, 25));
		assertThat(request.getAmount()).isEqualByComparingTo("6000.00");
		assertThat(request.getApplicationMonth()).isEqualTo("2026-06"); // concernedMonth → applicationMonth
		assertThat(request.getAccountingCode()).isEqualTo("5011");
		assertThat(request.getPayeeName()).isEqualTo("Hyresvärden AB");
		assertThat(request.getPaymentMethod()).isEqualTo("BANKGIRO");
		assertThat(request.getClearingNumber()).isEqualTo("6000"); // clearing → clearingNumber
		assertThat(request.getAccountNumber()).isEqualTo("123-4567");
		// The link to the payee row, and through it to lifecarePayeeId - without it the REGISTER_PAYMENT robot is left
		// matching the payee in Lifecare on name and account number.
		assertThat(request.getPayeeId()).isEqualTo("a1b2c3d4-0000-0000-0000-000000000001");
	}

	@Test
	void toPaymentRequestLeavesPayeeIdNullForALifecareDerivedPayee() {
		// A payee taken from the 12-month Lifecare history has no row on the errand, so the list serves it with a null id.
		final var payment = FinalizePayment.create()
			.withPayee(Payee.create().withPaymentMethod("BANKGIRO"));

		assertThat(FinalizeMapper.toPaymentRequest(payment).getPayeeId()).isNull();
	}

	@Test
	void toPaymentRequestCarriesAPayeePickedStraightFromLifecare() {
		// Draken reads the payees from Lifecare, so the picked payee has no row here - only its Lifecare id, its address,
		// and the payment's räkningsnummer / lokalbetalningsnummer, all of which Lifecare needs to register the payment.
		final var payment = FinalizePayment.create()
			.withLocalPaymentNumber("4711")
			.withInvoiceNumber("2026-00417")
			.withPayee(Payee.create()
				.withLifecarePayeeId("1234567")
				.withName("Hyresvärden AB")
				.withPaymentMethod("BANKGIRO")
				.withAddress("Storgatan 1")
				.withCareOf("c/o Bertil Bertilsson")
				.withZipCode("85230")
				.withCity("Sundsvall"));

		final var request = FinalizeMapper.toPaymentRequest(payment);

		assertThat(request.getPayeeId()).isNull();
		assertThat(request.getLifecarePayeeId()).isEqualTo("1234567");
		assertThat(request.getPayeeAddress()).isEqualTo("Storgatan 1");
		assertThat(request.getPayeeCareOf()).isEqualTo("c/o Bertil Bertilsson");
		assertThat(request.getPayeeZipCode()).isEqualTo("85230");
		assertThat(request.getPayeeCity()).isEqualTo("Sundsvall");
		assertThat(request.getLocalPaymentNumber()).isEqualTo("4711");
		assertThat(request.getInvoiceNumber()).isEqualTo("2026-00417");
	}

	@Test
	void toPaymentRequestIsNullSafe() {
		assertThat(FinalizeMapper.toPaymentRequest(null).getAmount()).isNull();
		assertThat(FinalizeMapper.toPaymentRequest(FinalizePayment.create()).getPayeeName()).isNull();
	}

	@Test
	void toIdListContentJoinsIdsAndCounts() {
		assertThat(FinalizeMapper.toIdListContent("monitoringIds", List.of("m-1", "m-2")))
			.containsOnly(java.util.Map.entry("monitoringIds", "m-1,m-2"), java.util.Map.entry("count", "2"));
		assertThat(FinalizeMapper.toIdListContent("documentIds", null))
			.containsOnly(java.util.Map.entry("documentIds", ""), java.util.Map.entry("count", "0"));
	}

	@ParameterizedTest
	@MethodSource("channelListArguments")
	void toChannelList(final CommunicationChannels channels, final String expected) {
		assertThat(FinalizeMapper.toChannelList(channels)).isEqualTo(expected);
	}

	private static Stream<Arguments> channelListArguments() {
		return Stream.of(
			Arguments.of(null, ""),
			Arguments.of(CommunicationChannels.create(), ""),
			Arguments.of(CommunicationChannels.create().withMinaSidor(true).withDigitalMailbox(true).withLetter(true), "MINA_SIDOR,DIGITAL_MAILBOX,LETTER"),
			Arguments.of(CommunicationChannels.create().withMinaSidor(false).withDigitalMailbox(false).withLetter(true), "LETTER"),
			Arguments.of(CommunicationChannels.create().withMinaSidor(true), "MINA_SIDOR"));
	}

	@Test
	void toRpaTask() {
		final var task = FinalizeMapper.toRpaTask(RpaAction.WRITE_DECISION, "ns:e:WRITE_DECISION", true);

		assertThat(task.getAction()).isEqualTo("WRITE_DECISION");
		assertThat(task.getReference()).isEqualTo("ns:e:WRITE_DECISION");
		assertThat(task.getEnqueued()).isTrue();

		assertThat(FinalizeMapper.toRpaTask(null, null, false)).hasAllNullFieldsOrPropertiesExcept("enqueued");
	}
}
