# Dynamic Config Topology Entity (`:dynamic-config:entities:topology`)

## Overview

The `dynamic-config:entities:topology` module provides real-time cluster topology synchronization and event broadcasting between Terracotta server nodes and connected client applications. When servers are added, detached, or configuration settings are modified dynamically, this entity pushes updated `Cluster` topology snapshots to connected clients.

The module is structured into three submodules:
- **`common`**: Runnel binary serialization codec, message definitions, and topology event types.
- **`client`**: Client entity (`TopologyEntity`) enabling applications to query current topology or register topology change listeners.
- **`server`**: Active and passive server entities (`TopologyActiveEntity`, `TopologyPassiveEntity`) broadcasting topology mutations to connected clients.

---

## Submodules & Architecture

```
+---------------------------------------------------------------+
|                      Client Application                       |
|           (Ehcache / TCStore Clustered Client)                |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|            :dynamic-config:entities:topology:client           |
|  - TopologyEntity / DynamicConfigTopologyService              |
|  - Listens for TopologyEvent broadcasts                       |
+-------------------------------+-------------------------------+
                                |
                                | (Runnel-encoded Voltron messages)
                                v
+---------------------------------------------------------------+
|            :dynamic-config:entities:topology:server           |
|  - TopologyActiveEntity / TopologyPassiveEntity               |
|  - Subscribes to server-side DynamicConfigListener            |
|  - Broadcasts new Cluster model on topology changes           |
+---------------------------------------------------------------+
```

### 1. `:dynamic-config:entities:topology:common`

- Binary protocol encoding topology requests, responses, and notification events using `common:runnel`.

### 2. `:dynamic-config:entities:topology:client`

- `TopologyEntity`: Client-side interface to query the active `Cluster` topology and receive async callbacks when servers join, fail over, or leave the cluster.

### 3. `:dynamic-config:entities:topology:server`

- Listens to server configuration changes via `DynamicConfigListener` and publishes updated topology snapshots to all connected client endpoints.

## Interactions & Dependencies

- **Dependencies**: `dynamic-config:model`, `common:runnel`, Terracotta Entity API (`org.terracotta:entity-api`).
- **Consumed By**: Terracotta client connection factories, Ehcache/TCStore clustered clients, and diagnostic tools.
