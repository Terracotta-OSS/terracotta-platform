# Resources Data Root (`:resources:data-root`)

## Overview

The `data-root` module manages named durable disk storage directories (`data-dirs`) on Terracotta server nodes. Server entities (such as persistent stores, Fast Restart persistence, or Nomad transaction logs) request access to disk directories via the `DataDirsConfig` service.

## Key Components

- [`DataDirsConfig`](resources/data-root/src/main/java/org/terracotta/config/data_roots/DataDirsConfig.java:23): Server service interface allowing entities to look up or create data directories by logical resource identifier (e.g., `main`).
- [`DataDirsConfigImpl`](resources/data-root/src/main/java/org/terracotta/config/data_roots/DataDirsConfigImpl.java:27): Implementation validating paths, checking disk permissions, and ensuring directory isolation.
- [`DataDirConfigChangeHandler`](resources/data-root/src/main/java/org/terracotta/config/data_roots/DataDirConfigChangeHandler.java:28): Dynamic config listener supporting adding new data directories at runtime.
- [`DataRootsDynamicConfigExtension`](resources/data-root/src/main/java/org/terracotta/config/data_roots/DataRootsDynamicConfigExtension.java:26): Service extension registering data root configuration with dynamic config.
- [`DataRootStatisticsManagementProvider`](resources/data-root/src/main/java/org/terracotta/config/data_roots/management/DataRootStatisticsManagementProvider.java:27): Exposes disk usage, free space, and capacity statistics to the Terracotta Management Server (TMS).

## Interactions & Dependencies

- **Dependencies**: `dynamic-config:server:api`, `management:model`, `management:registry`, Terracotta Configuration APIs.
- **Consumed By**: Clustered persistence storage plugins, server entities requiring durable disk paths, and dynamic config CLI for configuring `data-dirs`.
