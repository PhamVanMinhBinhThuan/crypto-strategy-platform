# F-010 Search Coordinator — Implementation Checkpoint

> **Purpose:** Handoff checkpoint for a new coding agent continuing `speckit-implement`.
>
> **Repository:** `crypto-strategy-platform`
>
> **Feature:** `specs/010-search-coordinator`
>
> **Date:** 2026-09-03
>
> **Primary instruction:** Continue implementing **all remaining unchecked tasks** in `specs/010-search-coordinator/tasks.md`. Only mark a task `[x]` after its implementation, verification, and required evidence are actually complete. Always follow `spec.md`, `plan.md`, architecture tests, ADRs, and Constitution constraints. Do not weaken architecture/tests just to make the build green.

---

## 1. First actions for the next agent

Before editing anything:

```powershell
Get-Content -Raw .agents/skills/speckit-implement/SKILL.md
Get-Content -Raw specs/010-search-coordinator/spec.md
Get-Content -Raw specs/010-search-coordinator/plan.md
Get-Content -Raw specs/010-search-coordinator/tasks.md
git status --short
git diff --check
git rev-parse --abbrev-ref HEAD
git rev-parse HEAD
rg -n "^- \[ \] T" specs/010-search-coordinator/tasks.md
```

Then inspect the exact current diff before changing F-010 code:

```powershell
git diff -- specs/010-search-coordinator
git diff -- modules/search
git diff -- modules/experiment-execution
git diff -- modules/persistence
git diff -- apps/api
git diff -- apps/worker
git diff -- architecture-tests
```

**Do not reset/revert the current working tree.** It contains the implementation accumulated across the previous agents.

---

## 2. Current overall status

The implementation has progressed through the core Search Coordinator runtime, persistence, Worker orchestration, public API gates, crash recovery, stop/deadline races, reproduction workflow, architecture-boundary repair, documentation parity, and benchmark setup.

At the latest known checkpoint:

- **T001–T084 are effectively completed/marked as appropriate**, including the major runtime and architecture work.
- **T083 and T084 were explicitly reported green.**
- **T085 is NOT complete yet.**
- After T085, finish **T086–T088** according to the exact current `tasks.md`.
- A previous summary said `tasks.md` only had **T080–T088** left before T083/T084 were completed. Therefore, **do not trust the summary blindly**—run `rg` against `tasks.md` immediately to see the authoritative unchecked list.

The immediate blocker at handoff is the **full Java 21 quality/full-suite gate for T085**.

---

## 3. Critical current blocker: T085

The previous agent ran the full Java suite and reached this state:

1. **API suite passed** after fixing a Spring bean ambiguity.
2. A **legacy F-008 migration checksum failure** remained.
3. An architecture/quality gate reported **23 raw String ID fields introduced by F-010**.
4. T085 was deliberately **not marked `[x]`** because those issues had not been resolved cleanly.
5. The agent then started mechanically changing IDs, which led to a partially edited state and repeated Gradle/IDE instability.

### Important warning

Do **not** continue blindly from the generated Python scripts such as:

- `fix_ids.py`
- `fix_search_ids.py`
- `fix_ulids.py`
- `rename_search_refs.py`
- `rename_search_refs_2.py`
- the many `fix_*.py` scripts currently untracked

These were ad-hoc repair scripts used during debugging. Some later transformations changed typed IDs to `UUID`, then to renamed `String ...Ref` fields. That direction may conflict with the project's canonical-value architecture rule.

The next agent must inspect the actual production source and architecture test before deciding what the correct model is.

---

## 4. Quality-gate failure that triggered the unfinished ID refactor

The full suite reported fields similar to:

