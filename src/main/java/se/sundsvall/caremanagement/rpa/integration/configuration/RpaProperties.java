package se.sundsvall.caremanagement.rpa.integration.configuration;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration for the UiPath Orchestrator RPA integration. A "write/fetch" is enqueued as a UiPath <em>queue
 * item</em> ({@code AddQueueItem}); a robot picks it up out of band and performs the Lifecare GUI work keyed on the
 * item's reference (the errandId).
 *
 * <ul>
 * <li>{@code url} — the Orchestrator base URL (host, the {@code /odata/...} path is on the client).</li>
 * <li>{@code queue} — the single financial assistance queue all items are added to; the concrete action travels in the
 * item's
 * {@code SpecificContent}, so no new queue has to be provisioned per action.</li>
 * <li>{@code folderIds} — municipalityId → Orchestrator folder (the {@code X-UIPATH-OrganizationUnitId} header).</li>
 * <li>{@code enabled} — master switch; when {@code false} an enqueue is a logged no-op (environments without an
 * Orchestrator).</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "integration.rpa")
public record RpaProperties(

	@DefaultValue("true") boolean enabled,

	@DefaultValue("RakelEkonomisktBistand") String queue,

	@DefaultValue Map<String, String> folderIds,

	@DefaultValue("5") int connectTimeout,

	@DefaultValue("30") int readTimeout) {
}
