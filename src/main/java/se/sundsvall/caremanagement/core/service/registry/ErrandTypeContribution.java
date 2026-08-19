package se.sundsvall.caremanagement.core.service.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * One per errand type. Declares slug, display name, allowed statuses (each with a human-readable display name, in
 * lifecycle order) and permitted status transitions. Type modules expose one of these as a Spring bean.
 */
public final class ErrandTypeContribution {

	private final String typeSlug;
	private final String displayName;
	private final Map<String, String> statusDisplayNames;
	private final Set<String> allowedStatuses;
	private final Map<String, Set<String>> allowedTransitions;
	private final boolean typedCreateOnly;

	private ErrandTypeContribution(final Builder builder) {
		this.typeSlug = builder.typeSlug;
		this.displayName = builder.displayName;
		this.statusDisplayNames = Collections.unmodifiableMap(new LinkedHashMap<>(builder.statusDisplayNames));
		this.allowedStatuses = Set.copyOf(builder.statusDisplayNames.keySet());
		this.allowedTransitions = Map.copyOf(builder.allowedTransitions);
		this.typedCreateOnly = builder.typedCreateOnly;
	}

	public String typeSlug() {
		return typeSlug;
	}

	public String displayName() {
		return displayName;
	}

	/** Allowed statuses, code → display name, in lifecycle (insertion) order. Display name may be {@code null}. */
	public Map<String, String> statusDisplayNames() {
		return statusDisplayNames;
	}

	public Set<String> allowedStatuses() {
		return allowedStatuses;
	}

	/**
	 * Whether errands of this type may only be created through the type module's own dedicated endpoint (which seeds the
	 * type-specific data and starts its process), never via the generic {@code POST /errands}. The generic create rejects
	 * such slugs so they can't produce a half-formed, process-less errand.
	 */
	public boolean typedCreateOnly() {
		return typedCreateOnly;
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
		private final Map<String, String> statusDisplayNames = new LinkedHashMap<>();
		private final Map<String, Set<String>> allowedTransitions = new HashMap<>();
		private boolean typedCreateOnly;

		private Builder(final String typeSlug) {
			this.typeSlug = typeSlug;
		}

		public Builder displayName(final String displayName) {
			this.displayName = displayName;
			return this;
		}

		/** Mark this type as creatable only via its module's dedicated endpoint, not the generic POST /errands. */
		public Builder typedCreateOnly(final boolean typedCreateOnly) {
			this.typedCreateOnly = typedCreateOnly;
			return this;
		}

		/** Declare an allowed status together with its human-readable display name; preserves declaration order. */
		public Builder status(final String code, final String displayName) {
			statusDisplayNames.put(code, displayName);
			return this;
		}

		/** Declare allowed statuses without display names (label falls back to {@code null}). */
		public Builder allowedStatuses(final String... statuses) {
			for (final String status : statuses) {
				statusDisplayNames.putIfAbsent(status, null);
			}
			return this;
		}

		public Builder allowedTransition(final String fromStatus, final String... toStatuses) {
			statusDisplayNames.putIfAbsent(fromStatus, null);
			for (final String toStatus : toStatuses) {
				statusDisplayNames.putIfAbsent(toStatus, null);
			}
			allowedTransitions
				.computeIfAbsent(fromStatus, _ -> new HashSet<>())
				.addAll(Set.of(toStatuses));
			return this;
		}

		public ErrandTypeContribution build() {
			return new ErrandTypeContribution(this);
		}
	}
}
