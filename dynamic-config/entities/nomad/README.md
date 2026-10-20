# Dynamic Config Nomad Entity (`:dynamic-config:entities:nomad`)

## Overview

The `dynamic-config:entities:nomad` module exposes the Nomad Two-Phase Commit (2PC) protocol over Terracotta Voltron Entity RPC. It allows external clients (such as `config-tool`) to execute distributed configuration discovery, prepare, commit, rollback, and recovery transactions against active server entities.

The module is structured into three submodules:
- **`common`**: Entity messages, responses, codecs, and serialization adapters.
- **`client`**: Client entity (`NomadEntity`) adapting Voltron entity endpoints to the standard [`NomadEndpoint`](common/nomad/src/main/java/org/terracotta/nomad/client/NomadEndpoint.java:22) interface.
- **`server`**: Active and passive server entities (`NomadActiveEntity`, `NomadPassiveEntity`) forwarding 2PC operations to the server's local `NomadServerManager`.

---

## Architecture & Submodules

```
+---------------------------------------------------------------+
|                      NomadClient (config-tool)                |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|             :dynamic-config:entities:nomad:client             |
|  - NomadEntity / NomadEntityClientService                     |
|  - Implements NomadEndpoint<NodeContext>                      |
+-------------------------------+-------------------------------+
                                |
                                | (Voltron Entity RPC Messages)
                                v
+---------------------------------------------------------------+
|             :dynamic-config:entities:nomad:server             |
|  - NomadActiveEntity / NomadPassiveEntity                     |
|  - Dispatches to server-side NomadServerManager               |
+---------------------------------------------------------------+
```

### 1. `:dynamic-config:entities:nomad:common`

- Encapsulates Nomad protocol messages (`DiscoverMessage`, `PrepareMessage`, `CommitMessage`, `RollbackMessage`, `TakeoverMessage`) into Voltron entity message frames using `dynamic-config:json`.

### 2. `:dynamic-config:entities:nomad:client`

- Adapts Terracotta client entity connections to the `NomadEndpoint` interface expected by `common:nomad`'s `NomadClient`.

### 3. `:dynamic-config:entities:nomad:server`

- Implements `NomadActiveEntity` and `NomadPassiveEntity`.
- Delegates transaction phases to the server's `NomadServerManager` and replicates state changes to passive entities.

## Interactions & Dependencies

- **Dependencies**: `common:nomad`, `dynamic-config:model`, `dynamic-config:json`, `dynamic-config:server:api`, Terracotta Entity API (`org.terracotta:entity-api`).
- **Consumed By**: `dynamic-config:cli:config-tool` for applying runtime cluster configuration changes.
