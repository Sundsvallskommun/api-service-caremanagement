# Type Modules

This directory is reserved for per-errand-type modules. Each type module follows the
dept44-aligned layout (see `docs/architecture/proposal-modulith.md` and `migration-plan.md`)
and registers itself with the core registries via Spring `@Configuration` beans.

The first type module lives here: `financialassistance` (EB / financial assistance) — create/read/update,
the common-entry-point eligibility routing, and a per-slug form schema (see the reference contribution
below). More types will follow as they are defined.

## Adding a new type

Recipe (see Phase 3 in the migration plan):

```
types/<slug>/
├── configuration/<Slug>ModuleConfig.java   ← ErrandTypeContribution + StakeholderRoleContribution (+ optional ErrandTypeSchemaContribution) beans
├── api/
│   ├── <Slug>Resource.java                 ← REST controller
│   └── model/                              ← DTOs (strongly-typed; NO parameters blob)
├── service/
│   ├── <Slug>Service.java                  ← delegates envelope ops to core.service.ErrandService
│   └── mapper/<Slug>DataMapper.java
└── integration/db/
    ├── <Slug>Repository.java
    └── model/<Slug>Entity.java             ← shared PK with errand.id, FK cascade-delete
```

Plus a Flyway migration `db/migration/V<n>_0__create_errand_<slug>.sql` and a
`package-info.java` declaring `@ApplicationModule(allowedDependencies = { "core", "stakeholders", ... })`.

The `Application` class already has `additionalPackages = "se.sundsvall.caremanagement.types"`
in its `@Modulithic` annotation, so any new sub-package is auto-discovered.

## Exposing the type to clients (`/errand-types`)

The `errandtypes` module serves a read-only catalogue at
`GET /{municipalityId}/{namespace}/errand-types` and `/{typeSlug}`, returning an
`ErrandTypeSchema` per registered type: slug, optional `applicationType` variant, display name,
allowed statuses, stakeholder roles and a per-type `FieldDescriptor[]` form spec (so a frontend can
discover what e.g. a financial-assistance renewal looks like without hard-coding it).

Statuses/display name come from the core `ErrandTypeRegistry` and roles from the stakeholder
registry — those appear automatically. To also describe the fields its `data` payload carries, a type
module exposes an `ErrandTypeSchemaContribution` bean (one per slug) declaring the `FieldDescriptor`s.
A type without one still appears, with a null `applicationType` and an empty field list. See
`types/financialassistance/configuration/FinancialAssistanceSchema.java` for the reference contribution.
