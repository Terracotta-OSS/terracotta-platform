# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Read first
- Read project READMEs when reviewing and coding to learn architecture and relationships. Read the module's `README.md` before touching a module (root [`README.md`](README.md) is the index). Update it when changing architecture, contracts, serialization codecs, or module interactions. Verify claims in code before writing them in a README.
- Consider the impact of every change you make across dependent modules, kit packaging, tests, and docs.
- Never rely on hypotheses or assumptions. If unsure, read the code or ask.
- Do not look into folders named `archived`: they contain obsolete documentation.

## Build / test
- Gradle wrapper only. Compiles with Java 17 toolchain by default (`-PcompileVM`, `-PtestVM` to override). First build: `git submodule update --init` (`build-logic/terracotta-gradle-plugins` is a submodule).
- Single test: `./gradlew :<module-path>:test --tests 'pkg.ClassName.method'`. Add `cleanTest` to force a rerun when Gradle skips up-to-date tests (e.g. `./gradlew :common:structures:cleanTest :common:structures:test --tests "org.terracotta.common.struct.MemoryUnitTest"`).
- Integration tests (Angela / Galvan): `./gradlew :dynamic-config:testing:integration-tests:test` or `:management:testing:integration-tests:test` (use `DC_IT_MAX_FORKS=1` if process-constrained).
- Full verification: `./gradlew check` runs tests, checkstyle, copyright, and spotbugs.
- Specific checks: `./gradlew checkstyle` (or `:module:checkstyleMain`), `./gradlew spotbugs`, `./gradlew copyright`.
- Dependency convergence: `failOnVersionConflict()` is enforced across all configurations.

## Build logic
- Apply `org.terracotta.build.convention.*` plugins (`java-library`, `base`, `angela`, `galvan`, etc.), never the raw standard plugins.
- Strict compilation: `-Xlint:all` and `-Werror` are enabled on all Java compilation tasks (all warnings fail the build).
- Artifact naming convention follows `${project.group}.${project.name}`.

## Code style & conventions (enforced by `config/checkstyle`)
- 2-space indentation, spaces only, UNIX line endings (LF), no trailing whitespace (build fails otherwise).
- License / Copyright headers:
  - When creating a source file, always add a license header.
  - When updating a source file, always update the year range of its license header to include the current year (`config/checkstyle/copyright.header`).
- Forbidden imports: Never import `org.junit.rules.TemporaryFolder` (checkstyle fails); use `org.terracotta.org.junit.rules.TemporaryFolder`.
- Java style: Blank line between static and non-static members. Remove unused imports, variables, and methods touched. Update or add matching tests with every class change.
- Custom primitives:
  - Use [`TimeBudget`](common/structures/src/main/java/org/terracotta/common/struct/TimeBudget.java) for Voltron timeouts (-1 try-once, 0 infinite, >0 bounded).
  - Use [`Measure`](common/structures/src/main/java/org/terracotta/common/struct/Measure.java) with `MemoryUnit`/`TimeUnit` for typed configurations.
- Concurrent tests: Start threads behind a start latch, join them with a latch without a timeout (CI is slow).

## Commits
- Ask for the JIRA ID (https://ibm-middleware.atlassian.net/), offer "No JIRA Ticket", and propose a title for confirmation.
- Format: `<JIRA-ID>: <Title>\n<Description>\n\nAssisted by: <Your Name> AI` (or `<Title>\n<Description>\n\nAssisted by: <Your Name> AI` if no ticket).
- Exclude `.bob/` untracked files, root-level uppercase progress `*.md` files, and root `*.adoc` plan files.
