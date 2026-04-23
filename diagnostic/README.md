# Diagnostic Subsystem (`:diagnostic`)

## Overview

The `diagnostic` subsystem provides an out-of-band administration, inspection, and RPC channel into Terracotta server nodes. It allows CLI tools, orchestrators, and diagnostic clients to inspect server state (e.g. `ACTIVE`, `PASSIVE`, `DIAGNOSTIC`), retrieve kit/version info, list connected clients, dump thread stacks, and execute configuration commands even when the cluster is not yet activated or is in diagnostic mode.

The subsystem is organized into five submodules:
- **`model`**: Data models for logical server states, connected client information, and kit metadata.
- **`common`**: Diagnostic request/response wire protocol and codecs (JSON, Java serialization).
- **`client`**: Client API and concurrent connection providers for interacting with remote diagnostic endpoints.
- **`server:api`**: Server-side registration interfaces and the `@Expose` annotation for diagnostic services.
- **`server:services`**: Voltron server service implementation and default MBean extensions.

---

## Submodules & Architecture

```
+---------------------------------------------------------------+
|                      Diagnostic Clients                       |
|         (CLI config-tool, cluster health tools, Angela)       |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|                      :diagnostic:client                       |
|  - DiagnosticService / DiagnosticServiceImpl                  |
|  - DiagnosticServiceProvider / ConcurrentDiagnosticService    |
+-------------------------------+-------------------------------+
                                |
                                | DiagnosticRequest (JSON/Java)
                                v
+---------------------------------------------------------------+
|                 :diagnostic:server:services                   |
|  - DiagnosticRequestHandler                                   |
|  - Dispatches to registered services and MBeans               |
+-------------------------------+-------------------------------+
                                |
                                v
+---------------------------------------------------------------+
|                    :diagnostic:server:api                     |
|  - DiagnosticServices registration registry                   |
|  - Services exposed with @Expose annotation                   |
+---------------------------------------------------------------+
```

### 1. `:diagnostic:model`

- [`LogicalServerState`](diagnostic/model/src/main/java/org/terracotta/diagnostic/model/LogicalServerState.java:23): Unified state enum representing `BOOTSTRAP`, `STARTING`, `UNINITIALIZED`, `SYNCHRONIZING`, `RECOVERING`, `ACTIVE`, `PASSIVE`, `DIAGNOSTIC`, `STOPPING`, `UNKNOWN`, and stopped states.
- [`ConnectedClientInformation`](diagnostic/model/src/main/java/org/terracotta/diagnostic/model/ConnectedClientInformation.java:25): Metadata on connected clients (client UUID, host, PID, connection timestamp).
- [`KitInformation`](diagnostic/model/src/main/java/org/terracotta/diagnostic/model/KitInformation.java:24): Version, build number, and build timestamp of the Terracotta installation.

### 2. `:diagnostic:common`

- [`DiagnosticRequest`](diagnostic/common/src/main/java/org/terracotta/diagnostic/common/DiagnosticRequest.java:24) / [`DiagnosticResponse`](diagnostic/common/src/main/java/org/terracotta/diagnostic/common/DiagnosticResponse.java:24): Envelope objects wrapping service interface, method name, argument types, and return values.
- [`DiagnosticCodec`](diagnostic/common/src/main/java/org/terracotta/diagnostic/common/DiagnosticCodec.java:20): Codec interface with implementations:
  - [`JsonDiagnosticCodec`](diagnostic/common/src/main/java/org/terracotta/diagnostic/common/JsonDiagnosticCodec.java:26): JSON encoding using `common:json`.
  - [`JavaDiagnosticCodec`](diagnostic/common/src/main/java/org/terracotta/diagnostic/common/JavaDiagnosticCodec.java:25): Standard Java object serialization.

### 3. `:diagnostic:client`

- [`DiagnosticService`](diagnostic/client/src/main/java/org/terracotta/diagnostic/client/DiagnosticService.java:27): Primary client interface providing typed proxy invocation (`getProxy(Class<T>)`) and logical state queries (`getLogicalServerState()`).
- [`DiagnosticServiceProvider`](diagnostic/client/src/main/java/org/terracotta/diagnostic/client/connection/DiagnosticServiceProvider.java:23): Factory connecting to a server endpoint over the diagnostic port.
- [`ConcurrentDiagnosticServiceProvider`](diagnostic/client/src/main/java/org/terracotta/diagnostic/client/connection/ConcurrentDiagnosticServiceProvider.java:29): Connects to multiple server endpoints simultaneously for parallel cluster inspection.

### 4. `:diagnostic:server:api`

- [`DiagnosticServices`](diagnostic/server/api/src/main/java/org/terracotta/diagnostic/server/api/DiagnosticServices.java:23): Server-side static registry for registering diagnostic service implementations.
- [`@Expose`](diagnostic/server/api/src/main/java/org/terracotta/diagnostic/server/api/Expose.java:27): Annotation marking service interfaces or methods to be queryable by remote diagnostic clients.

### 5. `:diagnostic:server:services`

- [`DiagnosticRequestHandler`](diagnostic/server/services/src/main/java/org/terracotta/diagnostic/server/DiagnosticRequestHandler.java:31): Receives incoming wire requests, finds matching registered services, invokes methods reflectively, and returns responses.
- [`DiagnosticExtensionsMBeanImpl`](diagnostic/server/services/src/main/java/org/terracotta/diagnostic/server/extensions/DiagnosticExtensionsMBeanImpl.java:31): Exposes built-in server diagnostics (state, threads, kit info).

## Interactions & Dependencies

- **Dependencies**: `common:json`, `common:inet`, `common:structures`, Terracotta Server API (`org.terracotta:terracotta-configuration-api`).
- **Consumed By**:
  - `dynamic-config:cli:config-tool` for node discovery, cluster activation, dynamic property updates, and topology queries.
  - `voter` CLI for monitoring node states and voting participation.
  - Integration testing frameworks (Angela) to wait for active/passive states.
