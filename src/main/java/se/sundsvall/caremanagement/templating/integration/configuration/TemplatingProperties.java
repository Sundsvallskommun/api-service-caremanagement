package se.sundsvall.caremanagement.templating.integration.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.templating")
public record TemplatingProperties(int connectTimeout, int readTimeout) {
}
