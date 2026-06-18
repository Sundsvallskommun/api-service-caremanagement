package se.sundsvall.caremanagement.errandtypes.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.core.service.registry.ErrandTypeContribution;
import se.sundsvall.caremanagement.core.service.registry.ErrandTypeRegistry;
import se.sundsvall.caremanagement.errandtypes.api.model.ErrandTypeSchema;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderRoleRegistry;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Assembles the {@code /errand-types} descriptor from three sources: the core type registry (display name, statuses),
 * the stakeholder role registry (roles) and the per-slug {@link ErrandTypeSchemaContribution} beans (application-type
 * variant, fields).
 */
@Service
public class ErrandTypeService {

	private final ErrandTypeRegistry typeRegistry;
	private final StakeholderRoleRegistry roleRegistry;
	private final Map<String, ErrandTypeSchemaContribution> schemaBySlug;

	ErrandTypeService(final ErrandTypeRegistry typeRegistry, final StakeholderRoleRegistry roleRegistry,
		final List<ErrandTypeSchemaContribution> schemaContributions) {
		this.typeRegistry = typeRegistry;
		this.roleRegistry = roleRegistry;
		this.schemaBySlug = schemaContributions.stream()
			.collect(Collectors.toUnmodifiableMap(ErrandTypeSchemaContribution::typeSlug, Function.identity()));
	}

	/** Every registered errand type, sorted by slug. */
	public List<ErrandTypeSchema> findAll() {
		return typeRegistry.knownSlugs().stream()
			.sorted()
			.map(this::toSchema)
			.toList();
	}

	/** The descriptor for one slug, or a 404 problem when the slug is unknown. */
	public ErrandTypeSchema findBySlug(final String typeSlug) {
		if (!typeRegistry.exists(typeSlug)) {
			throw Problem.valueOf(NOT_FOUND, "No errand type with slug '" + typeSlug + "'");
		}
		return toSchema(typeSlug);
	}

	private ErrandTypeSchema toSchema(final String typeSlug) {
		final ErrandTypeContribution type = typeRegistry.get(typeSlug);
		final Optional<ErrandTypeSchemaContribution> schema = Optional.ofNullable(schemaBySlug.get(typeSlug));

		return ErrandTypeSchema.create()
			.withTypeSlug(typeSlug)
			.withDisplayName(type.displayName())
			.withApplicationType(schema.map(ErrandTypeSchemaContribution::applicationType).orElse(null))
			.withStatuses(type.allowedStatuses().stream().sorted().toList())
			.withRoles(roleRegistry.rolesFor(typeSlug).stream()
				.sorted(Comparator.comparing(RoleDefinition::code))
				.toList())
			.withFields(schema.map(ErrandTypeSchemaContribution::fields).orElseGet(List::of))
			.withDecisionOptions(schema.map(ErrandTypeSchemaContribution::decisionOptions).orElseGet(List::of));
	}
}
