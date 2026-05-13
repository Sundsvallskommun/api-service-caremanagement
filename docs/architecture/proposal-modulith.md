# CareM Architecture Proposal — Modular Errand Foundation

**Status:** Draft for discussion
**Author:** Cheezi (with Claude)
**Date:** 2026-05-11
**Target stack:** dept44 8.x · Spring Boot 4 · Java 25 · Spring Modulith

---

## TL;DR

CareM today is a clone of SupportM's design and inherits its core sin: a generic `Errand` with a `parameters` blob that becomes a graveyard for everything we don't want to model properly. With ~50 errand types planned, doubling down on that pattern will be a disaster.

**The pivot:**

1. **Composition over generics.** `Errand` becomes a thin envelope. Each errand type lives in its own module with its own entity, its own table, its own endpoints, its own strongly-typed `data`.
2. **Spring Modulith** enforces module boundaries at build time. Cross-module communication is event-driven, not service-to-service calls.
3. **Registry over enum.** Errand types, stakeholder roles, notification triggers — registered at startup, never hardcoded in core.
4. **No `parameters` list, no `jsonParameters` blob, no `extraParameters` map.** Anywhere. If a field exists, it has a column.

API envelope stays close to SupportM so frontend doesn't burn down. Per-type `data` is strongly typed via OpenAPI generation.

---

## Where we are today

### CareM as it stands (the rescue target)

`se.sundsvall.caremanagement.integration.db.model.ErrandEntity` currently holds:

- `parameters: List<ParameterEntity>` — the swamp begins
- `externalTags: List<TagEmbeddable>` — more untyped k-v
- `StakeholderEntity` → `parameters: List<StakeholderParameterEntity>` — swamp jr.
- `status`, `category`, `type`, `priority`, `contactReason` — all untyped `String`
- `processDefinitionName`, `processInstanceId` — Operaton BPMN integration
- `decisions: List<DecisionEntity>` — already separate, good
- No notes, no status_history, no module boundaries

### SupportM (`api-service-support-management`)

The same swamp, mature edition:

|                                   Smell                                   |         Location         |
|---------------------------------------------------------------------------|--------------------------|
| `parameters: List<ParameterEntity>`                                       | `ErrandEntity.java`      |
| `jsonParameters: List<JsonParameterEntity>` (JSON blob + optional schema) | `ErrandEntity.java`      |
| `externalTags: List<DbExternalTag>`                                       | `ErrandEntity.java`      |
| `parameters` also on `StakeholderEntity`                                  | `StakeholderEntity.java` |
| `status: String` (no history, only `previousStatus`)                      | `ErrandEntity.java`      |
| `role: String` on stakeholder, `@ValidRole` annotation                    | `StakeholderEntity.java` |

Concrete swamp evidence: `ErrandParametersIT/__files/test01_updateErrandParameters/request.json` is a list of `{key, displayName, group, values[]}` with arbitrary `key` strings ("key0", "key1", "key2"). No schema, no type safety, no validation per key.

### CaseData (`api-service-case-data`)

Same disease, slightly different bottle:

|                     Smell                     |         Location         |
|-----------------------------------------------|--------------------------|
| `extraParameters: List<ExtraParameterEntity>` | `ErrandEntity.java`      |
| `jsonParameters: List<JsonParameterEntity>`   | `ErrandEntity.java`      |
| `roles: List<String>` on stakeholder          | `StakeholderEntity.java` |
| `caseType: Enum` (hardcoded in core)          | `ErrandEntity.java`      |

CaseData *does* have proper status history (`statuses: List<StatusEntity>`) — that's the one thing worth stealing.

---

## The pivot — four design tenets

### Tenet 1 — Errand is an envelope, not a god object

`Errand` (core) carries only the universal envelope: id, type-slug, namespace, municipality, status, parties (envelope-level FK), audit timestamps, BPMN handles.

Type-specific data lives in its own table, its own entity, its own module. No exceptions.

### Tenet 2 — Module boundaries are enforced by the compiler, not by convention

Spring Modulith verifies in a test that modules only touch each other's `api` (public) packages. If a type module tries to reach into `notes.internal`, the build breaks. This is the contract that SupportM never had.

