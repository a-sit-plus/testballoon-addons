# Changelog

## 0.x

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