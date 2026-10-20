# Dynamic Config Model (`:dynamic-config:model`)

## Overview

The `dynamic-config-model` module defines the immutable domain model for Terracotta 10.x+ dynamic cluster topologies and configurations. It defines clusters, stripes, server nodes, configuration settings, property keys, validation rules, and difference/comparator mechanisms.

## Key Components

- **Topology Model**:
  - `Cluster`: Represents an entire cluster containing one or more `Stripe`s, cluster-level settings (offheap resources, failover priority, client lease duration), and metadata.
  - `Stripe`: Represents a failover group containing one or more `Node` instances (active and passives).
  - `Node`: Represents a single server process with its network ports (port, group-port), directories (data-dirs, metadata-dir, log-dir, config-dir), and node settings.
  - `NodeContext`: Context object associating a `Cluster` topology with a specific target `Node` identifier or stripe ID.
- **Settings & Properties**:
  - `Setting`: Enum representing all configurable properties in Terracotta (`OFFHEAP_RESOURCES`, `DATA_DIRS`, `FAILOVER_PRIORITY`, `CLIENT_LEASE_DURATION`, `CLIENT_RECONNECT_WINDOW`, `SECURITY_DIR`, etc.).
  - `Props`: Utility for parsing and manipulating `.properties` files and property streams.
  - `ConfigProperties`: Formats a `Cluster` into standard configuration property syntax.
- **Validation**:
  - `ClusterValidator`: Verifies cluster consistency (unique ports, valid failover priorities, consistent offheap definitions, valid IP/hostnames).

## How It Works

1. **Hierarchy**: `Cluster` -> `Stripe` -> `Node`.
2. **Mutation via Clones**: All model objects are cloneable and support mutation pipelines that produce validated new instances.
3. **Property Serialization**: Clusters can be parsed from or dumped to human-readable property files (e.g. `stripe.1.node.1.data-dirs.main=data/main`).

## Interactions & Dependencies

- **Dependencies**: `common:structures`, `common:inet`.
- **Consumed By**: All `dynamic-config` modules, `diagnostic:model`, CLI configuration tools, and server startup providers.
