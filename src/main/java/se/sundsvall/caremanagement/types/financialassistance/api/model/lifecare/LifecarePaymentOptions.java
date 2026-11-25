package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The betalsätt, betalningsmottagare, ändamål, saldon and months an utbetalning on the errand's insats can use, read
 * from Lifecare, plus a proposal to start the form from.
 */
@Schema(description = "The betalsätt, betalningsmottagare, ändamål, saldon and months an utbetalning on the insats can use, read from Lifecare")
public record LifecarePaymentOptions(

	@Schema(description = "The betalsätt in use on the insats") List<LifecarePaymentMethod> paymentMethods,

	@Schema(description = "The active betalningsmottagare") List<LifecarePayee> payees,

	@Schema(description = "The insats's konteringsrader") List<LifecarePaymentPosting> postings,

	@Schema(description = "The insats's saldon: what is left to pay out, as Lifecare counts it") List<LifecarePaymentBalance> balances,

	@Schema(description = "The months an utbetalning may concern") List<LifecarePaymentConcernMonth> concernMonths,

	@Schema(description = "What the utbetalning form starts from") LifecarePaymentProposal proposal) {
}