```text
SearchCoordinationCommand.experimentId must use UUID for user identity or a typed domain ULID
SearchCoordinationCommand.searchJobId must use UUID for user identity or a typed domain ULID
SearchCoordinationResult.searchRunId must use UUID for user identity or a typed domain ULID
SearchStartCommandFactory$Request.generatorId must use UUID for user identity or a typed domain ULID

TrustedSearchCoordinationUseCase$CompletionTrigger.backtestJobId ...
TrustedSearchCoordinationUseCase$CompletionTrigger.candidateId ...
TrustedSearchCoordinationUseCase$CompletionTrigger.experimentId ...
TrustedSearchCoordinationUseCase$CoordinationOutcome.searchRunId ...
TrustedSearchCoordinationUseCase$ReconciliationTrigger.experimentId ...
TrustedSearchCoordinationUseCase$StopTrigger.experimentId ...

SearchReproductionGateway$CandidateCopy.backtestJobId ...
SearchReproductionGateway$CandidateCopy.candidateId ...
SearchReproductionGateway$CandidateCopy.sourceCandidateId ...
SearchReproductionGateway$CreateCommand.searchRunId ...
SearchReproductionGateway$CreateCommand.verificationId ...

SearchReproductionVerificationGateway$Completion.verificationId ...
SearchReproductionVerificationGateway$Work.verificationId ...

TrustedSearchCoordinationGateway$Transition.processedMessageId ...

CoordinationDecision.backtestJobId ...
CoordinationDecision.candidateId ...

SearchRun.experimentId ...
SearchRun.searchJobId ...
SearchRun.sourceExperimentId ...
```

The architecture test inspected was:

```text
architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/PurityAndCycleTest.java
```

Specifically inspect:

```text
productionPublicBoundariesUseCanonicalValues
```

### Required approach

Resolve the failure **according to that test and the existing canonical ID conventions**, not by renaming `...Id` to `...Ref` to escape the rule.

Search the repository for existing canonical types and patterns first:

```powershell
rg -n "record .*Id|class .*Id|interface .*Id" modules apps
rg -n "productionPublicBoundariesUseCanonicalValues" architecture-tests
rg -n "must use UUID for user identity or a typed domain ULID" architecture-tests
```

Check which types already exist for:

- experiment ID
- job ID
- candidate ID
- search run ID
- generator ID
- reproduction verification ID
- message ID

If F-010 has its own published typed ULID wrappers (for example `SearchRunId`, `GeneratorId`, etc.), prefer those where the architecture permits. Preserve module dependency direction.

---

## 5. Architecture constraints already discovered and repaired

The architecture suite previously exposed real F-010 violations. These were fixed and must **not regress**.

### 5.1 API boundary

Previously, API code was directly constructing Search models and calling `search.internal`. This violated the dependency matrix/ADR.

The repaired direction is:

```text
API
  -> published experiment-execution API / commands
  -> application service
  -> published search API / generator registry
```

The API should **not** import or instantiate:

```text
com.cryptostrategy.platform.search.internal.*
com.cryptostrategy.platform.execution.internal.*
```

### 5.2 Composition roots

API/Worker were also directly instantiating `execution.internal` services.

This was repaired by moving construction behind a **published module factory** and exposing public ports.

Do not undo this by adding host imports to internal implementation classes.

### 5.3 Allocation ownership

The accepted architecture from previous work is:

- `experiment-execution`: orchestration/application workflow
- `search`: search-space generation + generator guard/registry
- `persistence`: durable authoritative snapshot, frozen context, composite atomic transaction
- `worker`: invokes published ports only
- `api`: maps transport DTO to published application commands only

### 5.4 Reproduction workflow

The old synchronous reproduction flow was considered incompatible with F-010 because the requirement is durable asynchronous reproduction verification.

The new implementation was built around durable application/persistence boundaries. Do not reintroduce synchronous direct reproduction logic.

---

## 6. Completed critical runtime work

The following work was reported complete and verified before the T085 cleanup.

### T043 — finite allocation pipeline / Start gate

Implemented runtime allocator through the correct dependency direction.

Reported behavior:

- application allocator connected to composite transaction
- `search` owns generator guard
- persistence reads frozen context
- persistence commits composite state atomically
- Worker calls only published ports
- finite pipeline test passed
- allocation fence regression passed
- deadline regression passed
- Start gate opened **only after smoke test passed**

### T045 — stop-vs-allocation PostgreSQL race

A PostgreSQL integration test verifies:

- stop and allocation contend on the same version fence
- after stop is durable, later allocation returns `STALE_FENCE`
- Candidate count does not increase after stop

Relevant test:

```text
modules/persistence/src/experimentIntegrationTest/java/
  com/cryptostrategy/platform/persistence/internal/search/
  SearchStopRaceIntegrationTest.java
```