### Tenet 3 — Cross-module communication is event-driven

Core publishes events (`ErrandCreated`, `ErrandStatusChanged`, `ErrandAssigned`). Other modules subscribe via `@ApplicationModuleListener`. Modulith persists events to an outbox so they survive crashes.

A new type module reacting to a status change does **not** require touching core. A new notification trigger does **not** require touching core. New behavior = new subscriber.

### Tenet 4 — Registries over enums

`ErrandTypeRegistry`, `StakeholderRoleRegistry`, `StatusTransitionRegistry`, `NotificationTriggerRegistry`. Each type module registers its valid values at startup via Spring autoconfig. Frontend fetches the registry over HTTP to render dropdowns. No enum in core ever names a specific type.

---

## Module layout (Spring Modulith)

Single Maven module, package-per-module under `se.sundsvall.caremanagement`:

```
se.sundsvall.caremanagement
├── Application.java                ← @Modulithic
│
├── core/                           ← envelope, lifecycle, registry
│   ├── api/                        ← public API: events, registry beans, base DTOs
│   ├── domain/                     ← Errand entity, repository, service
│   └── web/                        ← envelope endpoints, OpenAPI
│
├── stakeholders/                   ← universal stakeholder model
│   ├── api/                        ← public: Stakeholder DTO, role registry contract
│   └── ... (domain, web)
│
├── notes/                          ← universal
├── attachments/                    ← universal
├── statushistory/                  ← state transition log
├── notifications/                  ← listens to events, dispatches
├── decisions/                      ← universal decision base (D2)
│
├── lookup/                         ← LookupEntity/LookupKind infrastructure (D1)
├── operaton/                       ← BPMN integration (existing, generic glue)
│
└── types/
    ├── fostercare/
    │   ├── api/                    ← FosterCareData DTO (public for OpenAPI)
    │   ├── domain/                 ← FosterCareEntity, repository, service
    │   ├── web/                    ← /errands/fostercare/* endpoints
    │   ├── events/                 ← type-specific events (optional)
    │   └── FosterCareModuleConfig  ← registers type + roles + transitions
    ├── adoption/                   ← same shape
    └── ...                         ← ~50 of these
```

**Module declarations:**

Each module gets a `package-info.java` with `@ApplicationModule`:

```java
@ApplicationModule(
    displayName = "Foster Care",
    allowedDependencies = { "core::api", "stakeholders::api", "notes::api" }
)
package se.sundsvall.caremanagement.types.fostercare;
```

The `::api` suffix says "I can only depend on the `api` subpackage of this module" — internals are off-limits even within the same JVM.

**Verification test** (lives once, in the app module):

```java
@Test
void verifyModuleBoundaries() {
    ApplicationModules.of(Application.class).verify();
}
```

This fails the build if anyone reaches across a boundary. End of debate.

### Why one Maven module, not 50

|                   Option                    |                          Verdict                           |
|---------------------------------------------|------------------------------------------------------------|
| 50 deployables                              | Operational suicide for one team                           |
| 1 Maven module, 50 packages, no enforcement | What SupportM became — coupling everywhere                 |
| **1 Maven module, Spring Modulith**         | Compile-time boundaries, single deployable, fast iteration |
| 50 Maven submodules                         | Heavy ceremony, slow builds, real isolation                |

Start with Modulith. If compile times degrade or we genuinely need per-type release cadence, promote individual types to Maven submodules later. Modulith doesn't lock that door.

---

## Data model strategy

### Shared schema, prefixed tables, per-module Flyway locations

```
errand                          ← core (envelope)
errand_status_history           ← statushistory module
errand_note                     ← notes module
errand_attachment               ← attachments module
errand_stakeholder              ← stakeholders module
errand_stakeholder_role         ← stakeholder roles (lookup, populated by registries)
errand_fostercare               ← type module, FK to errand.id
errand_adoption                 ← type module, FK to errand.id
...
```

**Why not schema-per-type?**
- 50 schemas in MariaDB is operationally noisy
- Cross-schema FKs are clunky
- Backup/restore granularity per-type is overkill at this scale
- Flyway location-per-module gives the same module ownership without the schema overhead

