package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CommunicationChannels;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeDecision;
import se.sundsvall.caremanagement.types.financialassistance.api.model.FinalizeRequest;
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
}
