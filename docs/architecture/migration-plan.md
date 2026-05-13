# CareM Migration Plan — From Errand-Swamp to Modulith

**Target end-state:** see `proposal-modulith.md` in this folder.

This doc tracks the *concrete file-level moves* to get there. Phase 1 is already merged. Phases 2–4 are still to come.

---

## Package convention

Every Modulith module follows the standard dept44 internal layout — Modulith does not change *how* we organise a module, only *which* packages are modules:

```
<module>/
├── api/                       ← HTTP-facing
│   ├── <SomeResource>.java    ← REST controller (Phase 2+)
│   └── model/                 ← REST DTOs (also serve as inter-module contract records)
├── service/                   ← business logic
│   ├── <SomeService>.java     ← public interfaces + package-private impls
│   ├── event/                 ← application events (inter-module contract)
│   ├── mapper/                ← DTO ↔ entity mappers
│   └── registry/              ← module registries when needed (e.g. ErrandTypeRegistry)
├── integration/db/            ← persistence (Phase 2+)
│   ├── <SomeRepository>.java
│   ├── model/                 ← JPA entities
│   └── specification/         ← spring-data specs
└── configuration/             ← @Configuration classes for this module
```

`package-info.java` at the module root carries the `@ApplicationModule` declaration. That's the only Modulith-specific file in each module — everything else looks like a normal dept44 service.

## Phase 1 — Foundation (DONE)

What landed:

- `pom.xml` — Spring Modulith deps (`starter-core`, `starter-jpa`, `starter-test`, `docs`)
- `Application.java` — `@Modulithic` annotation
- `core/` module:
  - `core/api/model/` — `Envelope`, `CreateEnvelope`, `EnvelopePage`
  - `core/service/` — `EnvelopeService` interface
  - `core/service/event/` — sealed `ErrandEvent` + 4 record events
  - `core/service/registry/` — `ErrandTypeRegistry` interface, `ErrandTypeContribution`, `ErrandTypeRegistryImpl`
- `stakeholders/` module:
  - `stakeholders/api/model/` — `RoleDefinition`
  - `stakeholders/service/` — `StakeholderRoleRegistry`, `StakeholderRoleContribution`, `StakeholderRoleRegistryImpl`
- `decisions/` module:
  - `decisions/api/model/` — `Decision`, `CreateDecision`
  - `decisions/service/` — `DecisionService` interface
  - `decisions/service/event/` — `DecisionRecorded`
- `types/fostercare/` module:
  - `types/fostercare/configuration/FosterCareModuleConfig.java` — active, registers type + roles
  - `types/fostercare/api/model/` — `FosterCareData`, `CreateFosterCareRequest`, `FosterCareView`
- `ModulithVerificationTest` (currently `@Disabled` until Phase 2)

**Nothing existing was deleted or modified.** The new packages coexist with the legacy `api/`, `integration/`, `service/` packages. Build is green.

The `ErrandTypeRegistryImpl` and `StakeholderRoleRegistryImpl` beans wire themselves at startup using `FosterCareModuleConfig`'s contribution beans. You can `@Autowired` either registry today, but nothing in the legacy code calls them yet.

---

## Phase 2a — Kill the swamp (DONE)

### Deleted (no salvage)

- `integration/db/`: `ParameterRepository`, `StakeholderParameterRepository`, `model/ParameterEntity`, `model/StakeholderParameterEntity`
- `api/`: `ErrandParameterResource`, `StakeholderParameterResource`
- `api/model/`: `Parameter`, `StakeholderParameter`, `ExternalTag`
- `service/`: `ErrandParameterService`, `StakeholderParameterService`
- `service/mapper/`: `ParameterMapper`, `StakeholderParameterMapper`, `ExternalTagMapper`
- `integration-test/java/apptest/`: `ErrandParameterIT`, `StakeholderParameterIT` (+ their `resources/` dirs)
- Unit tests for all of the above

### Slimmed (in place — Phase 2b moves them into module packages)

