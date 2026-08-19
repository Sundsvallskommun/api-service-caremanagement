/**
 * Financial-aid (SSBTEK proxy) integration.
 *
 * <p>
 * Exposed so other modules — the lifecare SSBTEK income pipeline in particular — can fetch a person's aggregated SSBTEK
 * basis through {@link se.sundsvall.caremanagement.financialaid.integration.FinancialAidIntegration} without reaching
 * into the OAuth2/Feign configuration. The basis is returned as a generic per-agency map, so no financial-aid type
 * crosses the module boundary beyond the integration entry point.
 * </p>
 */
@NamedInterface("integration")
package se.sundsvall.caremanagement.financialaid.integration;

import org.springframework.modulith.NamedInterface;
