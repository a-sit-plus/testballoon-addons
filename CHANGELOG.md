# Changelog

## 0.13.0
* **Matrix testing:***
    * Fix `TestConfig` being re-applied at every nested matrix level. The base config (a session config captured via
      `MatrixTestDefaults { }`, or a suite-level `testConfig`) is now applied once at the level it's set and inherited
      to children by TestBalloon, instead of being re-chained at each nested data/property case, suite, and test. This
      caused stateful wrappers to multiply — most visibly a session `testScope` combined with `execution = Concurrent`
      deadlocking or aborting discovery ("did not discover any tests"), and a suite-level `aroundAll` running once per
      case. Removed the internal `MatrixTestDefaults.testSessionConfig` capture (TestBalloon already propagates the
      session config). Per-element `testConfig` on `test` / `testSuite` / `matrixSuite` (incl. FreeSpec forms) still
      applies exactly once, so `aroundAll` / `aroundEach` behave as expected.
    * `test` / `testSuite` and their FreeSpec `"name"(…)` forms now accept a full matrix config via a new
      `matrixConfig { … }` value (e.g. `testSuite("g", matrixConfig { execution = ExecutionMode.Concurrent(8) }) { }`,
      `"g"(matrixConfig { … }) - { }`). Unset fields inherit the enclosing scope; `testConfig` is one of the fields, so
      the existing `testConfig =` forms are now thin wrappers around `matrixConfig { testConfig = … }`. Lets you set
      per-subtree concurrency (and have it auto-disable the `TestScope`) without a separate `matrixSuite`. Also
      available on the fixture-scope `test` / `testSuite` (and their FreeSpec forms). When a `matrixConfig` sets both
      `execution = Concurrent(…)` and a `testConfig` that enables a `TestScope`, the concurrency disable wins.
    * Matrix concurrency now auto-disables TestBalloon's virtual-time `TestScope`: any `ExecutionMode.Concurrent`
      layer/suite and every `compact` block (both execute on real dispatchers) chain `testScope(isEnabled = false)`,
      so concurrent/compact matrices run even under a session that enables `testScope` (the default) — no manual
      `testScope(isEnabled = false)` needed. Sequential execution leaves the inherited `TestScope` intact.
    * Fail early nesting tests in tests instead of suites
    * Deduplicate the `data` / `property` API across the real-tree and `compact` scopes. Both now share a single
      sealed `MatrixScope` surface and one set of `data` / `property` overloads (returning `DataLayer` /
      `PropertyLayer`), instead of two parallel copies. No behaviour change; the per-scope layer types
      (`MatrixDataLayer` / `CompactDataLayer` / …) are gone.
    * Internal: collapse the compact `VirtualNode` model from four layer variants (`Data` / `DataTest` /
      `Property` / `PropertyTest`) to two (`Data` / `Property`) carrying a `LayerBody` (container vs. terminal),
      halving the execution dispatch and removing duplicated child-scope building. No behaviour change.
    * **Breaking — replay API.** Replace the scalar+list replay overloads with one `replay` parameter per layer,
      typed as a small self-describing value class: data takes `replay = Indexes(0L, 2L)` (or `Indexes(0L..9L)`),
      property takes `replay = Cases(seed = 1L, iteration = 2L)` (flat common case), `Cases(Input(...), Input(...))`
      for several seeds, or `Cases(seed, 1L..5L)` for a range. `ReplayInput` is renamed `Input`. The copy-paste
      failure-report lines now emit these forms. Net: the `data`/`property` overloads drop from 12 to 6, and range
      selection is available via secondary constructors. Migrate `replayIndex(es) = …` → `replay = Indexes(…)`,
      `replay(s) = ReplayInput(…)` → `replay = Cases(…)`.
    * Internal: unify the parallel data/property machinery behind a single sealed `LayerSpec`. The four real-tree
      `*Internal` registration methods collapse to one `registerLayer`, the two compact registration methods to one
      `addLayer`, the two `VirtualNode` layer kinds to one `VirtualNode.Layer`, and the two execution branches /
      four dispatch helpers each halve. ~190 fewer lines; public API (the `data` / `property` overloads,
      `DataLayer` / `PropertyLayer`, config builders) and all output are unchanged.
    * Annotate the named `data` / `property` and `compact` layer functions with `@TestRegistering`, so the IDE shows a
      run-gutter on a top-level or statically-nested layer line (running the whole layer). Tests/suites nested inside a
      `data` / `property` / `compact` layer still cannot be run from the gutter — their path contains a runtime-generated
      per-case segment — so run the enclosing layer or use a `TESTBALLOON_INCLUDE_PATTERNS` filter. Nameless layers stay
      unannotated. See the Matrix "Notes" in the README.

## 0.12.0
* **Matrix testing: nameless `data` / `property` layers**
    * `data` and `property` now have overloads that omit the leading name (e.g. `data(listOf(...)) test { ... }`,
      `property(Arb.int()) - { ... }`). A nameless layer skips the intermediate grouping node and registers its rows
      directly in the surrounding scope. A named layer remains shorthand for wrapping a nameless one in a labeled
      suite. Available in both regular and `compact` scopes, across all overloads (Iterable/Sequence,
      `replayIndex(es)` / `replay(s)`, `- { }` and `test { }`).
