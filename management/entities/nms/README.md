# NMS Management Entity (`:management:entities:nms`)

## Overview

The `nms` (Network Management Service) entity acts as the centralized management gateway on the Terracotta active server. It aggregates topology information, runtime notifications, and metrics from all connected clients and server nodes, exposing a single unified endpoint for the Terracotta Management Server (TMS) and external monitoring consoles.

The module is structured into three submodules:
- **`common`**: Protocol messages, responses, codecs, and query structures.
- **`client`**: Client API used by TMS (`NmsEntity`) to connect to the server, subscribe to event streams, read cluster topologies, and trigger remote management calls.
- **`server`**: Active and passive server entity implementations (`NmsActiveEntity`, `NmsPassiveEntity`) managing the central metric buffer and client subscriptions.

---

## Architecture & Submodules

```
+---------------------------------------------------------------+
|             Terracotta Management Server (TMS)                |
+-------------------------------+-------------------------------+
                                | (NmsEntity Client API)
                                v
+---------------------------------------------------------------+
|                 :management:entities:nms:client               |
|  - NmsEntity / NmsEntityClientService                         |
+-------------------------------+-------------------------------+
                                | (Voltron Entity Messages)
                                v
+---------------------------------------------------------------+
|                 :management:entities:nms:server               |
|  - NmsActiveEntity / NmsPassiveEntity                         |
|  - Aggregates messages from all NmsAgent entities             |
+---------------------------------------------------------------+
```

### 1. `:management:entities:nms:common`

- Encapsulates management request messages, response envelopes, message codecs, and topology change events.

### 2. `:management:entities:nms:client`

- Provides the `NmsEntity` interface used by monitoring consoles to:
  - Fetch aggregated cluster topology.
  - Stream real-time contextual notifications from all cluster participants.
  - Execute remote management operations across specific nodes or clients.

### 3. `:management:entities:nms:server`

- Implements `NmsActiveEntity` and `NmsPassiveEntity`.
- Maintains active subscriptions for monitoring clients.
- Coordinates active-to-passive state replication so management event buffers survive active server failover.

## Interactions & Dependencies

- **Dependencies**: `management:model`, `management:server:api`, `communicator:server`, Terracotta Entity API (`org.terracotta:entity-api`).
- **Consumed By**: Terracotta Management Server (TMS), web dashboards, and monitoring agents.
