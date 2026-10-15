# NMS Agent Entity (`:management:entities:nms-agent`)

## Overview

The `nms-agent` entity module connects individual client processes and server instances to the cluster-wide NMS management hub. Every managed client JVM or server plugin uses an `nms-agent` client to publish its local `ManagementRegistry` capabilities, push contextual notifications, and execute incoming management calls sent by administrators.

The module is structured into three submodules:
- **`common`**: Agent message envelopes, capability exchange payloads, and codecs.
- **`client`**: Client-side agent bridge (`NmsAgentEntity`) linking local `ManagementRegistry` instances to the server.
- **`server`**: Server-side agent entity handling agent registration, routing commands, and forwarding notifications to the NMS hub.

---

## Submodules & Architecture

```
+---------------------------------------------------------------+
|         Client App / Clustered Cache / Server Plugin          |
|  - ManagementRegistry (holds local beans, stats, operations)  |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|              :management:entities:nms-agent:client            |
|  - NmsAgentEntity                                             |
|  - Pushes local notifications & capabilities                  |
|  - Listens for remote management invocations                  |
+-------------------------------+-------------------------------+
                                |
                                | (Voltron Entity Messages)
                                v
+---------------------------------------------------------------+
|              :management:entities:nms-agent:server            |
|  - NmsAgentActiveEntity / NmsAgentPassiveEntity               |
|  - Forwards data to NmsActiveEntity (the central hub)         |
+---------------------------------------------------------------+
```

### 1. `:management:entities:nms-agent:common`

- DTOs and codecs for agent registration, capability synchronization, notification publishing, and call dispatching.

### 2. `:management:entities:nms-agent:client`

- `NmsAgentEntity`: Client endpoint exposing methods to expose capabilities, stream notifications, and bind incoming RPC invocations to local `ManagementRegistry` methods.

### 3. `:management:entities:nms-agent:server`

- `NmsAgentActiveEntity` & `NmsAgentPassiveEntity`: Server entities managing the lifecycle of each connected agent, tracking liveness, and replicating agent state across server stripes.

## Interactions & Dependencies

- **Dependencies**: `management:model`, `management:registry`, `communicator:client`, `communicator:server`.
- **Consumed By**: Terracotta Ehcache client, Terracotta Store client, server-side management plugins.
