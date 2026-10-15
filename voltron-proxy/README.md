# Voltron Proxy (`:voltron-proxy`)

## Overview

The `voltron-proxy` module provides a high-level RPC framework that simplifies building Terracotta Voltron entities. Instead of manually serializing byte buffers, dispatching message codes, and handling entity endpoints, developers can define standard Java interfaces and let `voltron-proxy` handle message encoding, dynamic client proxies, active/passive server dispatching, active-to-passive synchronization, and server-to-client event messaging.

The module is structured into three subparts:
- **`common`**: Shared annotations, message descriptors, codecs, and execution strategies.
- **`client`**: Dynamic client proxy generator and `EntityClientService` implementation.
- **`server`**: Server-side entity dispatcher (`ActiveProxiedServerEntity`, `PassiveProxiedServerEntity`), server-to-client event messenger, and sync codecs.

---

## Architecture & Submodules

```
+---------------------------------------------------------------+
|                       Client Application                      |
|           (calls methods on typed Java interface)             |
+-------------------------------+-------------------------------+
                                | (Dynamic Proxy)
                                v
+---------------------------------------------------------------+
|                     :voltron-proxy:client                     |
|  - VoltronProxyInvocationHandler                              |
|  - ProxyEntityClientService                                   |
+-------------------------------+-------------------------------+
                                | (EntityMessage over network)
                                v
+---------------------------------------------------------------+
|                     :voltron-proxy:server                     |
|  - ActiveProxiedServerEntity  / PassiveProxiedServerEntity     |
|  - ProxyServerEntityService   / MessengerProxyFactory         |
+---------------------------------------------------------------+
```

### 1. `:voltron-proxy:common`

Defines shared abstractions, annotations, and wire formats:
- **Annotations**:
  - [`@Async`](voltron-proxy/common/src/main/java/org/terracotta/voltron/proxy/Async.java:27): Marks a method as non-blocking/asynchronous (returns `Future` or `CompletableFuture`).
  - [`@ClientId`](voltron-proxy/common/src/main/java/org/terracotta/voltron/proxy/ClientId.java:27): Injects the client's `ClientDescriptor` into a server-side method parameter.
  - [`@ConcurrencyStrategy`](voltron-proxy/common/src/main/java/org/terracotta/voltron/proxy/ConcurrencyStrategy.java:28): Customizes concurrency keys for multi-threaded server scheduling.
  - [`@ExecutionStrategy`](voltron-proxy/common/src/main/java/org/terracotta/voltron/proxy/ExecutionStrategy.java:28): Controls execution scope (`ACTIVE`, `BOTH`, `PASSIVE`).
- **Wire Codecs & Messages**:
  - [`ProxyEntityMessage`](voltron-proxy/common/src/main/java/org/terracotta/voltron/proxy/ProxyEntityMessage.java:22) / [`ProxyEntityResponse`](voltron-proxy/common/src/main/java/org/terracotta/voltron/proxy/ProxyEntityResponse.java:22): Binary message envelopes.
  - [`Codec`](voltron-proxy/common/src/main/java/org/terracotta/voltron/proxy/Codec.java:20) / [`SerializationCodec`](voltron-proxy/common/src/main/java/org/terracotta/voltron/proxy/SerializationCodec.java:26): Serializers for arguments and return values.

### 2. `:voltron-proxy:client`

Creates client entity instances and transparent proxies:
- [`ClientProxyFactory`](voltron-proxy/client/src/main/java/org/terracotta/voltron/proxy/client/ClientProxyFactory.java:29): Creates dynamic Java proxies implementing the target interface.
- [`VoltronProxyInvocationHandler`](voltron-proxy/client/src/main/java/org/terracotta/voltron/proxy/client/VoltronProxyInvocationHandler.java:31): Intercepts client method invocations, encodes them into `ProxyEntityMessage`, sends them to the server endpoint, and decodes the response.
- [`ProxyEntityClientService`](voltron-proxy/client/src/main/java/org/terracotta/voltron/proxy/client/ProxyEntityClientService.java:23): Standard Terracotta client service registry provider.

### 3. `:voltron-proxy:server`

Dispatches RPC requests to actual Java service instances on the server:
- [`ActiveProxiedServerEntity`](voltron-proxy/server/src/main/java/org/terracotta/voltron/proxy/server/ActiveProxiedServerEntity.java:32): Unpacks `ProxyEntityMessage`, reflects or invokes matching methods on target server objects, and returns execution results.
- [`PassiveProxiedServerEntity`](voltron-proxy/server/src/main/java/org/terracotta/voltron/proxy/server/PassiveProxiedServerEntity.java:28): Handles replicated state mutations on passive nodes.
- [`Messenger`](voltron-proxy/server/src/main/java/org/terracotta/voltron/proxy/server/Messenger.java:20) / [`MessengerProxyFactory`](voltron-proxy/server/src/main/java/org/terracotta/voltron/proxy/server/MessengerProxyFactory.java:24): Generates server-to-client event dispatchers.
- [`SyncProxyFactory`](voltron-proxy/server/src/main/java/org/terracotta/voltron/proxy/server/SyncProxyFactory.java:26): Automates active-to-passive data synchronization during server failover or attachment.

## Interactions & Dependencies

- **Dependencies**: Terracotta Entity API (`org.terracotta:entity-api`, `org.terracotta:standard-cluster-services`).
- **Consumed By**:
  - Custom entities across Terracotta platform and extensions needing typed RPC without boilerplate message encoding.