### T046 — kill-point / reclaim recovery

Worker crash/reclaim recovery test completed.

### T047 — queue-loss reconciliation

Queue-loss recovery/reconciliation test completed.

### T048 — bounded retry / dead-letter / redaction

Implemented/tested failure policy including:

- finite retries
- dead-letter behavior
- redaction

### T049 — authoritative completion vs deadline PostgreSQL integration

This is important and caught a real bug.

`SearchDeadlineIntegrationTest` verifies:

- durable frozen deadline survives adapter restart
- authoritative child counts update SEARCH Job
- authoritative completion **on time** ends `COMPLETED`
- completion **after deadline** ends `STOPPED`

A bug was found in late-completion handling:

```text
RUNNING -> STOPPING -> STOPPED
```

The same `messageId` had been claimed for both transitions, so durable dedupe blocked the second transition.

Fix:

- the external message receipt is claimed once
- the internal follow-up transition still uses the version fence
- the internal transition does not claim the same message receipt again

Do not regress this.

### T051 — Trusted Search Coordination

Implemented:

```text
TrustedSearchCoordinationService
TrustedSearchCoordinationGateway
JdbcTrustedSearchCoordinationGateway
```

Expected guarantees:

- decision from authoritative durable snapshot only
- idempotent completion handling
- deadline frozen from initial start
- on-time authoritative completion beats deadline
- late completion moves STOPPING -> STOPPED
- version-fence conflict cannot overwrite newer state
- Candidate/Backtest/Evaluation lineage validated
- authoritative counters loaded
- Search Run + SEARCH Job updated atomically
- durable message deduplication

### T053

Durable reload occurs before fill/complete/stop decisions.

### T055

Scheduled reconciliation derives truth from PostgreSQL.

### T056

`SearchFailureHandler` implemented:

- bounded retry
- dead-letter
- lifecycle failure publication

### T057

Stop gate goes through trusted orchestration port.

### T058

`SearchObservability` implemented with:

- metrics
- sanitized correlation/identity/failure code data

### T059

Crash/restart evidence completed using a real Redis restart plus PostgreSQL stop/deadline verification.

Commands previously used:

```powershell
docker restart crypto-f010-redis
docker exec crypto-f010-redis redis-cli ping

.\gradlew.bat --no-daemon `
  -I .specify\gradle-f010-isolation.init.gradle `
  :apps:worker:test `
  --tests "*SearchCrashRecoveryTest" `
  --tests "*SearchReconciliationTest" `
  --tests "*SearchFailurePolicyTest"

$env:DATABASE_URL='jdbc:postgresql://localhost:54322/crypto_f010'
$env:DATABASE_USERNAME='postgres'
$env:DATABASE_PASSWORD='f010_fixture_password'

.\gradlew.bat --no-daemon `
  -I .specify\gradle-f010-isolation.init.gradle `
  :modules:persistence:experimentIntegrationTest `
  --tests "*SearchStopRaceIntegrationTest" `
  --tests "*SearchDeadlineIntegrationTest"
