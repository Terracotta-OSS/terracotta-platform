# Dynamic Config Server Suite (`:dynamic-config:server`)

## Overview

The `dynamic-config:server` module suite provides the server-side runtime, bootstrap providers, and Voltron services that drive dynamic configuration, node bootstrapping, and Nomad 2PC consensus inside Terracotta server processes.

The suite is structured into three submodules:
- **`server:api`**: Server-side interfaces (`ConfigChangeHandler`, `DynamicConfigExtension`) allowing server plugins to participate in dynamic configuration changes.
- **`server:config-provider`**: Bootstraps the Terracotta server process using dynamic configuration repositories or property files.
- **`server:services`**: Implements the server-side `DynamicConfigService`, manages local `NomadServer` instances, coordinates two-phase commits, and applies configuration mutations to the live runtime.

---

## Architecture & Submodules

```
+---------------------------------------------------------------+
|                    Terracotta Server Main                     |
+-------------------------------+-------------------------------+
                                | (Bootstraps server)
                                v
+---------------------------------------------------------------+
|             :dynamic-config:server:config-provider            |
|  - DynamicConfigConfigurationProvider                         |
|  - Reads config-dir / .properties / CLI arguments             |
|  - Configures ports, logging, offheap, data roots             |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|                 :dynamic-config:server:services               |
|  - DynamicConfigServiceServerProvider                         |
|  - NomadServerManager (Nomad 2PC on server)                   |
|  - Coordinates with registered ConfigChangeHandler plugins    |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|                   :dynamic-config:server:api                  |
|  - ConfigChangeHandler SPI                                    |
|  - DynamicConfigExtension SPI                                 |
+---------------------------------------------------------------+
```

### 1. `:dynamic-config:server:api`

- [`ConfigChangeHandler`](dynamic-config/server/api/src/main/java/org/terracotta/dynamic_config/server/api/ConfigChangeHandler.java): SPI interface implemented by subsystems (offheap, data roots, leases, security) to validate proposed configuration changes (`tryApply()`) and apply them to the live runtime (`apply()`).
- [`DynamicConfigExtension`](dynamic-config/server/api/src/main/java/org/terracotta/dynamic_config/server/api/DynamicConfigExtension.java): SPI interface registering change handlers and validators during server startup.

### 2. `:dynamic-config:server:config-provider`

- [`DynamicConfigConfigurationProvider`](dynamic-config/server/config-provider/src/main/java/org/terracotta/dynamic_config/server/configuration/service/DynamicConfigConfigurationProvider.java): Implements Terracotta's server bootstrap configuration provider interface.
- Reads `--config-dir`, `--config-file`, or diagnostic startup flags, creates or validates Sanskrit repositories, and initializes server runtime configurations.

### 3. `:dynamic-config:server:services`

- Implements `DynamicConfigService` and exposes it via diagnostic endpoints.
- Manages local `NomadServer` instances backed by Sanskrit storage.
- Routes incoming Nomad `prepare`, `commit`, `rollback` operations to matching `ConfigChangeHandler`s.

## Interactions & Dependencies

- **Dependencies**: `dynamic-config:model`, `dynamic-config:repository`, `dynamic-config:json`, `common:nomad`, `common:sanskrit`, `diagnostic:server:api`, Terracotta Server API (`org.terracotta:terracotta-configuration-api`).
- **Consumed By**: Terracotta server core executable, server plugins, and dynamic config entities.
