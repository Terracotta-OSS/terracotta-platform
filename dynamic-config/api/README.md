# Dynamic Config API (`:dynamic-config:api`)

## Overview

The `dynamic-config-api` module defines the core service interfaces, listeners, events, and configuration change handlers used across clients, server entities, and CLI tools for dynamic configuration management.

## Key Components

- `DynamicConfigService`: Primary client/diagnostic interface for reading the active or upcoming `Cluster` topology and initiating configuration mutations.
- `DynamicConfigListener`: Callback interface notified when cluster configuration changes or activations occur.
- `DynamicConfigEvent`: Event payload describing configuration modifications, license changes, or topology updates.
- `DynamicConfigExtension`: Service provider interface allowing server plugins (like offheap, data roots, leases, security) to contribute configuration validators and change handlers.
- `License`: Interface representing cluster license state and capabilities.

## Interactions & Dependencies

- **Dependencies**: `dynamic-config:model`, `common:nomad`.
- **Consumed By**: `dynamic-config:server`, `dynamic-config:cli`, `dynamic-config:entities`, and server resource plugins.
