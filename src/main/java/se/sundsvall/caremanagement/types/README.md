# Type Modules

This directory is reserved for per-errand-type modules. Each type module follows the
dept44-aligned layout (see `docs/architecture/proposal-modulith.md` and `migration-plan.md`)
and registers itself with the core registries via Spring `@Configuration` beans.

Nothing lives here yet — the actual type list is still being defined.

## Adding a new type

Recipe (see Phase 3 in the migration plan):

```
types/<slug>/
├── configuration/<Slug>ModuleConfig.java   ← ErrandTypeContribution + StakeholderRoleContribution beans
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
