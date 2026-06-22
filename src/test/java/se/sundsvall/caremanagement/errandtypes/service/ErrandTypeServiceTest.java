package se.sundsvall.caremanagement.errandtypes.service;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.registry.ErrandTypeContribution;
import se.sundsvall.caremanagement.core.service.registry.ErrandTypeRegistry;
import se.sundsvall.caremanagement.errandtypes.api.model.DecisionOption;
import se.sundsvall.caremanagement.errandtypes.api.model.FieldDescriptor;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderRoleRegistry;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ErrandTypeServiceTest {

	private static final String SLUG_NEW = "financial-assistance-new";
	private static final String SLUG_RENEWAL = "financial-assistance-renewal";

	private static final FieldDescriptor FIELD = FieldDescriptor.create()
		.withName("maritalStatus").withType("ENUM").withRequired(true).withAppliesTo(List.of("NEW"));

	@Mock
	private ErrandTypeRegistry typeRegistryMock;

	@Mock
	private StakeholderRoleRegistry roleRegistryMock;

	private ErrandTypeService newService(final List<ErrandTypeSchemaContribution> contributions) {
		return new ErrandTypeService(typeRegistryMock, roleRegistryMock, contributions);
	}

	@Test
	void findAllSortsBySlug() {
		when(typeRegistryMock.knownSlugs()).thenReturn(Set.of(SLUG_RENEWAL, SLUG_NEW));
		when(typeRegistryMock.get(SLUG_NEW)).thenReturn(type(SLUG_NEW, "New"));
		when(typeRegistryMock.get(SLUG_RENEWAL)).thenReturn(type(SLUG_RENEWAL, "Renewal"));
		when(roleRegistryMock.rolesFor(SLUG_NEW)).thenReturn(Set.of());
		when(roleRegistryMock.rolesFor(SLUG_RENEWAL)).thenReturn(Set.of());

		final var all = newService(List.of()).findAll();

		assertThat(all).extracting("typeSlug").containsExactly(SLUG_NEW, SLUG_RENEWAL);
	}

	@Test
	void findBySlugMapsEveryPart() {
		when(typeRegistryMock.exists(SLUG_NEW)).thenReturn(true);
		when(typeRegistryMock.get(SLUG_NEW)).thenReturn(type(SLUG_NEW, "New"));
		when(roleRegistryMock.rolesFor(SLUG_NEW)).thenReturn(Set.of(
			new RoleDefinition("CO_APPLICANT", "Medsökande", 1, false),
			new RoleDefinition("APPLICANT", "Sökande", 1, true)));

		final var schema = newService(List.of(contribution(SLUG_NEW, "NEW", List.of(FIELD)))).findBySlug(SLUG_NEW);

		assertThat(schema.getTypeSlug()).isEqualTo(SLUG_NEW);
		assertThat(schema.getDisplayName()).isEqualTo("New");
		assertThat(schema.getApplicationType()).isEqualTo("NEW");
		assertThat(schema.getStatuses()).containsExactly("RECEIVED", "REJECTED", "UNDER_REVIEW");
		assertThat(schema.getRoles()).extracting(RoleDefinition::code).containsExactly("APPLICANT", "CO_APPLICANT");
		assertThat(schema.getFields()).containsExactly(FIELD);
		assertThat(schema.getDecisionOptions()).isEmpty();
	}

	@Test
	void findBySlugSurfacesDecisionOptionsFromContribution() {
		final var option = DecisionOption.create().withCode("BIFALL").withDisplayName("Bifall").withCarriesAmount(true);
		when(typeRegistryMock.exists(SLUG_NEW)).thenReturn(true);
		when(typeRegistryMock.get(SLUG_NEW)).thenReturn(type(SLUG_NEW, "New"));
		when(roleRegistryMock.rolesFor(SLUG_NEW)).thenReturn(Set.of());

		final var schema = newService(List.of(contributionWithOptions(SLUG_NEW, List.of(option)))).findBySlug(SLUG_NEW);

		assertThat(schema.getDecisionOptions()).containsExactly(option);
	}

	@Test
	void findBySlugWithoutSchemaContributionDegradesGracefully() {
		when(typeRegistryMock.exists(SLUG_RENEWAL)).thenReturn(true);
		when(typeRegistryMock.get(SLUG_RENEWAL)).thenReturn(type(SLUG_RENEWAL, "Renewal"));
		when(roleRegistryMock.rolesFor(SLUG_RENEWAL)).thenReturn(Set.of());

		final var schema = newService(List.of()).findBySlug(SLUG_RENEWAL);

		assertThat(schema.getApplicationType()).isNull();
		assertThat(schema.getFields()).isEmpty();
		assertThat(schema.getDecisionOptions()).isEmpty();
	}

	@Test
	void findBySlugUnknownThrowsNotFound() {
		when(typeRegistryMock.exists("bogus")).thenReturn(false);

		final var service = newService(List.of());

		assertThatThrownBy(() -> service.findBySlug("bogus"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessageContaining("bogus");
	}

	private static ErrandTypeContribution type(final String slug, final String displayName) {
		return ErrandTypeContribution.builder(slug)
			.displayName(displayName)
			.allowedStatuses("RECEIVED", "UNDER_REVIEW", "REJECTED")
			.build();
	}

	private static ErrandTypeSchemaContribution contribution(final String slug, final String applicationType,
		final List<FieldDescriptor> fields) {
		return new ErrandTypeSchemaContribution() {
			@Override
			public String typeSlug() {
				return slug;
			}

			@Override
			public String applicationType() {
				return applicationType;
			}

			@Override
			public List<FieldDescriptor> fields() {
				return fields;
			}
		};
	}

	private static ErrandTypeSchemaContribution contributionWithOptions(final String slug, final List<DecisionOption> options) {
		return new ErrandTypeSchemaContribution() {
			@Override
			public String typeSlug() {
				return slug;
			}

			@Override
			public String applicationType() {
				return "NEW";
			}

			@Override
			public List<FieldDescriptor> fields() {
				return List.of();
			}

			@Override
			public List<DecisionOption> decisionOptions() {
				return options;
			}
		};
	}
}