```

### T078

Coordinator resolves generators only through the public registry using exact ID/version.

### T079

Generator replaceability evidence added to architecture evidence.

---

## 7. US3 / Reproduce state

US3 was reported passing at two layers:

1. **PostgreSQL atomic copy / rollback**
2. **durable comparator** running to `MATCHED`

Public Reproduce smoke test also passed, and the Reproduce gate was opened separately only after that verification.

Preserve the invariant that Start and Reproduce gates are independently justified by their own smoke/evidence.

---

## 8. T083 / benchmark state

A benchmark file exists:

```text
apps/api/src/test/java/com/cryptostrategy/platform/api/performance/
SearchCoordinatorPerformanceTest.java
```

The first version had package-access issues because it tried to access package-private mapper/constructors. This was addressed while also repairing the API boundary.

At the later checkpoint:

- **T083 was reported green**
- benchmark-related changes should be retained unless the current tree proves otherwise

Run the exact task/test from `tasks.md` rather than assuming only this test is sufficient.

---

## 9. T084 / architecture state

**T084 was reported green.**

The architecture suite at that point confirmed:

- API -> experiment-execution direction
- no host import of `search.internal`
- no host import of `execution.internal`

Before final completion, rerun all architecture tests because the unfinished ID edits may have introduced regressions.

Suggested command:

```powershell
.\gradlew.bat --no-daemon -I .specify\gradle-f010-isolation.init.gradle :architecture-tests:test
```

If there is no need for the isolation init script according to the current task definition, run the canonical command from `tasks.md` as well.

---

## 10. Legacy F-008 migration checksum failure

During T085 full-suite execution, a failure occurred in:

```text
modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/news/
NewsMigrationContractTest.java
```

The previous agent classified it as **legacy F-008 checksum migration** rather than core F-010 behavior and edited this test during investigation.

Next agent must:

1. inspect the current diff in that file
2. identify the expected migration checksum source
3. determine whether F-010 legitimately changed the migration set/order or whether the test fixture was stale
4. fix the correct source of truth
5. do **not** disable the checksum assertion just to pass T085

Also inspect migration ordering around:

```text
supabase/migrations/20260903000100_f010_search_coordinator.sql
```

---

## 11. Database / immutable evidence constraint

During `SearchDeadlineIntegrationTest`, cleanup originally attempted to delete F-006 Evaluation evidence.

PostgreSQL correctly rejected this because Evaluation evidence is immutable under the project Constitution.

The test was changed to run scenarios within transaction rollback instead of mutating/deleting evidence.

Rule for future integration tests:

- do not delete immutable evaluation evidence as test cleanup
- prefer transaction rollback / isolated fixture IDs
- any one-time trigger bypass used on a local disposable test container is not production behavior and must not become part of application code

---

## 12. Working tree at handoff

The user supplied this `git status --short` snapshot. Run a fresh status because it may have changed slightly, but treat all of these as part of the active F-010 implementation unless inspection proves otherwise.

### Modified tracked files

```text
M apps/api/build.gradle.kts
M apps/api/src/main/java/com/cryptostrategy/platform/api/config/ExperimentApiConfiguration.java
M apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentController.java
M apps/api/src/main/resources/application.yml
M apps/api/src/test/java/com/cryptostrategy/platform/api/contract/DocumentationParityTest.java
M apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/IdempotencyCommandIntegrationTest.java
M apps/worker/build.gradle.kts
M apps/worker/src/main/java/com/cryptostrategy/platform/worker/config/WorkerProperties.java
M apps/worker/src/main/java/com/cryptostrategy/platform/worker/engine/OutboxPublisherEngine.java
M apps/worker/src/main/java/com/cryptostrategy/platform/worker/infra/redis/RedisStreamTopologyInitializer.java
M apps/worker/src/main/resources/application.yml
M apps/worker/src/test/java/com/cryptostrategy/platform/worker/config/WorkerPropertiesTest.java
M apps/worker/src/test/java/com/cryptostrategy/platform/worker/engine/OutboxPublisherEngineTest.java
M architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/ModuleBoundaryTest.java
M docs/api/error-catalog.md
M docs/api/examples.md
M docs/api/openapi.yaml
M docs/api/websocket-events.md
M docs/architecture/architecture-evidence.md
M docs/architecture/data-flows.md
M modules/contracts/src/main/java/com/cryptostrategy/platform/contracts/api/SearchRequestPayload.java
M modules/contracts/src/test/java/com/cryptostrategy/platform/contracts/api/MessageContractSerializationTest.java
M modules/experiment-execution/build.gradle.kts
M modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/ExperimentExecutionModuleFactory.java
M modules/persistence/build.gradle.kts
M modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/news/NewsMigrationContractTest.java
M modules/search/build.gradle.kts
M specs/007-worker-reliable-job-processing/contracts/search-requests-reservation.md
M specs/009-public-api-realtime/quickstart.md
M specs/009-public-api-realtime/tasks.md
M specs/010-search-coordinator/plan.md
M specs/010-search-coordinator/quickstart.md
M specs/010-search-coordinator/spec.md
M specs/010-search-coordinator/tasks.md
```

### Important untracked F-010 implementation files

```text
?? .specify/gradle-f010-isolation.init.gradle