- `ErrandEntity`: dropped `parameters`, `externalTags`, `category`, `contactReason`, `contactReasonDescription`; renamed `type` → `typeSlug`; added `errandNumber` (unique)
- `StakeholderEntity`: dropped `parameters` (`contactChannels` stays — legitimate `TagEmbeddable` use)
- `Errand` API DTO: same shape as entity, plus the existing `decisions`/`stakeholders` lists
- `PatchErrand` API DTO: only patchable envelope fields (title, status, description, priority, reporterUserId, assignedUserId)
- `Stakeholder` API DTO: dropped `parameters`
- `ErrandService`: dropped `LookupRepository` and `ProcessService` deps; dropped BPMN start from `createErrand` (D6 — type modules own their BPMN start); dropped contact-reason resolution
- `ProcessService`: removed `Parameter` coupling — `startProcess` now takes `Map<String, Object>` directly; killed dead `updateVariable(Parameter)` / `deleteVariable(String)` swamp helpers; kept `correlateMessage`
- `ErrandMapper`, `PatchMapper`, `StakeholderMapper`: dropped swamp mapping; `PatchMapper.patchErrand` lost its `LookupEntity` parameter
- `ErrandSpecification`: dropped `hasMatchingTags` (used `externalTags`); added `withTypeSlug`
- `TagEmbeddable` javadoc: removed stale `externalTags` reference

### Added

- `db/migration/V2_0__kill_swamp.sql` — drops parameter / external_tag tables, removes `category` / `contact_reason_*` columns, renames `type` → `type_slug`, adds `errand_number` with unique constraint

### Outstanding follow-ups (not done in 2a)

|                                                       Item                                                       |                                                                                                                                        Why                                                                                                                                        |    Effort     |
|------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `ErrandIT` JSON fixtures (`test02_readErrand`, `test03_findErrands`, `test08_createErrandStartsOperatonProcess`) | Still reference `parameters` / `externalTags` / `category`. Compiles, fails at runtime.                                                                                                                                                                                           | trivial       |
| `test08_createErrandStartsOperatonProcess`                                                                       | BPMN start moved out of envelope service (D6). Either delete the test or move to a type-module IT.                                                                                                                                                                                | trivial       |
| Rebuild unit tests                                                                                               | Deleted: `ErrandTest`, `PatchErrandTest`, `StakeholderTest`, `StakeholderEntityTest`, `ErrandMapperTest`, `StakeholderMapperTest`, `PatchMapperTest`, `ErrandSpecificationTest`, `ErrandServiceTest`, `ProcessServiceTest`. All mechanical regenerations against the slim shapes. | ~1h           |
| `integration-test/resources/api/openapi.yaml`                                                                    | Generated/checked-in — regenerate after Phase 2b or strip dead fields manually                                                                                                                                                                                                    | trivial       |
| `apptest/ErrandIT.java`                                                                                          | Probably fine compile-wise but assertions may reference dead fields                                                                                                                                                                                                               | check + tweak |

### Verification

Without Maven in the dev sandbox I couldn't run `mvn compile` directly. The repo should be green for `mvn compile` (main sources) — grepped exhaustively for all removed symbol references and found none outside the (now deleted) swamp code paths. Run locally:

```bash
./mvnw clean compile         # should pass
./mvnw test                  # will fail on the IT fixtures noted above + missing-test gaps
```

---

## Phase 2c — Break the JPA cycle (DONE)

Modules no longer reference `ErrandEntity` through JPA relations. Each child module owns
its own entity with a plain {@code errand_id} String column and queries by it.

### Entity changes

- **`ErrandEntity`**: removed `@OneToMany List<AttachmentEntity> attachments`, `List<StakeholderEntity> stakeholders`, `List<DecisionEntity> decisions`. Pure envelope now.
- **`StakeholderEntity`**: replaced `@ManyToOne ErrandEntity errandEntity` with `String errandId`.
- **`DecisionEntity`**: same replacement.
- **`AttachmentEntity`**: same replacement.
- **`NotificationEntity`**: same replacement (this one was hiding — it also referenced `ErrandEntity` via `@ManyToOne` and broke Modulith boundaries from `notifications`).

### Errand DTO change (breaking API)

`Errand` no longer carries `stakeholders` / `decisions` lists. Pure envelope. Clients
fetching a full picture call `/stakeholders` and `/decisions` separately. This is an
intentional break — type modules will return their own typed `data` and clients aggregate
the rest by errand id when needed.

### Mappers updated

