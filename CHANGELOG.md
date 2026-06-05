# Changelog

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
