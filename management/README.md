# Terracotta Management Framework (`:management`)

## Overview

The `management` module suite provides the complete distributed management, metrics collection, notification streaming, and remote execution infrastructure for Terracotta clusters.

```
                                  +------------------------------------+
                                  | Terracotta Management Server (TMS) |
                                  +-----------------+------------------+
                                                    | (NMS Client API)
                                                    v
+-----------------------+         +------------------------------------+
|  Managed Application  |         |        Active Server Node          |
|  (Ehcache / TCStore)  |         |                                    |
|  +-----------------+  |         |  +------------------------------+  |
|  |ManagementRegist.|  |         |  |   NMS Hub Active Entity      |  |
|  +--------+--------+  |         |  +--------------^---------------+  |
|           |           |         |                 |                  |
|           v           |         |  +--------------+---------------+  |
|  +-----------------+  |         |  |  NMS Agent Active Entity     |  |
|  | NMS Agent Client+--+---------+->+                              |  |
|  +-----------------+  | (Voltron|  +------------------------------+  |
|                       | Entity) +------------------------------------+
+-----------------------+
```

## Submodules

- [`:management:sequence-generator`](management/sequence-generator/README.md): Monotonically increasing unique sequence generator for distributed events.
- [`:management:model`](management/model/README.md): Core management data types, capabilities, statistics, contexts, and notifications.
- [`:management:registry`](management/registry/README.md): In-process registry managing manageable objects, statistics collectors, and operation descriptors.
- [`:management:server`](management/server/README.md): Server-side management services and in-memory event queues.
- [`:management:entities:nms`](management/entities/nms/README.md): Centralized NMS management gateway entity connecting to TMS.
- [`:management:entities:nms-agent`](management/entities/nms-agent/README.md): Distributed agent entity bridging local registries to the central NMS hub.
- [`:management:testing`](management/testing/README.md): Mock entities, sample applications, and integration test suites.