**Flyway layout:**

```
src/main/resources/db/migration/
├── core/                ← V1_001__create_errand.sql, etc.
├── stakeholders/
├── notes/
├── attachments/
├── statushistory/
└── types/
    ├── fostercare/      ← V1_001__create_errand_fostercare.sql
    ├── adoption/
    └── ...
```

`spring.flyway.locations` lists each. Each module owns its DDL physically — the directory mirrors the package.

### Base `errand` table — the only universal columns

```sql
CREATE TABLE errand (
    id                   CHAR(36)     PRIMARY KEY,           -- UUID
    type_slug            VARCHAR(64)  NOT NULL,              -- 'fostercare', 'adoption'...
    municipality_id      VARCHAR(8)   NOT NULL,
    namespace            VARCHAR(32)  NOT NULL,
    errand_number        VARCHAR(64)  NOT NULL UNIQUE,
    status               VARCHAR(64)  NOT NULL,
    title                VARCHAR(255),
    description          LONGTEXT,
    priority             VARCHAR(32),
    reporter_user_id     VARCHAR(64),
    assigned_user_id     VARCHAR(64),
    process_definition   VARCHAR(128),
    process_instance_id  VARCHAR(128),
    created              DATETIME(3)  NOT NULL,
    modified             DATETIME(3),
    touched              DATETIME(3),
    -- INDEX (municipality_id, namespace, type_slug, status, touched)
    -- INDEX (municipality_id, namespace, assigned_user_id)
);
```

**Conspicuously absent:** `category`, `type`, `parameters_*`, `extra_parameters_*`, `json_parameters_*`, `external_tags_*`. `type_slug` is the only type-discriminator and it's tied to a registered module.

**Things that look universal but aren't:**

- `priority` — keep, only if every type genuinely uses it the same way. Push back if not.
- `title`, `description` — keep, but `title` is a display hint, not a structured field. Each type can override how it renders.
- `category` — **drop**. This was always a stand-in for "more detailed type". With per-type modules, the module IS the category.

### Per-type table — owned by the type module

```sql
CREATE TABLE errand_fostercare (
    errand_id           CHAR(36)    PRIMARY KEY,
    placement_date      DATE        NOT NULL,
    foster_family_id    CHAR(36)    NOT NULL,
    placement_type      VARCHAR(64) NOT NULL,
    expected_end_date   DATE,
    -- ... whatever foster-care needs, all properly typed
    CONSTRAINT fk_fostercare_errand FOREIGN KEY (errand_id)
        REFERENCES errand(id) ON DELETE CASCADE
);
```

Shared PK with `errand.id`. One row per errand. Cascade delete keeps lifecycle simple.

---

## API envelope shape

### Keep SupportM's path shape, replace the payload

```
POST   /{municipalityId}/{namespace}/errands/{typeSlug}
GET    /{municipalityId}/{namespace}/errands/{errandId}
GET    /{municipalityId}/{namespace}/errands           ← lists envelopes (no type-data)
PATCH  /{municipalityId}/{namespace}/errands/{errandId}
DELETE /{municipalityId}/{namespace}/errands/{errandId}

POST   /{municipalityId}/{namespace}/errands/{typeSlug}/{errandId}/data   ← type-specific update
GET    /{municipalityId}/{namespace}/errands/{typeSlug}/{errandId}        ← envelope + typed data
```

The envelope endpoints live in core. Type-data endpoints live in the type module.

### Request body — strongly typed `data`

```json
POST /2281/socialservices/errands/fostercare
{
  "namespace": "socialservices",
  "title": "Placement för Familjen X",
  "description": "...",
  "priority": "NORMAL",
  "reporterUserId": "user-123",
  "stakeholders": [
    { "role": "CHILD",          "personId": "..." },
    { "role": "FOSTER_PARENT",  "personId": "..." },
    { "role": "BIO_PARENT",     "personId": "..." }
  ],
  "data": {
    "placementDate": "2026-06-01",
    "fosterFamilyId": "ff-789",
    "placementType": "EMERGENCY",
    "expectedEndDate": "2026-12-01"
  }
}
```

