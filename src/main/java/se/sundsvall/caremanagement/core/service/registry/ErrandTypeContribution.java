package se.sundsvall.caremanagement.core.service.registry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * One per errand type. Declares slug, display name, allowed statuses, and
 * permitted status transitions. Type modules expose one of these as a Spring bean.
 */
public final class ErrandTypeContribution {

	private final String typeSlug;
	private final String displayName;
	private final Set<String> allowedStatuses;
	private final Map<String, Set<String>> allowedTransitions;

	private ErrandTypeContribution(final Builder builder) {
		this.typeSlug = builder.typeSlug;
		this.displayName = builder.displayName;
		this.allowedStatuses = Set.copyOf(builder.allowedStatuses);
		this.allowedTransitions = Map.copyOf(builder.allowedTransitions);
	}

	public String typeSlug() {
		return typeSlug;
	}

	public String displayName() {
		return displayName;
	}

	public Set<String> allowedStatuses() {
		return allowedStatuses;
	}

	public boolean isValidStatus(final String status) {
		return allowedStatuses.contains(status);
	}

	public boolean isValidTransition(final String fromStatus, final String toStatus) {
		return allowedTransitions
			.getOrDefault(fromStatus, Set.of())
			.contains(toStatus);
	}

	public static Builder builder(final String typeSlug) {
		return new Builder(typeSlug);
	}

	public static final class Builder {
		private final String typeSlug;
		private String displayName;
		private final Set<String> allowedStatuses = new HashSet<>();
		private final Map<String, Set<String>> allowedTransitions = new HashMap<>();

		private Builder(final String typeSlug) {
			this.typeSlug = typeSlug;
		}

		public Builder displayName(final String displayName) {
			this.displayName = displayName;
			return this;
		}

		public Builder allowedStatuses(final String... statuses) {
			allowedStatuses.addAll(Set.of(statuses));
			return this;
		}

		public Builder allowedTransition(final String fromStatus, final String... toStatuses) {
			allowedStatuses.add(fromStatus);
			allowedStatuses.addAll(Set.of(toStatuses));
			allowedTransitions
				.computeIfAbsent(fromStatus, k -> new HashSet<>())
				.addAll(Set.of(toStatuses));
			return this;
		}

		public ErrandTypeContribution build() {
			return new ErrandTypeContribution(this);
		}
	}
}
