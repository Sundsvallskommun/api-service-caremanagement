package se.sundsvall.caremanagement.stakeholders.api.model;

public record RoleDefinition(
	String code,             // FOSTER_PARENT
	String displayName,      // "Familjehemsförälder"
	int maxOccurrences,      // 0 = unbounded
	boolean required) {
	public RoleDefinition {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException("code must not be blank");
		}
	}
}