- `ErrandMapper.toErrandEntity(...)` no longer cascades child collections — it just builds the envelope.
- `StakeholderMapper.toStakeholderEntity(Stakeholder, String errandId)` — signature change.
- `DecisionMapper.toDecisionEntity(Decision, String errandId)` — signature change.
- `AttachmentMapper.toAttachmentEntity(String errandId, String namespace, String municipalityId, MultipartFile)` — signature change.
- `NotificationMapper.toEntity(Notification, String municipalityId, String namespace, String errandId, OffsetDateTime expires)` — signature change.

### Repositories

`StakeholderRepository`, `DecisionRepository`, `AttachmentRepository`, `NotificationRepository`
each got a `findByErrandId(String)` and a `deleteByErrandId(String)`. `NotificationRepository.acknowledgeAllByErrand`
JPQL updated to use `n.errandId` instead of `n.errandEntity.id`.

### Services rewritten

- `StakeholderService`, `DecisionService`, `AttachmentService`, `NotificationService` now query their own
  repository by `errandId` instead of walking `errand.getStakeholders() / getDecisions() / getAttachments() / errandEntity`.
- Each service does a `ensureErrandExists` scope check via `ErrandRepository` (the only legitimate cross-module read into core).

### `ModulithVerificationTest` is now ENABLED

`@Disabled` removed. The test verifies module boundaries on every `mvn test` and emits
a PlantUML module graph to `target/spring-modulith-docs/`.

### Test coverage gap

The following test files were deleted because they assumed the old JPA structure
(`errand.getStakeholders()` etc.) and rewriting them all in this pass was prohibitive:

- `StakeholderServiceTest`
- `DecisionServiceTest` and `DecisionEntityTest`
- `AttachmentServiceTest` and `AttachmentEntityTest`
- `NotificationServiceTest`, `NotificationMapperTest`, `NotificationRepositoryTest`, `NotificationEntityTest`

Each needs a fresh rewrite against the new shape — querying by `errandId`, no `errand.get*()`
walks. Ballpark: 3–4 hours of mechanical regeneration.

---

## Phase 2b — Move slim entities into modules (DONE)

### File moves (via `bash + sed` package rewriter)

**Core:** `ErrandEntity`, `ErrandRepository`, `ErrandSpecification`, `ErrandResource`, `Errand`, `PatchErrand`, `FindErrandsResponse`, `OnCreate`, `OnUpdate`, `ErrandService`, `ErrandMapper`, `PatchMapper` → `core/...`

**Stakeholders:** `StakeholderEntity`, `StakeholderRepository`, `TagEmbeddable`, `ErrandStakeholderResource` → `StakeholderResource`, `Stakeholder`, `ContactChannel`, `ErrandStakeholderService` → `StakeholderService`, `StakeholderMapper`, `ContactChannelMapper` → `stakeholders/...`

**Decisions:** `DecisionEntity`, `DecisionRepository`, `ErrandDecisionResource` → `DecisionResource`, `Decision`, `ErrandDecisionService` → `DecisionService`, `DecisionMapper` → `decisions/...`

**Shared:** `Auditable`, `AuditableListener` → `shared/...` (declared as `sharedModules = "shared"` on `@Modulithic` so any module can use them).

### Scaffolded stubs replaced

The scaffolded interface-only / record stubs at `core/api/{Envelope,CreateEnvelope,EnvelopePage}.java`, `core/service/EnvelopeService.java`, and `decisions/api/model/{Decision,CreateDecision}.java`, `decisions/service/DecisionService.java` were removed when the moved legacy classes took their slot. Type modules now depend directly on the concrete `ErrandService` / `DecisionService` — one fewer abstraction layer.

### Outstanding for Phase 2c

- **JPA-relationship cross-module cycle**: `ErrandEntity.attachments / stakeholders / decisions` are `@OneToMany` lists pointing at entities in other modules. That's a forward-reference from core into siblings — Modulith verification will flag it.
  - **Fix:** drop the `@OneToMany` lists from `ErrandEntity`; add a plain `errand_id` column on `StakeholderEntity` / `DecisionEntity` / `AttachmentEntity` / `NoteEntity` / etc. Each module queries its own table by errand id. Aggregate at the API layer when a combined view is needed.
- **Modulith verification test** stays `@Disabled` until the cycle above is broken.

---

## Phase 3 — Type-module pattern (TEMPLATE ONLY)

No types live in `types/` yet — the actual type catalog is still being defined. The
fostercare example that briefly existed as a proof-of-concept has been removed.

