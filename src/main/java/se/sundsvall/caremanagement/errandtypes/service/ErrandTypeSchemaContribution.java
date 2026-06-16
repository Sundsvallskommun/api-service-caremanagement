package se.sundsvall.caremanagement.errandtypes.service;

import java.util.List;
import se.sundsvall.caremanagement.errandtypes.api.model.FieldDescriptor;

/**
 * Each type module exposes one of these as a Spring bean per slug to declare the field specification of that slug's
 * {@code data} payload. The {@code errandtypes} module aggregates them and merges them with the type registry
 * (statuses,
 * display name) and the stakeholder role registry into the public {@code /errand-types} descriptor.
 *
 * <p>
 * A type that registers an {@link se.sundsvall.caremanagement.core.service.registry.ErrandTypeContribution} but no
 * schema contribution still appears in the catalogue — with a null {@code applicationType} and an empty field list.
 * </p>
 */
public interface ErrandTypeSchemaContribution {

	/** The slug this contribution describes. */
	String typeSlug();

	/**
	 * The application-type variant the slug maps to (e.g. {@code RENEWAL}), or {@code null} when the type has no
	 * sub-type variant.
	 */
	String applicationType();

	/** The fields this slug's data payload should carry, as form guidance. */
	List<FieldDescriptor> fields();
}
