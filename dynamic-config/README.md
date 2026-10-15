# Dynamic Configuration Subsystem (`:dynamic-config`)

## Overview

The `dynamic-config` subsystem is the configuration management architecture of Terracotta Platform (10.x+). It replaces legacy static XML configuration files (`tc-config.xml`) with a dynamic, cluster-aware, distributed configuration system that supports:
- Starting servers without pre-configured files in diagnostic mode.
- Activating clusters dynamically from property files, repositories, or remote CLI commands.
- Dynamic runtime updates (e.g. resizing offheap resources, adding data roots, altering timeouts) without restarting the cluster.
- Dynamic topology mutations (attaching/detaching nodes and stripes) on a live running cluster.
- Safe two-phase commit consensus and crash recovery powered by the **Nomad** protocol.

---

## Architecture

```
                          +-------------------------------+
                          |    CLI: config-tool.sh / API   |
                          +---------------+---------------+
                                          |
                +-------------------------+-------------------------+
                | (Diagnostic RPC)                                  | (Nomad Entity RPC)
                v                                                   v
+-------------------------------+                   +-------------------------------+
|       :diagnostic:client      |                   |:dynamic-config:entities:nomad |
+---------------+---------------+                   +---------------+---------------+
                |                                                   |
                v                                                   v
+-----------------------------------------------------------------------------------+
|                            Terracotta Server Node                                 |
|                                                                                   |
|  +--------------------------------+       +------------------------------------+  |
|  |:dynamic-config:server:services |       | :dynamic-config:repository         |  |
|  | - DynamicConfigServiceImpl     |       | - Reads/writes Sanskrit on-disk    |  |
|  | - NomadServerManager (2PC)     +------>|   configuration repository         |  |
|  +----------------+---------------+       +------------------------------------+  |
|                   |                                                               |
|                   v                                                               |
|  +--------------------------------+                                               |
|  | ConfigChangeHandler plugins    |                                               |
|  | (Offheap, DataRoots, Leases)   |                                               |
|  +--------------------------------+                                               |
+-----------------------------------------------------------------------------------+
```

## Submodules

- [`:dynamic-config:model`](dynamic-config/model/README.md): Immutable cluster, stripe, node, and setting domain models.
- [`:dynamic-config:api`](dynamic-config/api/README.md): Core service interfaces, listener SPIs, and dynamic change events.
- [`:dynamic-config:json`](dynamic-config/json/README.md): Gson serialization module for configuration topologies and Nomad messages.
- [`:dynamic-config:repository`](dynamic-config/repository/README.md): Sanskrit-backed on-disk configuration repository layout and schema upgrader.
- [`:dynamic-config:server`](dynamic-config/server/README.md): Server-side bootstrap providers, services, and change handler hooks.
- [`:dynamic-config:entities:nomad`](dynamic-config/entities/nomad/README.md): Voltron entity exposing Nomad 2PC protocol over entity RPC.
- [`:dynamic-config:entities:topology`](dynamic-config/entities/topology/README.md): Voltron entity broadcasting cluster topology updates to clients.
- [`:dynamic-config:entities:management`](dynamic-config/entities/management/README.md): Server entity exposing configuration to NMS/TMS.
- [`:dynamic-config:cli`](dynamic-config/cli/README.md): Command-line administration tools (`config-tool`, `upgrade-tool`).
- [`:dynamic-config:testing`](dynamic-config/testing/README.md): Galvan/Angela testing harnesses and integration tests.