?? apps/api/src/main/java/com/cryptostrategy/platform/api/experiment/ExperimentRequestMapper.java
?? apps/api/src/test/java/com/cryptostrategy/platform/api/contract/SearchDocumentationParityTest.java
?? apps/api/src/test/java/com/cryptostrategy/platform/api/contract/SearchPublicFailureRedactionTest.java
?? apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/ReproduceExperimentIntegrationTest.java
?? apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/StartExperimentIntegrationTest.java
?? apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/StartExperimentReadinessIntegrationTest.java
?? apps/api/src/test/java/com/cryptostrategy/platform/api/performance/SearchCoordinatorPerformanceTest.java

?? apps/worker/src/main/java/com/cryptostrategy/platform/worker/config/SearchWorkerConfiguration.java
?? apps/worker/src/main/java/com/cryptostrategy/platform/worker/search/
?? apps/worker/src/test/java/com/cryptostrategy/platform/worker/search/

?? architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/SearchCoordinatorBoundaryTest.java
?? architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/SearchGeneratorReplaceabilityTest.java
?? architecture-tests/src/test/java/com/cryptostrategy/platform/architecture/SearchScopeBoundaryTest.java

?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/SearchCandidateAllocationUseCase.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/SearchCoordinationCommand.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/SearchCoordinationResult.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/SearchReproductionVerificationUseCase.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/SearchStartCommandFactory.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/StartSearchExperimentUseCase.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/StartSearchReproductionUseCase.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/in/TrustedSearchCoordinationUseCase.java

?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/AllocateSearchCandidateCommand.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/SearchAllocationContextGateway.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/SearchAllocationResult.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/SearchExperimentTransactionGateway.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/SearchReproductionGateway.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/SearchReproductionVerificationGateway.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/StartSearchGraphCommand.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/StartSearchGraphResult.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/api/port/out/TrustedSearchCoordinationGateway.java

?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchCandidateAllocationService.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchExperimentOrchestrationService.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionApplicationService.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchReproductionVerificationCoordinator.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/SearchStartCommandFactoryService.java
?? modules/experiment-execution/src/main/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationService.java

?? modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/SearchExperimentOrchestrationServiceTest.java
?? modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/SearchReproductionValidationTest.java
?? modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/SearchReproductionVerificationTest.java
?? modules/experiment-execution/src/test/java/com/cryptostrategy/platform/execution/internal/TrustedSearchCoordinationServiceTest.java

?? modules/persistence/src/experimentIntegrationTest/java/com/cryptostrategy/platform/persistence/internal/
?? modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/api/SearchPersistenceFactory.java
?? modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/execution/JdbcSearchExperimentTransaction.java
?? modules/persistence/src/main/java/com/cryptostrategy/platform/persistence/internal/search/
?? modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/api/
?? modules/persistence/src/test/java/com/cryptostrategy/platform/persistence/internal/search/

?? modules/search/src/main/java/com/cryptostrategy/platform/search/api/SearchModuleFactory.java
?? modules/search/src/main/java/com/cryptostrategy/platform/search/api/model/
?? modules/search/src/main/java/com/cryptostrategy/platform/search/api/port/
?? modules/search/src/main/java/com/cryptostrategy/platform/search/internal/CanonicalSearchSpace.java
?? modules/search/src/main/java/com/cryptostrategy/platform/search/internal/RandomStrategyGenerator.java
?? modules/search/src/main/java/com/cryptostrategy/platform/search/internal/SearchGenerationService.java
?? modules/search/src/main/java/com/cryptostrategy/platform/search/internal/StrategyGeneratorRegistry.java
?? modules/search/src/test/java/com/

