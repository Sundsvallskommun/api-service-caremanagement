package se.sundsvall.caremanagement.core.service.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Aggregates {@link ErrandTypeContribution} beans from type modules at startup.
 * Type modules never touch this class — they expose a bean, Spring wires it in.
 */
@Component
class ErrandTypeRegistryImpl implements ErrandTypeRegistry {

	private final Map<String, ErrandTypeContribution> bySlug;

	ErrandTypeRegistryImpl(final List<ErrandTypeContribution> contributions) {
		this.bySlug = contributions.stream()
			.collect(Collectors.toUnmodifiableMap(
				ErrandTypeContribution::typeSlug,
				c -> c));
	}

	@Override
	public Set<String> knownSlugs() {
		return bySlug.keySet();
	}

	@Override
	public ErrandTypeContribution get(final String typeSlug) {
		return Optional.ofNullable(bySlug.get(typeSlug))
			.orElseThrow(() -> new IllegalArgumentException("Unknown errand type: " + typeSlug));
	}

	@Override
	public boolean exists(final String typeSlug) {
		return bySlug.containsKey(typeSlug);
	}

	@Override
	public boolean isValidStatus(final String typeSlug, final String status) {
		return Optional.ofNullable(bySlug.get(typeSlug))
			.map(c -> c.isValidStatus(status))
			.orElse(false);
	}

	@Override
	public boolean isValidTransition(final String typeSlug, final String fromStatus, final String toStatus) {
		return Optional.ofNullable(bySlug.get(typeSlug))
			.map(c -> c.isValidTransition(fromStatus, toStatus))
			.orElse(false);
	}
}
