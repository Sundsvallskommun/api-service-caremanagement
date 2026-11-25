package se.sundsvall.caremanagement.lifecare.integration.integrator.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Timeouts (in seconds) for the lifecare-integrator route to FamilyCare. The base url and the OAuth2
 * client-credentials registration live in {@code application.yml} ({@code integration.lifecare-integrator.url} and the
 * {@code lifecare-integrator} Spring security registration).
 *
 * <p>
 * The read timeout is deliberately more generous than the five-second default used elsewhere: a call here traverses two
 * hops — the WSO2 gateway and then the integrator's own call into FamilyCare — and the proposal reads in particular
 * pull
 * whole code lists.
 */
@ConfigurationProperties("integration.lifecare-integrator")
public record LifecareIntegratorProperties(@DefaultValue("5") int connectTimeout, @DefaultValue("60") int readTimeout) {
}