?? specs/010-search-coordinator/checklists/implementation-readiness.md
?? supabase/migrations/20260903000100_f010_search_coordinator.sql
```

### Untracked ad-hoc scripts

Many temporary scripts exist, for example:

```text
check_f009.py
fix_all.py
fix_api.py
fix_apps.py
fix_coordinator.py
fix_everything.py
fix_execution_compilation*.py
fix_final_*.py
fix_generator*.py
fix_ids.py
fix_jdbc.py
fix_mapper*.py
fix_persistence*.py
fix_record_constructors.py
fix_remaining_errors.py
fix_repro_import.py
fix_search_ids.py
fix_tasks.py
fix_test.py
fix_trusted.py
fix_ulids.py
rename_search_refs.py
rename_search_refs_2.py
update_09_quickstart.py
update_10_quickstart.py
update_evidence.py
```

These are **not production deliverables**. Once the implementation is stable, determine whether any are intentionally needed. Otherwise they should not be part of the final feature commit.

---

## 13. Gradle / IDE problem at handoff

The user is currently seeing:

```text
No connection to gradle server. Try restarting the server.
```

Repeated "restart Gradle server" attempts have not solved it.

The previous agent also repeatedly ran combinations of:

```powershell
./gradlew --stop
taskkill /F /IM java.exe /T
Remove-Item -Recurse -Force .gradle, build, build-logic/.gradle, build-logic/build, .kotlin, build-logic/.kotlin
./gradlew clean testClasses
./gradlew testClasses --no-daemon --no-build-cache
```

This became a loop and is **not evidence that the project itself is broken**.

### Correct recovery procedure

First distinguish **IDE Gradle language server** from **Gradle wrapper/build**.

Run in project terminal:

```powershell
java -version
.\gradlew.bat --version
.\gradlew.bat tasks
```

The project requires **Java 21**.

Then try a small compile/test from the terminal:

```powershell
.\gradlew.bat --no-daemon :modules:search:compileJava
```

or the exact task currently needed.

If terminal Gradle works but the IDE still says "No connection to gradle server":

- treat it as VS Code/Gradle extension/Java language-server issue
- use **Java: Clean Java Language Server Workspace**
- then **Gradle: Refresh Gradle Project**
- verify VS Code/Gradle JVM points to **JDK 21**

Do **not** keep deleting `.gradle` and killing every Java process unless the wrapper itself is demonstrably hung.

If terminal Gradle fails, capture the final 20–30 lines and fix the actual compile/build failure.

---

## 14. Recommended continuation order

### Step A — inspect and stabilize the unfinished ID refactor

Open all currently modified F-010 public boundary/model files.

Especially:

```text
modules/experiment-execution/src/main/java/.../api/port/in/
modules/experiment-execution/src/main/java/.../api/port/out/
modules/search/src/main/java/.../api/model/
```

Compare against:

```text
architecture-tests/.../PurityAndCycleTest.java
```

Revert only the **incorrect mechanical transformations**, not the legitimate F-010 feature work.

Goal:

- typed/canonical IDs
- no raw String identity fields where prohibited
- no fake `...Ref` renaming workaround
- no illegal module dependencies

### Step B — compile narrowly

Avoid starting with `./gradlew test`.

Suggested progression:

```powershell
.\gradlew.bat --no-daemon :modules:search:compileJava
.\gradlew.bat --no-daemon :modules:experiment-execution:compileJava
.\gradlew.bat --no-daemon :modules:persistence:compileJava
.\gradlew.bat --no-daemon :apps:worker:compileJava
.\gradlew.bat --no-daemon :apps:api:compileJava
```

Then:

```powershell
.\gradlew.bat --no-daemon :architecture-tests:test
```

Then targeted F-010 tests.

### Step C — rerun T085 exact quality/full-suite command

Use the exact acceptance command written in `tasks.md`.

Do not mark T085 until:

- canonical-value/ID quality gate passes
- architecture suite passes
- F-008 checksum issue is resolved correctly or the exact T085 definition explicitly permits documented pre-existing failure (do not assume this)
- Java 21 full suite satisfies task definition

### Step D — T086

The previous trace said T086 should be the real integration/E2E suite after T085.

Read the exact task definition first.

Likely reuse the established isolated environment:

```text
PostgreSQL: localhost:54322 / crypto_f010
Redis container: crypto-f010-redis
init script: .specify/gradle-f010-isolation.init.gradle
```

Do not fake external-system behavior if the task specifically asks for real DB/Redis evidence.

### Step E — T087/T088

Complete final evidence and release-readiness requirements from `tasks.md`.

Likely areas already partially updated:

```text
docs/api/openapi.yaml
docs/api/error-catalog.md
docs/api/examples.md
docs/api/websocket-events.md
docs/architecture/architecture-evidence.md
docs/architecture/data-flows.md
specs/009-public-api-realtime/quickstart.md
specs/009-public-api-realtime/tasks.md
specs/010-search-coordinator/quickstart.md
```

Validate that any F-009 evidence link / public API readiness / redaction matrix requested by tasks is actually cross-referenced and tested.

Finally:

```powershell
git diff --check
rg -n "^- \[ \] T" specs/010-search-coordinator/tasks.md
git status --short
```

Only when there are no remaining required tasks should all final checkboxes be `[x]`.

---

## 15. Tests/evidence known to have passed earlier

These were reported green before the unfinished canonical-ID cleanup:

```text
SearchFailurePolicyTest
TrustedSearchCoordinationServiceTest
SearchStopRaceIntegrationTest
SearchDeadlineIntegrationTest
SearchCoordinatorTest
SearchCrashRecoveryTest
SearchCompletionConsumerTest
SearchReconciliationTest
finite allocation pipeline test
allocation fence regression
deadline regression
public Start smoke
PostgreSQL reproduction atomic-copy/rollback test
durable reproduction comparator -> MATCHED
public Reproduce smoke
T083 benchmark
T084 architecture suite
```

Because the source tree has changed since some of these runs, **rerun relevant suites before finalizing**.

---

## 16. Behavioral invariants that must remain true

The implementation must continue to satisfy these semantics:

1. **Authoritative durable state wins.**
   - Worker queue contents are not the source of truth.
   - Reconciliation reloads PostgreSQL truth.

2. **Deadline is frozen.**
   - Adapter/process restart must not reset it.

3. **On-time authoritative completion beats deadline.**
   - If durable child completion was authoritative within the deadline, finish `COMPLETED`.

4. **Late completion does not resurrect the run.**
   - Late path terminates through `STOPPING -> STOPPED`.

5. **Version fences prevent stale writes.**

6. **Stop prevents subsequent allocations.**

7. **Message dedupe applies once to the external event.**
   - Do not reuse the same durable receipt claim for multiple internal lifecycle transitions.

8. **Evaluation evidence is immutable.**

9. **Generator implementation is replaceable.**
   - Resolve through public registry, exact generator ID/version.

10. **Host modules depend only on published boundaries.**
    - No API/Worker imports of implementation internals.

11. **Start/Reproduce readiness gates are evidence-based and separate.**

12. **Failure and observability outputs must redact sensitive/publicly unsafe details.**

---

## 17. What NOT to do

Do not:

- `git reset --hard`
- discard the current working tree
- weaken architecture tests
- rename `Id` fields to `Ref` merely to evade the canonical-ID test
- expose `search.internal` or `execution.internal` to API/Worker
- change ADR/dependency rules to fit the implementation
- delete immutable Evaluation evidence for test cleanup
- make reproduction synchronous again
- reopen API gates without smoke/evidence
- repeatedly kill Java/delete Gradle caches without first testing the wrapper directly
- mark tasks `[x]` based only on compilation when the task requires integration/evidence/docs

---

## 18. Suggested first message/action for a new agent

Use this as the continuation goal:

```text
$speckit-implement

