# Management Registry (`:management:registry`)

## Overview

The `management-registry` module provides the in-process registry used by clients, server entities, and plugins to register manageable objects, expose actions/operations for remote execution, record notifications, and collect real-time statistics.

## Key Components

- `ManagementRegistry`: Core interface managing the lifecycle of manageable objects within a JVM.
- `AbstractManagementRegistry`: Base implementation providing thread-safe object registration, lookup, capability discovery, and statistical polling.
- `ManagementProvider`: Adapter translating Java domain objects (e.g. Ehcache caches, offheap memory pools, data roots) into management capabilities and statistics.
- `ConsumerManagementRegistry`: Specialized registry streaming collected statistics and events directly to consumer callbacks or remote agents.
- `Collectable`: Marker interface for statistics collectors.

## How It Works

1. **Registration**: An entity or cache registers a manageable Java object:
   ```java
   registry.register(new CacheBinding("my-cache", cacheInstance));
   ```
2. **Capability Exposure**: The registry exposes descriptors (operations like `clear()`, metrics like `getHitCount()`).
3. **Execution & Sampling**: Remote management tools invoke actions or schedule periodic metric queries against the registry.

## Interactions & Dependencies

- **Dependencies**: `management:model`.
- **Consumed By**:
  - Terracotta Ehcache and Store clients.
  - Terracotta server plugins (`resources:offheap`, `resources:data-root`, `dynamic-config:entities:management`).
  - `management:entities:nms-agent` to bridge local registries to the cluster-wide management entity.
