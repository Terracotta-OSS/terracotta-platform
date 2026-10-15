# Common Test Utilities (`:common:test-utilities`)

## Overview

The `test-utilities` module provides reusable JUnit rules, matchers, process runners, and temporary folder utilities tailored for Terracotta unit and integration test suites.

## Key Components

- [`TmpDir`](common/test-utilities/src/main/java/org/terracotta/testing/TmpDir.java:27): JUnit `@Rule` providing isolated temporary directories that are automatically created and cleaned up.
- [`Retry`](common/test-utilities/src/main/java/org/terracotta/testing/Retry.java:28): JUnit rule and annotation to automatically re-run flaky tests under slow CI conditions.
- [`ExtendedTestRule`](common/test-utilities/src/main/java/org/terracotta/testing/ExtendedTestRule.java:23): Base class for custom JUnit test rules.
- [`ExceptionMatcher`](common/test-utilities/src/main/java/org/terracotta/testing/ExceptionMatcher.java:26): Hamcrest matchers for verifying root causes and exception message substrings.
- [`JavaBinary`](common/test-utilities/src/main/java/org/terracotta/testing/JavaBinary.java:26) / [`JavaTool`](common/test-utilities/src/main/java/org/terracotta/testing/JavaTool.java:24): Helpers to spawn external Java child processes and CLI tools with standard argument handling.

## Interactions & Dependencies

- **Dependencies**: JUnit 4, Hamcrest.
- **Consumed By**: Test suites across `dynamic-config`, `management`, `lease`, `diagnostic`, and `common` modules.