Continue from specs/010-search-coordinator/IMPLEMENTATION_CHECKPOINT.md.

Read SKILL.md, spec.md, plan.md, tasks.md and the current git diff first.
Do not reset the worktree.

Complete every remaining unchecked task in specs/010-search-coordinator/tasks.md and mark [x] only after implementation + required verification/evidence pass.

Immediate priority:
1. repair the unfinished F-010 canonical-ID refactor according to PurityAndCycleTest and existing typed-ID conventions;
2. get targeted module compile + architecture tests green;
3. resolve the F-008 migration checksum failure correctly;
4. complete T085 full Java 21 gate;
5. finish T086-T088 exactly as tasks.md specifies.

Preserve all architecture constraints and the already verified stop/deadline/reconciliation/reproduction invariants.
```

---

## 19. Final handoff summary

The feature is **near completion**, but the tree is currently in a sensitive intermediate state caused by the attempted quality-gate ID cleanup.

The most important thing for the next agent is **not to redo F-010 from scratch**. The hard runtime work is already implemented and previously tested. The agent should stabilize the canonical IDs, recover the build from the current partial refactor, complete the Java 21/full integration gates, update final evidence, and then mark the remaining tasks complete.

The authoritative truth for completion remains:

```text
specs/010-search-coordinator/tasks.md
```

not this checkpoint.
