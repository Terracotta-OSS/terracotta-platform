# Resources Suite (`:resources`)

## Overview

The `resources` directory hosts Terracotta server resource management modules responsible for allocating, partitioning, and monitoring hardware resources (direct offheap memory and local storage roots) on server nodes.

## Submodules

- [`:resources:data-root`](resources/data-root/README.md): Configures and manages durable disk storage paths (`data-dirs`) for persistent server entities and metadata.
- [`:resources:offheap`](resources/offheap/README.md): Configures and monitors named direct offheap memory pools (`offheap-resources`) used by in-memory stores and caches.
