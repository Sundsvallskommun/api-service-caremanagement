package se.sundsvall.caremanagement.types.financialassistance.configuration;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the decision proposal (beslutsförslag) on the DECISION tab.
 *
 * @param recoveryClaimLookbackMonths how far back, in months ending at the application month, återkrav (FLAG-05) are
 *                                    looked for in Lifecare. Money owed back stays owed across many applications, so
 *                                    this reaches further than the previous-decision lookup
 */
@Validated
@ConfigurationProperties(prefix = "financial-assistance.decision-proposal")
public record DecisionProposalProperties(

	@Positive @DefaultValue("36") int recoveryClaimLookbackMonths) {
}
