package se.sundsvall.caremanagement.financialaid.integration.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Timeouts for the financial-aid (SSBTEK proxy) integration. The base url and OAuth2 client-credentials registration
 * are
 * configured in {@code application.yml} ({@code integration.financial-aid.url} and the {@code financial-aid} Spring
 * security registration).
 */
@ConfigurationProperties("integration.financial-aid")
public record FinancialAidProperties(int connectTimeout, int readTimeout) {
}