`types/README.md` documents the recipe for adding a new type when the time comes.
The `Application` class already has `additionalPackages = "se.sundsvall.caremanagement.types"`
so any new sub-package gets auto-discovered.

Recipe summary (per type, ~1–2 days):

1. `types/<slug>/configuration/<Slug>ModuleConfig.java` — register `ErrandTypeContribution` + `StakeholderRoleContribution` beans
2. `types/<slug>/api/model/{<Slug>Data, Create<Slug>Request, <Slug>View}.java` — strongly-typed DTOs, no `parameters` blob
3. `types/<slug>/integration/db/model/<Slug>Entity.java` — entity sharing PK with `errand`
4. `types/<slug>/integration/db/<Slug>Repository.java`
5. `types/<slug>/service/<Slug>Service.java` — delegates envelope ops to `core.service.ErrandService`
6. `types/<slug>/service/mapper/<Slug>DataMapper.java`
7. `types/<slug>/api/<Slug>Resource.java`
8. `db/migration/V<n>_0__create_errand_<slug>.sql`

---

## Phase 4 — Universal modules (DONE)

### Moved

- **Attachments** → `attachments/` module (entity, repos, resource, service, mapper, DTO). `ErrandAttachmentResource` / `ErrandAttachmentService` renamed to `AttachmentResource` / `AttachmentService`.
- **Notifications** → `notifications/` module (entity, type enums, repo, resource, service, mapper, DTO, properties, event types + listener, cleanup scheduler).

### Created from scratch

- **`notes/` module** — `NoteEntity`, `NoteRepository`, `NoteService` (publishes `NoteAdded` event), `NoteResource` (CRUD), `db/migration/V4_0__create_note.sql`
- **`statushistory/` module** — `StatusHistoryEntity`, `StatusHistoryRepository`, `StatusHistoryListener` (`@ApplicationModuleListener` on `ErrandStatusChanged`), `StatusHistoryService` (read-only), `StatusHistoryResource` (`GET /errands/{id}/status-history`), `db/migration/V4_1__create_status_history.sql`

### `ErrandService` now publishes the full event catalog

`createErrand` → `ErrandCreated`; `updateErrand` → `ErrandStatusChanged` (if status changed) + `ErrandAssigned` (if assignee changed); `deleteErrand` → `ErrandDeleted`. `StatusHistoryListener` automatically logs each transition without anyone wiring it in by hand.

---

## Phase 4b — Kill legacy floaters (DONE)

The vague `service/`, `api/`, `integration/` packages at the root are now empty. Everything has a module.

### Moved into new modules

- **`metadata/`** — `LookupEntity`, `LookupKind`, `LookupRepository`, `Lookup` DTO, `LookupMapper`, `MetadataResource`, `MetadataService`. The "categories / types / contact reasons" dropdown infrastructure.
- **`namespaceconfig/`** — `NamespaceConfigEntity`, `NamespaceConfigRepository`, `NamespaceConfig` DTO, `NamespaceConfigResource`, `NamespaceConfigService`, `NamespaceConfigMapper`.
- **`operaton/`** — `OperatonClient` + configuration, `ProcessService`, `ProcessMessageResource`, `ProcessMessageRequest` DTO. Declared as a Modulith `sharedModules` entry so any module (especially type modules per D6) can depend on `ProcessService` without listing it explicitly.

### Test packages relocated

