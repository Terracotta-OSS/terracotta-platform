# Dynamic Config Management Entity (`:dynamic-config:entities:management`)

## Overview

The `dynamic-config:entities:management:server` module bridges the dynamic configuration subsystem to the Terracotta Management framework (`:management`). It registers management bindings and providers so that the Terracotta Management Server (TMS) and web consoles can inspect cluster configuration, view node topology, and monitor configuration changes.

## Key Components

- `DynamicConfigSettingsManagementProvider`: Management provider exposing cluster, stripe, and node configuration settings as manageable properties.
- `DynamicConfigBinding`: Binds the server's `DynamicConfigService` into the local `ManagementRegistry`.

## Interactions & Dependencies

- **Dependencies**: `dynamic-config:server:api`, `management:model`, `management:registry`.
- **Consumed By**: `:management:server:services` and `:management:entities:nms` for monitoring cluster configuration.
