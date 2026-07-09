# Test Findings Tracking

Original review docs moved out of the Maven project:

`/private/tmp/api-service-caremanagement-Testfindings`

This file tracks which findings have been handled in the codebase.

## Done

- `configurationtest-pattern-findings.md`
  - Switched `FinancialAidConfigurationTest` to `AssertionsForClassTypes.assertThat`.
- `propertiestest-pattern-findings.md`
  - Converted citizen, financial aid, messaging, templating, Lifecare, and message archive properties tests to Spring-bound `@SpringBootTest` tests.
  - Renamed those methods to `testProperties`.
- `integrationtest-pattern-findings.md`
  - Added exact `ThrowableProblem.detail` assertions in `FinancialAidIntegrationTest`, `LifecareFcIntegrationTest`, and `TemplatingIntegrationTest`.
  - Extracted `MUNICIPALITY_ID` in `TemplatingIntegrationTest`.
- `schedulertest-pattern-findings.md`
  - Tightened notification cleanup cutoff assertions and added `verifyNoMoreInteractions`.
  - Added database cleanup failure-path coverage proving foreign key checks are restored after `executeBatch()` failure.
- `listenertest-pattern-findings.md`
  - Renamed the snake_case listener test methods.
  - Added `verifyNoMoreInteractions` to referral and permit errand-deleted listener tests.
- `mappertest-pattern-findings.md`
  - Added field-completeness assertions to straightforward mapper happy paths across the touched mapper suites.
- `entitytest-pattern-findings.md`
  - Rewrote the fully hand-rolled entity tests for permit, referral, and financial assistance.
  - Added reflective equals/toString coverage to the remaining entity tests, with intentional exclusions where production behavior requires it.
- `plainmodeltest-pattern-findings.md`
  - Replaced `org.hamcrest.core.AllOf.allOf` imports with `org.hamcrest.CoreMatchers.allOf`.
  - Added beanmatcher/no-dirt constructor coverage to the mutable hand-rolled API model tests called out by the review.
  - Renamed API model test methods to the `test...` convention.
- `resourcetest-pattern-findings.md`
  - Added response `Content-Type` and exact `Location` assertions to create happy-path `*ResourceTest` calls.
  - Fixed a wrong `Location` assertion in `MetadataResourceTest.createLookup` surfaced once the Spring tests could actually run: the endpoint intentionally appends `?kind=<kind>` to the created-location, so the assertion now expects `.../metadata/{name}?kind={kind}`.
- `servicetest-pattern-findings.md`
  - Added exact exception message assertions beside all `ThrowableProblem` service-test status assertions.
- `specificationtest-pattern-findings.md`
  - Replaced the mocked criteria white-box `ErrandSpecificationTest` with `ErrandRepositoryTest`, which saves real `ErrandEntity` rows and verifies the specifications through `ErrandRepository`.
  - Moved the `ErrandQueryService` selection predicate into `ErrandSpecification.selection(...)` and removed the remaining mocked criteria assertion from `ErrandQueryServiceTest`.
  - Fixed `ErrandRepositoryTest` (also surfaced once it could run): it manually set entity `id`s, so Spring Data's `isNew()` returned false and `saveAll` did `merge()` → `ObjectOptimisticLockingFailureException`. Now lets `@UuidGenerator` assign ids and asserts on the stable `errandNumber`. Also, `AuditableListener@PrePersist` stamps `created=now`, so seeded created-dates are re-applied after `saveAll` and before `flush` for the created-range case.
- `resourcefailuretest-pattern-findings.md` (Prio 1 — the only correctness item)
  - All 30 `*ResourceFailureTest` classes (214 tests) now assert the **exact** `Violation(field, message)` tuple(s) via the `assertConstraintViolation(body, tuple(...))` overload, replacing the loose non-empty-only helper. Ground truth was captured by running the full failure suite against real Testcontainers MariaDB.
  - Fixed 9 tests that were failing because they asserted `ConstraintViolationProblem` on endpoints that actually return a plain `400 Bad Request` (missing required param/header/part, or unreadable JSON): `FinancialAssistanceErrand.createErrand_malformedRequestJson`, `FinancialAssistanceIntake.archiveToActualisation_missingFile`, `FinancialAssistanceLifecare.readDocumentContent_missingPartyId`, `Metadata.createLookup_missingKind` + `readLookups_missingKind`, `Notification.readNotificationsByOwner_missingOwnerId`, `Message.{post,unreadCount,markRead}_missingIdentifier`. These now assert `title`/`status`/`detail` via jsonPath.

## Decided against

- `resourcefailuretest-pattern-findings.md` Prio 2 — FA resource base-class unwind. **Tried, measured, reverted.** The review flags `AbstractFinancialAssistanceResourceTest` as a deviation from support-management's no-base-class canon. We actually performed the unwind (deleted the base, made all 16 FA `*ResourceTest`/`*ResourceFailureTest` free-standing with their own `@SpringBootTest` + focused `@MockitoBean` sets) and ran the full suite: it OOM'd — `java.lang.OutOfMemoryError: Java heap space` on context startup, cascading into 34 context-load errors across resource tests. Root cause: each `@SpringBootTest` context is keyed by its full `@MockitoBean` set, so free-standing FA tests spawn one heavy full-app context per distinct mock set and exhaust the 1 GB surefire heap. The shared base collapses them to a single cached context. Reverted to `extends AbstractFinancialAssistanceResourceTest`; full suite green again. The review itself rates Prio 2–4 as pure convention (no correctness value); the measured OOM decisively outweighs it. Prio 3 (single `INVALID` literal) and Prio 4 (camelCase method names) are cosmetic-only and were likewise left as-is to avoid churn.

## Verification Notes

- `mvn -q compiler:compile compiler:testCompile` passes after the latest changes.
- Targeted non-Spring Surefire runs passed with the Mockito agent, including:
  - `*EntityTest`
  - `*MapperTest`
  - service-test batches covering document, journal, note, message, metadata, namespace config, referral, permit, errand, attachment, form snapshot, process, conversation SPI, decision, stakeholder, errand type, status history, and financial assistance services.
- `mvn -q -DskipTests package` passes after formatting and the final specification refactor.
- Spring resource tests now run locally against Testcontainers MariaDB (Docker via colima) with `TESTCONTAINERS_RYUK_DISABLED=true`. Full `*ResourceFailureTest` suite: `Tests run: 214, Failures: 0, Errors: 0, Skipped: 0`. `dept44-formatting:check` passes.

