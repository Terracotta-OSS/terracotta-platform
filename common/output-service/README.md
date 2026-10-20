# Common Output Service (`:common:output-service`)

## Overview

The `output-service` module provides an abstraction for writing log, command result, and status messages across different execution environments (command line, test runners, logging frameworks, and in-memory buffers).

## Key Components

- [`OutputService`](common/output-service/src/main/java/org/terracotta/dynamic_config/cli/api/output/OutputService.java:21): Core interface defining message channels: `out(String)`, `info(String)`, `warn(String)`, and `error(String)`.
- [`ConsoleOutputService`](common/output-service/src/main/java/org/terracotta/dynamic_config/cli/api/output/ConsoleOutputService.java:20): Outputs directly to `System.out` and `System.err`.
- [`FileOutputService`](common/output-service/src/main/java/org/terracotta/dynamic_config/cli/api/output/FileOutputService.java:23): Streams output to target log and report files.
- [`InMemoryOutputService`](common/output-service/src/main/java/org/terracotta/dynamic_config/cli/api/output/InMemoryOutputService.java:24): Captures messages in memory for programmatic inspection and unit testing.
- [`LoggingOutputService`](common/output-service/src/main/java/org/terracotta/dynamic_config/cli/api/output/LoggingOutputService.java:22): Forwards output to SLF4J loggers.
- [`StreamOutputService`](common/output-service/src/main/java/org/terracotta/dynamic_config/cli/api/output/StreamOutputService.java:22): Writes to arbitrary Java `PrintStream` instances.

## Interactions & Dependencies

- **Dependencies**: SLF4J API (`org.slf4j:slf4j-api`).
- **Consumed By**:
  - `dynamic-config:cli:api` and `dynamic-config:cli:config-tool` for decoupling command logic from console presentation.
  - Test suites capturing CLI outputs for assertions.
