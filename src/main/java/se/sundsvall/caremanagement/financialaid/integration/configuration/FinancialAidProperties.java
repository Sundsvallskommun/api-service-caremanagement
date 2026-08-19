package se.sundsvall.caremanagement.financialaid.integration.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Timeouts (in seconds) for the financial-aid (SSBTEK proxy) integration. The base url and OAuth2 client-credentials
 * registration are configured in {@code application.yml} ({@code integration.financial-aid.url} and the
 * {@code financial-aid} Spring security registration).
 */
@ConfigurationProperties("integration.financial-aid")
public record FinancialAidProperties(@DefaultValue("5") int connectTimeout, @DefaultValue("30") int readTimeout) {
}