`data` is a strongly typed object generated from a per-type OpenAPI spec. Frontend gets exact TypeScript types per endpoint via codegen. No more guessing keys.

### Response body — envelope + typed data

```json
{
  "id": "errand-uuid",
  "type": "fostercare",
  "errandNumber": "CAREM-2026-00042",
  "namespace": "socialservices",
  "status": "ONGOING",
  "title": "...",
  "stakeholders": [ ... ],
  "data": { ...same shape as request data... },
  "created": "...",
  "modified": "...",
  "_links": {
    "self": "/2281/socialservices/errands/errand-uuid",
    "typed": "/2281/socialservices/errands/fostercare/errand-uuid"
  }
}
```

### List endpoint returns envelopes only — the inbox view

```
GET /{municipalityId}/{namespace}/errands?filter=assignedUserId:'me' and status:'ONGOING'&sort=touched,desc
```

Lists return envelope shape **without** `data`. This is the cross-type inbox view (D5). Frontend renders per-row using `typeSlug` to pick the right icon/label. If you want type data, fetch the typed endpoint per errand. Payload size stays bounded regardless of how fat any type module grows.

Filterable on every envelope column (`typeSlug`, `status`, `assignedUserId`, `priority`, `created`, `touched`, `errandNumber`) via spring-filter.

---

## Universal modules

### Stakeholders

Universal data shape (person/org + contact), type-specific roles.

```java
// stakeholders::api - public registry contract
public interface StakeholderRoleRegistry {
    Set<RoleDefinition> rolesFor(String typeSlug);
    boolean isValidRole(String typeSlug, String role);
}

// stakeholders::api - public role definition
public record RoleDefinition(
    String code,           // FOSTER_PARENT
    String displayName,    // "Familjehemsförälder"
    int maxOccurrences,    // 2 (e.g. couple)
    boolean required
) {}
```

Each type module registers its roles in its config:

```java
// types/fostercare/FosterCareModuleConfig.java
@Configuration
class FosterCareModuleConfig {
    @Bean
    StakeholderRoleContribution fosterCareRoles() {
        return new StakeholderRoleContribution("fostercare", Set.of(
            new RoleDefinition("CHILD",         "Barn",                    1, true),
            new RoleDefinition("FOSTER_PARENT", "Familjehemsförälder",     2, true),
            new RoleDefinition("BIO_PARENT",    "Biologisk förälder",      2, false)
        ));
    }
}
```

Frontend fetches `GET /errands/{typeSlug}/stakeholder-roles` to populate dropdowns dynamically. Adding a new role to fostercare = one bean change in one module. No core touch.

### Notes

Plain text + author + timestamp + (optional) type. No relation to the swamp. Linked by `errand_id`.

Public events: `NoteAdded`, `NoteUpdated`, `NoteDeleted`. That's it.

### Attachments

Already exists today (`AttachmentEntity`, `AttachmentDataEntity`). Move into the `attachments` module. Strip the parameter-style metadata if any has crept in.

### Status history

Steal CaseData's design — `errand_status_history` row per transition, automatic via `@ApplicationModuleListener` on `ErrandStatusChanged`. Type modules can ALSO register valid transitions in a `StatusTransitionRegistry` so core can reject invalid transitions at the API layer.

### Notifications

Universal: a notification is `{ recipient, channel, template, payload, errandId, status }`. Type-specific: which events trigger which templates.

Each type module declares its notification config:

```java
@Bean
NotificationTriggerContribution fosterCareNotifications() {
    return NotificationTriggerContribution.builder("fostercare")
        .on(ErrandStatusChanged.class)
            .when(e -> e.newStatus().equals("PLACED"))
            .sendTemplate("placement_confirmed")
            .toRoles("FOSTER_PARENT", "BIO_PARENT")
        .on(ErrandCreated.class)
            .sendTemplate("application_received")
            .toRoles("CHILD")
        .build();
}
```

Notification module subscribes to all `Errand*` events, looks up the active trigger config for the errand's type, and dispatches. Core knows nothing.

---

## Event catalog (initial)

Public events live in `core::api.events` (and equivalent for each module).

