# Resources Offheap (`:resources:offheap`)

## Overview

The `offheap` module manages server-side offheap memory resources (`offheap-resources`) on Terracotta nodes. It allows allocating, reserving, freeing, and monitoring dedicated pools of direct memory outside the Java garbage collector heap.

## Key Components

- [`OffHeapResource`](resources/offheap/src/main/java/org/terracotta/offheapresource/OffHeapResource.java:23): Represents a single named offheap memory pool with allocation methods (`reserve(long)`, `release(long)`, `getAvailable()`).
- [`OffHeapResources`](resources/offheap/src/main/java/org/terracotta/offheapresource/OffHeapResources.java:23): Service providing lookup of configured offheap pools by name.
- [`OffHeapResourcesProvider`](resources/offheap/src/main/java/org/terracotta/offheapresource/OffHeapResourcesProvider.java:31): Voltron service provider registering offheap resources in the server runtime.
- [`OffheapResourceConfigChangeHandler`](resources/offheap/src/main/java/org/terracotta/offheapresource/OffheapResourceConfigChangeHandler.java:28): Dynamic config listener handling dynamic increases or additions of offheap memory resources.
- [`OffHeapResourceStatisticsManagementProvider`](resources/offheap/src/main/java/org/terracotta/offheapresource/management/OffHeapResourceStatisticsManagementProvider.java:27): Exposes allocation metrics, max capacity, and consumption statistics to the Terracotta Management Server.

## Interactions & Dependencies

- **Dependencies**: `common:structures`, `dynamic-config:server:api`, `management:model`, `management:registry`.
- **Consumed By**:
  - Clustered cache and store entities (e.g. Ehcache clustered tiers) to allocate offheap memory storage.
  - Dynamic config CLI tools for defining and resizing offheap memory pools.
