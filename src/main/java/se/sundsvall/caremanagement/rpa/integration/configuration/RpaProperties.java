package se.sundsvall.caremanagement.rpa.integration.configuration;

import java.util.Map;
import java.util.Set;
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
 * Orchestrator). Defaults to {@code false}, the same as {@code application.yml}, so an unconfigured deploy never talks
 * to
 * an Orchestrator.</li>
 * <li>{@code disabledActions} — {@link se.sundsvall.caremanagement.rpa.service.RpaAction} names that are a logged no-op
 * even while RPA is enabled. It lets another executor (Draken's BFF writing to Lifecare directly) take over one action
 * at
 * a time without the robot writing the same thing a second time.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "integration.rpa")
public record RpaProperties(

	@DefaultValue("false") boolean enabled,

	@DefaultValue Set<String> disabledActions,

	@DefaultValue("RakelEkonomisktBistand") String queue,

	@DefaultValue Map<String, String> folderIds,

	@DefaultValue("5") int connectTimeout,

	@DefaultValue("30") int readTimeout) {
}
