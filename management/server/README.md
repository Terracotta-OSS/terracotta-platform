# Management Server Suite (`:management:server`)

## Overview

The `management:server` module suite provides the server-side Voltron management services that collect, aggregate, and route management data, notifications, and statistics within a Terracotta server process.

The suite contains two submodules:
- **`server:api`**: Server-side interfaces (`ManagementDataService`, `MonitoringService`, `ManagementAgentConfigurator`) for interacting with server management.
- **`server:services`**: Voltron server service implementation providing ring buffers, statistics aggregators, and notification dispatchers.

## Key Components

- `ManagementDataService`: Service aggregating statistics, capabilities, and notifications from server entities and local providers.
- `MonitoringService`: Server-side service recording cluster monitoring events, topology mutations, and entity lifecycle changes.
- `DefaultManagementDataService`: Default implementation managing in-memory circular buffers for collected stats and notifications.

## Interactions & Dependencies

- **Dependencies**: `management:model`, `management:registry`, Terracotta Server API (`org.terracotta:terracotta-configuration-api`).
- **Consumed By**:
  - `management:entities:nms` and `management:entities:nms-agent` to bridge server-level metrics to connected monitoring clients.
  - Terracotta server entities (e.g. dynamic config, offheap, data roots) registering management bindings.