* **Matrix testing: replay info shows layer kind**
    * `Error replay info` frames are now tagged with their layer kind — `(property)` or `(data)` — for named and
      nameless layers alike (e.g. `(property) seed: ...`). Nameless layers print the marker without a name; the
      general failure/report path still shows no synthesized segment.
* **Matrix testing: replace layer separator slash with TestBalloon separator arrow**
* **Matrix testing: replay info shows the full path**
    * The `Error replay info` path now mirrors the compact report path in full — enclosing grouping suites
      (`"name" - { ... }`) and plain `test` leaves appear as path segments, not just the replayable `data`/`property`
      layers. Only replayable layers still emit a `- ...` argument line; group-only chains produce no replay block.

## 0.11.0
* **Matrix testing: bounded compact concurrency**
    * `CompactConcurrency.Shared(n)` runs a compact block through one compact-wide worker budget, so nested virtual
      layers can no longer multiply coroutine counts; `CompactConcurrency.Layered` keeps per-layer behaviour.
      Set it per `compact` block or via `defaultCompactConcurrency`.
* **Matrix testing: replay failing cases**
    * Failure reports — real test-tree leaves and compacted virtual rows alike — now include a copy-paste-ready
      `Error replay info` block whose lines are valid `property` / `data` arguments.
    * Pin a property layer to recorded cases with `replay = ReplayInput(seed, iteration)`, or
      `replays = listOf(...)` for several seed/iteration pairs at once; pin a data layer with `replayIndex` /
      `replayIndexes`. Pasting the reported lines re-runs exactly the failing case(s).
    * `replay` is independent of a layer's `seed` (deterministic full run vs. selecting recorded cases).
    * Replay info captures the full enclosing layer chain and records data indexes independently of `nameFn`.
* **Matrix testing: API changes:**
    * Make name for compact layers mandatory 

## 0.10.0
* **Matrix testing**

### 0.9.0

* TestBalloon 1.0.0
* Improve compacted data/property test reporting:
    * Add a `Summary: X OK, Y failed` line to compacted failure messages
    * Add `suppressCompactSuccesses` configuration and terminal overrides to omit individual `OK` rows while keeping
      the summary
    * Avoid recursively embedding full nested compacted reports in parent compacted error rows
    * Bound compacted failure row names using the configured test name length
* Add concurrent execution for compacted suspending terminal `withData` and `checkAll` leaves via `compactConcurrent`
* Add periodic progress heartbeat output while compacted terminal `withData` and `checkAll` leaves are running
* Simplify and fix custom truncation/stringification for primitive and unsigned arrays
* By default, only the first error's stack trace is reported for compacted data/property tests. This used to be
  different. To fall back to the old behaviour (or for debugging), set `addSuppressedErrorsToCompactedFailures = true`
* Remove deprecated functions marked for removal. Refactor as follows:
    * `withDataSuites(…) {…}` -> `withData(…) - {…}` (note the dash)
    * `checkAllSuites(…) {…}` -> `checkAll(…) - {…}` (note the dash)

### 0.8.0

* Remove `displayName` and `displayNameLength`
* Remove deprecated `DEFAULT_TEST_NAME_MAX_LEN`

### 0.7.0 - 0.7.1

* treat varags of `Pair<String,*>` the same as `Map<String,*>` in data-driven tests
* TestBalloon 0.8.2
* Fix incorrect internal reference to max length

### 0.7.0-RC

* Platform-agnostic test name length defaults
* Prefixes for data test and property test
* Shorter compact names
* Global test name length configuration -> deprecate `DEFAULT_TEST_NAME_MAX_LEN`
* Hard fails on too long overall test names on Android

### 0.6.2-RC

* Support TestBalloon 0.8-RC

### 0.6.1

* Add missing TestExecutionScope to data-driven test leaves

### 0.6.0 Breaking FreeSpec Fixture Generation

* Support fixture generation for suites in addition to tests
    * Support nesting, but only ever use the fixture generator for the toplevel scope
    * FreeSpec now requires explicit fixture parameter specification to disambiguate
* Remove deprecated properties marked for removal with 0.6

### 0.5.0

* Refactor to get rid of reification for:
    * proper stack traces
    * Kotlin 2.3 compatibility
* Sane default stringification of collection and arrays types
    * All primitive arrays are correctly joined to string
    * All unsigned arrays are correctly joined to string
    * `ByteArray` and `UByteArray` use hex notation
* fix property test compacting bug
* clarify deprecations
* Datatest
    * fix misnamed `withDataSuites`
    * fix misnamed `arguments` -> `parameters`

### 0.4.1

* Fix length config bug
* Default display name max length to -1 (no truncation)
* Remove bare-lambda fixture generation

### 0.4.0

* Change global defaults and fix length bug
* Allow globally (and per test/suite) setting of max display length

### 0.3.1

Configurable max test name length (globally and per test)

### 0.3.0

* Revised fixture generation
    * Use explicit function instead of bare lambdas; no more context params needed
    * Deprecate old bare lambdas

### 0.2.0

* Optionally compacted data driven tests and property tests

### 0.1.3

* Work around test name issues

### 0.1.2

* More migration helpers and alternative syntax for nesting suites

### 0.1.x

#### 0.1.1

* Per-test fixture generation addon
* Update to TestBalloon 0.7.1

#### 0.1.0

Initial release