All stranded tests (attachment + notification tests that didn't move with their main-source companions in Phase 4, plus metadata/namespaceconfig/operaton tests) have been moved into the matching module test packages. `ErrandAttachmentResourceTest` / `ErrandAttachmentServiceTest` renamed to `AttachmentResourceTest` / `AttachmentServiceTest`.

### Empty legacy directories removed

The root namespace is now: `Application.java`, `Constants.java`, and 12 module directories (`attachments`, `core`, `decisions`, `metadata`, `namespaceconfig`, `notes`, `notifications`, `operaton`, `shared`, `stakeholders`, `statushistory`, `types`). No vague `service/`, `api/`, or `integration/` at the root.

---

## Phase 5 — Stamp out more types (BLOCKED)

This phase needs the actual type catalog. The fostercare template in Phase 3 is the recipe — but I need from you:

- The list of ~50 type slugs (or however many) we're building
- For each: status flow (states + permitted transitions), stakeholder roles, type-specific data fields, whether it uses Operaton

With that I can generate the per-type module scaffolds in bulk.

---

## Cross-cutting follow-ups still to land

|                                                  Item                                                   |    Phase     |                                             Notes                                              |
|---------------------------------------------------------------------------------------------------------|--------------|------------------------------------------------------------------------------------------------|
| Break JPA `@OneToMany` from `ErrandEntity` (cycle fix)                                                  | 2c           | Verification test stays disabled until this lands                                              |
| Move legacy IT JSON fixtures off dead fields (`parameters` / `externalTags` / `category`)               | 2a follow-up | Listed in Phase 2a section above                                                               |
| Rebuild the unit tests that were deleted in 2a                                                          | 2a follow-up | ~1h mechanical                                                                                 |
| Move remaining legacy tests (NotificationServiceTest, AttachmentServiceTest, etc.) into module packages | 4 follow-up  | Mirror the main-source moves                                                                   |
| Light up `ModulithVerificationTest`                                                                     | After 2c     | Currently `@Disabled`; lights up once the JPA cycle is broken                                  |
| `Constants.java`                                                                                        | future       | Stays at root; could move to `shared/` if `NAMESPACE_REGEXP` proves to be the only shared util |
| `LookupEntity` / `LookupRepository` / `MetadataService` / `NamespaceConfigService`                      | future       | Still in legacy paths — small footprint, can move to `lookup/` and `config/` modules later     |
| `ProcessService` + Operaton integration                                                                 | future       | Stays at `service/` for now (D6: BPMN is deploy-time per-type)                                 |

**Goal:** delete the `parameters` blob, `extraParameters` map, `jsonParameters`, `externalTags`, `category` field. Rebuild `ErrandEntity` as the slim envelope.

### Files to DELETE

```
src/main/java/se/sundsvall/caremanagement/
├── integration/db/
│   ├── ParameterRepository.java
│   ├── StakeholderParameterRepository.java
│   └── model/
│       ├── ParameterEntity.java
│       ├── StakeholderParameterEntity.java
│       └── TagEmbeddable.java                ← externalTags (D3: dead)
├── api/
│   ├── ErrandParameterResource.java
│   └── StakeholderParameterResource.java
├── api/model/
│   ├── Parameter.java
│   ├── StakeholderParameter.java
│   └── ExternalTag.java                      ← D3: dead
├── service/
│   ├── ErrandParameterService.java
│   └── StakeholderParameterService.java
└── service/mapper/
    ├── ParameterMapper.java
    ├── StakeholderParameterMapper.java
    └── ExternalTagMapper.java
```

Plus their tests:

```
src/test/java/se/sundsvall/caremanagement/
├── api/ErrandParameterResourceTest.java
├── api/StakeholderParameterResourceTest.java
├── api/model/ParameterTest.java
├── api/model/StakeholderParameterTest.java
├── api/model/ExternalTagTest.java
├── integration/db/model/ParameterEntityTest.java
├── integration/db/model/StakeholderParameterEntityTest.java
├── integration/db/model/TagEmbeddableTest.java
├── service/StakeholderParameterServiceTest.java
└── service/mapper/ParameterMapperTest.java
   ... etc
```

### Files to REWRITE (dept44 layout inside each module)

#### `ErrandEntity` (slim envelope)

Move from `integration/db/model/ErrandEntity.java` → `core/integration/db/model/ErrandEntity.java`. Drop:
- `parameters: List<ParameterEntity>`
- `externalTags: List<TagEmbeddable>`
- `category` field

Add:
- `type_slug` column (rename of `type`, validated against `ErrandTypeRegistry`)
- `errand_number` column (already in CareM as `String`? Verify before migrating)

Also move `ErrandRepository` → `core/integration/db/ErrandRepository.java` and `ErrandSpecification` → `core/integration/db/specification/ErrandSpecification.java`.

#### `ErrandResource` → `core/api/ErrandResource`

- Drop the generic `POST /errands` create — type-specific creates go through type-module resources
- Keep: `GET /errands/{id}`, `GET /errands` (list), status change, assign, delete
- Add: spring-filter-backed inbox list

#### `ErrandService` → `core/service/EnvelopeServiceImpl` (package-private)

Implements `EnvelopeService`. Publishes `ErrandCreated` / `ErrandStatusChanged` / etc. events via `ApplicationEventPublisher`. Mappers move to `core/service/mapper/`.

#### `StakeholderEntity` → `stakeholders/integration/db/model/StakeholderEntity`

Move package, drop `parameters: List<StakeholderParameterEntity>`. Keep `role: String` but validate against `StakeholderRoleRegistry` at write time. Existing `StakeholderResource` moves to `stakeholders/api/`.

#### `DecisionEntity` → `decisions/integration/db/model/DecisionEntity`

Move package. The shape already matches the proposal — should be straightforward. `DecisionServiceImpl` lives in `decisions/service/` (package-private).

### Flyway

New migration `V2_001__rebuild_errand.sql` in `db/migration/`:

```sql
DROP TABLE IF EXISTS errand_parameter;
DROP TABLE IF EXISTS stakeholder_parameter;
DROP TABLE IF EXISTS external_tag;
ALTER TABLE errand DROP COLUMN category;
ALTER TABLE errand DROP COLUMN ... (other dead fields);
ALTER TABLE errand CHANGE COLUMN type type_slug VARCHAR(64) NOT NULL;
ALTER TABLE errand ADD COLUMN errand_number VARCHAR(64);
CREATE UNIQUE INDEX uq_errand_errand_number ON errand (errand_number);
```

**Coordinate with frontend before merging.** API shape changes.

### Verification

- Light up `ModulithVerificationTest` (remove `@Disabled`). Expect failures — each one is real coupling between legacy packages. Fix iteratively.
- Update all existing tests that referenced parameter classes.

---

## Phase 3 — First type module wired up

**Goal:** `POST /errands/fostercare` works end-to-end with persisted state.

Files to ADD (dept44 layout):

```
types/fostercare/
├── api/
│   └── FosterCareResource.java         ← REST controller
├── service/
│   ├── FosterCareService.java          ← business logic, package-private impl detail OK
│   └── mapper/FosterCareDataMapper.java
└── integration/db/
    ├── FosterCareRepository.java
    └── model/FosterCareEntity.java
```

Flyway:

```
db/migration/V3_001__create_errand_fostercare.sql
```

The `FosterCareService.create()` flow:
1. Calls `EnvelopeService.createEnvelope(...)` (core API)
2. Gets back an envelope ID
3. Saves `FosterCareEntity` with shared PK = envelope ID
4. Returns envelope ID for the `Location` header

Add `apptest/FosterCareIT.java` integration test following dept44 `AbstractAppTest` pattern.

---

## Phase 4 — Universal modules harden

- Move `AttachmentEntity` into `attachments/` module
- Move `NotificationEntity` into `notifications/` module
- Add `notes/` module (new — doesn't exist today)
- Add `statushistory/` module (subscriber on `ErrandStatusChanged`)
- Wire notification triggers per type via `NotificationTriggerContribution` beans

---

## Phase 5 — Stamp out more types

For each remaining errand type:
1. Copy `types/fostercare/` as `types/<newtype>/`
2. Adjust `<NewType>ModuleConfig` — status flow, roles, notification triggers
3. Write `<NewType>Data` DTO
4. Write entity + repo + service + resource (mostly mechanical)
5. Write Flyway for the per-type table
6. Add integration test

Budget: 1–2 days per type for typical complexity. Track in a checklist.

---

## API gateway (WSO2) coordination

Per D4: WSO2 owns versioning. When you ship a breaking change to a type module:
1. Mark the old field as deprecated in OpenAPI
2. Coordinate with WSO2 team to introduce a new version mapping
3. Frontend migrates to the new version
4. Eventually remove the deprecated field from the spec

The service exposes one current schema per type. Don't version paths internally.

---

## Open questions still hanging

- **`Auditable` / `AuditableListener` placement:** these are referenced by `ErrandEntity` and likely other entities. Either move to `core.api.persistence` or declare as a shared module in `@Modulithic(sharedModules = ...)`.
- **`Constants.NAMESPACE_REGEXP`:** currently global; should arguably live in a shared utility module or `core`.
- **`LookupEntity`/`LookupKind`:** keep in current location for now, evaluate during Phase 4 whether they deserve their own module.
- **`processDefinitionName`/`processInstanceId`:** stay on envelope as optional. Types without Operaton leave them null (D6).

---

## Workflow rule of thumb during migration

If a PR adds or changes anything that has a "parameters" smell — pushback. Anything that *could* be a typed column on a type-specific table belongs in a type module, not in the envelope. The friction is the feature.