|          Event          |        Module        |                       Payload                       |
|-------------------------|----------------------|-----------------------------------------------------|
| `ErrandCreated`         | core                 | id, type, namespace, municipalityId, reporterUserId |
| `ErrandStatusChanged`   | core                 | id, type, oldStatus, newStatus, by                  |
| `ErrandAssigned`        | core                 | id, type, oldAssignee, newAssignee, by              |
| `ErrandDeleted`         | core                 | id, type                                            |
| `ErrandTypeDataUpdated` | type module          | id, type, fields (set of changed field names)       |
| `StakeholderAdded`      | stakeholders         | errandId, stakeholderId, role                       |
| `NoteAdded`             | notes                | errandId, noteId, author                            |
| `AttachmentUploaded`    | attachments          | errandId, attachmentId, fileName                    |
| `DecisionRecorded`      | (?) decisions module | errandId, decisionId, outcome                       |

All events are records, immutable, JSON-serializable. Modulith persists them to an event log (Spring Modulith's `@Externalized` or just in-DB).

---

## Side-by-side: what dies, what replaces it

|                     SupportM / current CareM                     |                                                  New CareM                                                  |
|------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `ErrandEntity.parameters: List<ParameterEntity>`                 | **DELETED**. Each type's columns live in `errand_<type>` table.                                             |
| `ErrandEntity.jsonParameters: List<JsonParameterEntity>`         | **DELETED**. If structured data is needed, the type module owns a column or sub-table.                      |
| `ErrandEntity.externalTags: List<TagEmbeddable>`                 | **DELETED** (D3). Revives as a proper `labels` module if a real need emerges.                               |
| `ErrandEntity.category: String`                                  | **DELETED**. The type module IS the category.                                                               |
| `ErrandEntity.type: String`                                      | **RENAMED** to `type_slug`, validated against `ErrandTypeRegistry`.                                         |
| `ErrandEntity.status: String`                                    | **STAYS**, but values validated by `StatusTransitionRegistry` per type. Plus `errand_status_history` table. |
| `StakeholderEntity.parameters: List<StakeholderParameterEntity>` | **DELETED**. If a type needs extra stakeholder data, model it in the type module.                           |
| `StakeholderEntity.role: String` + `@ValidRole`                  | **STAYS** as string but validated per-type via `StakeholderRoleRegistry`.                                   |
| `ErrandParameterResource` (REST)                                 | **DELETED**.                                                                                                |
| `StakeholderParameterResource` (REST)                            | **DELETED**.                                                                                                |
| Generic `POST /errands` with type as a body field                | **REPLACED** by `POST /errands/{typeSlug}` with typed `data`.                                               |
| Service-to-service direct method calls                           | **REPLACED** by `@ApplicationModuleListener` on events.                                                     |
| `caseType: Enum` (CaseData)                                      | **REPLACED** by `ErrandTypeRegistry` populated by type modules.                                             |

---

## Decisions log (resolved 2026-05-11)

### D1 — What survives from current CareM? ✅ DECIDED

Keep the listed items:
- `decisions: List<DecisionEntity>` → promote to its own `decisions` module (see D2)
- `contactReason: LookupEntity` + `contactReasonDescription` → **type-specific**. Type modules own these if they need them, with their own lookup if applicable.
- `processDefinitionName` / `processInstanceId` → universal, stays on envelope (optional fields — see D6)
- `LookupEntity` / `LookupKind` → keep as lookup infrastructure under a `lookup` module (or core, since it's pure utility)

### D2 — Decisions: universal or type-specific? ✅ DECIDED — both

A `decisions` module provides the universal base: id, errand_id, outcome, motivation, decided_by, decided_at. That's enough for an inbox-style "what decisions exist on this errand" view across all types.

Type modules extend by owning their own `errand_<type>_decision_detail` table with FK to `decision.id`. The type-specific resource adds an endpoint like `GET /errands/fostercare/{errandId}/decisions/{decisionId}` returning the base record + type detail.

Same composition pattern as the envelope itself — base + per-type extension, no inheritance. Universal `DecisionRecorded` event lives in the decisions module; type modules can emit their own type-specific decision events on top.

### D3 — externalTags ✅ DECIDED — KILL IT

Delete. Add it back later if a concrete need surfaces. If/when it returns, it should be a proper `labels` module with typed semantics, not free-form k-v.

### D4 — Versioning the type `data` schema ✅ DECIDED — (b) additive evolution, WSO2 owns versioning

- Type module OpenAPI specs evolve **additively only**. New fields default to optional. Frontend ignores unknown fields.
- **WSO2 owns endpoint versioning.** The service exposes a single current schema per type. WSO2 handles `/v1/...` → `/v2/...` routing if a hard break ever happens. We don't carve version numbers into our internal paths.
- No `dataSchemaVersion` field needed in responses — WSO2 sees that information.
- Breaking change protocol: deprecate the field in OpenAPI → coordinate with frontend → WSO2 introduces a new version mapping → eventually drop the field.

### D5 — Cross-type listing / search ✅ DECIDED — YES, build the inbox

`GET /{municipalityId}/{namespace}/errands` returns a page of envelopes (data-free), filterable via spring-filter on envelope columns. Supports the "show me all my errands" inbox view.

- Filterable fields: `typeSlug`, `status`, `assignedUserId`, `reporterUserId`, `priority`, `created`, `touched`, `errandNumber`
- Sortable by `touched` (default), `created`, `priority`
- Response includes envelope shape only — no type-specific `data`
- Frontend's inbox renders per-row using `typeSlug` to pick the right icon/label

If a user wants the full picture of one errand they click through to `/errands/{typeSlug}/{errandId}` which hits the type module and returns envelope + data.

### D6 — Operaton BPMN integration ✅ DECIDED — per type, deploy-time concern

- `processDefinitionName` / `processInstanceId` stay on the envelope as **optional**. Types that don't use Operaton leave them null.
- BPMN definitions are deployed alongside the type module but managed at deploy time (Operaton config, not Java code).
- Type modules that DO use Operaton subscribe to BPMN events via `@ApplicationModuleListener` to react to task assignments, etc.
- Core's `operaton` integration package handles the generic glue. Type-specific process logic stays in the type module.

---

## Phased migration plan

Current CareM is not yet in production-heavy use (small footprint, fresh repo). That's a gift — we can do a hard pivot without preserving the parameter swamp.

### Phase 0 — alignment (this doc)

Land the proposal. Decide D1–D6. Write a decision log.

### Phase 1 — scaffold the Modulith skeleton (1–2 days)

- Add `spring-modulith` deps to pom
- Restructure existing packages into `core/`, `stakeholders/`, `attachments/`, `notifications/`
- Add `@Modulithic` + `@ApplicationModule` declarations
- Land the boundary verification test (it WILL fail at first, that's the point — surface the coupling)

### Phase 2 — kill the swamp (1 week)

- Delete `ParameterEntity`, `StakeholderParameterEntity`, `ErrandParameterResource`, `StakeholderParameterResource`
- Drop `parameters` from `ErrandEntity`
- Drop `category` from `ErrandEntity`
- Drop `parameters` from `StakeholderEntity`
- Flyway down-migration to drop the tables
- Update tests

### Phase 3 — first type module (1 week)

- Scaffold `types/fostercare` end-to-end:
  - `FosterCareEntity` + `errand_fostercare` table + Flyway
  - `FosterCareData` DTO with OpenAPI spec
  - `FosterCareResource` for typed endpoints
  - Role contribution bean
  - Notification trigger contribution
  - Module-level test using `AbstractAppTest`
- Adjust core `ErrandResource` to handle `POST /errands/{typeSlug}`

### Phase 4 — universal modules harden

- Add status history module
- Add notes module
- Add notification trigger registry
- Add stakeholder role registry
- Event catalog goes live; Modulith event log enabled

### Phase 5 — stamp out more types

With one type module as the template, additional types are a copy-paste-and-customize job. ~1–2 days per type for typical complexity.

---

## What this gets us

|                     Pain                     |                              Before                               |                       After                       |
|----------------------------------------------|-------------------------------------------------------------------|---------------------------------------------------|
| "What fields does foster-care actually use?" | Grep `parameters[i].key == "..."` across the codebase             | Read `FosterCareData.java`                        |
| Adding a new type                            | Add enum value, add new `parameters` keys, hope frontend keeps up | New module, OpenAPI codegen gives frontend types  |
| Adding a new stakeholder role                | Pray nobody hardcoded the old set                                 | Add to role contribution bean                     |
| Type modules accidentally coupling           | Inevitable                                                        | Build fails                                       |
| Renaming a "parameter" key                   | Free-text find/replace + hope                                     | Refactor a typed field, compiler catches all uses |
| Notification logic for type X                | Service method with switch on errand type                         | Notification trigger bean in module X             |
| Frontend type safety                         | Free-form keys, runtime guessing                                  | Generated TS types per endpoint                   |

---

## What this costs us

Honest about the trade-offs:

- **More files.** 50 type modules = 50 sets of entity/resource/service/mapper. Mitigated by good scaffolding (see scaffold task next).
- **OpenAPI generation per type.** Build complexity goes up. Worth it for frontend type safety.
- **Modulith learning curve.** Team needs to grok `@ApplicationModuleListener` and event-driven flow. Pays back fast.
- **Migration churn.** Phase 2 will produce a chunky PR. Coordinated with frontend.
- **No more "just add a parameter" escape hatch.** This is the point. The friction is the feature.

---

## Next deliverables (after this doc lands)

1. Boundary verification test scaffold in current CareM (proves Modulith works in our setup)
2. Actual `carem-core` + `types/fostercare` scaffold as runnable code
3. Frontend coordination doc (what their generated client needs to look like)

---

## Appendix A — example module config

```java
// types/fostercare/src/main/java/.../types/fostercare/FosterCareModuleConfig.java
package se.sundsvall.caremanagement.types.fostercare;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.sundsvall.caremanagement.core.api.registry.ErrandTypeContribution;
import se.sundsvall.caremanagement.stakeholders.api.RoleDefinition;
import se.sundsvall.caremanagement.stakeholders.api.StakeholderRoleContribution;

@Configuration
class FosterCareModuleConfig {

    static final String TYPE_SLUG = "fostercare";

    @Bean
    ErrandTypeContribution fosterCareType() {
        return ErrandTypeContribution.builder(TYPE_SLUG)
            .displayName("Familjehemsplacering")
            .allowedStatuses("DRAFT", "OPEN", "ONGOING", "PLACED", "CLOSED")
            .allowedTransition("DRAFT", "OPEN")
            .allowedTransition("OPEN",  "ONGOING")
            .allowedTransition("ONGOING", "PLACED", "CLOSED")
            .allowedTransition("PLACED", "CLOSED")
            .build();
    }

    @Bean
    StakeholderRoleContribution fosterCareRoles() {
        return new StakeholderRoleContribution(TYPE_SLUG, Set.of(
            new RoleDefinition("CHILD",         "Barn",                    1, true),
            new RoleDefinition("FOSTER_PARENT", "Familjehemsförälder",     2, true),
            new RoleDefinition("BIO_PARENT",    "Biologisk förälder",      2, false),
            new RoleDefinition("SOCIAL_WORKER", "Socialsekreterare",       1, true)
        ));
    }
}
```

## Appendix B — example event listener

```java
// statushistory/src/main/java/.../statushistory/StatusHistoryListener.java
@Component
class StatusHistoryListener {

    private final StatusHistoryRepository repository;

    StatusHistoryListener(final StatusHistoryRepository repository) {
        this.repository = repository;
    }

    @ApplicationModuleListener
    void onStatusChanged(final ErrandStatusChanged event) {
        final var entry = StatusHistoryEntity.create()
            .withErrandId(event.errandId())
            .withFromStatus(event.oldStatus())
            .withToStatus(event.newStatus())
            .withChangedBy(event.changedBy())
            .withChangedAt(event.timestamp());
        repository.save(entry);
    }
}
```

`@ApplicationModuleListener` is transactional + async by default in Modulith. Event is persisted to the outbox; if the listener fails, it's retried.

---

**End of proposal.** Comments / pushback welcome. Once D1–D6 are decided, we scaffold.
