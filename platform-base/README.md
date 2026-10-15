# Platform Base (`:platform-base`)

## Overview

The `platform-base` module provides foundational runtime metadata services for Terracotta server instances. It exposes identity and node information (such as server symbolic name and instance descriptors) to server-side entities and plugins.

## Key Components

- [`ServerInfo`](platform-base/src/main/java/org/terracotta/platform/ServerInfo.java:22): Model object holding server identification metadata, including symbolic name and node identifiers.
- [`ServerInfoProvider`](platform-base/src/main/java/org/terracotta/platform/ServerInfoProvider.java:26): Voltron service provider registering `ServerInfo` into the server runtime service registry.

## Interactions & Dependencies

- **Dependencies**: Terracotta Service APIs (`org.terracotta:terracotta-configuration-api`).
- **Consumed By**: Server plugins, dynamic config services, and management providers needing to discover the local server node's identity.
