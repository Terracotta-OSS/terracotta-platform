# Healthchecker (`:healthchecker`)

## Overview

The `healthchecker` module provides an active heartbeat and liveness detection mechanism between Terracotta clients and server instances. It ensures that unresponsive servers, hung network connections, or half-open TCP sockets are promptly detected on the client side, triggering failover or reconnect workflows before operations hang indefinitely.

The module is divided into three parts:
- **`common`**: Runnel-based wire protocol and heartbeat request/response data objects.
- **`client`**: Client-side background heartbeat sender, timeout manager, moving average latency calculator, and listener notifications.
- **`server`**: Server-side entity replying immediately to heartbeat probes.

---

## Submodules & Architecture

```
+-------------------------------------------------------------------+
|                        HealthCheckerClient                        |
|  - Fetches entity: "staticHealthChecker"                          |
|  - Timer driver runs periodic pings (1 - 120 cycles/minute)       |
|  - Tracks microsecond ping moving average                         |
+---------------------------------+---------------------------------+
                                  | (HealthCheckReq via Runnel)
                                  v
+-------------------------------------------------------------------+
|                        HealthCheckerServer                        |
|  - Immediately echoes probe with HealthCheckRsp                   |
+---------------------------------+---------------------------------+
                                  | (HealthCheckRsp via Runnel)
                                  v
+-------------------------------------------------------------------+
|  - On response: updates moving average ping latency               |
|  - If probe unanswered within timeout threshold:                  |
|    1. Fires TimeoutListener.probeFailed(connection)               |
|    2. If elapsed >= probeTimeoutInMillis:                         |
|       Closes root connection & fires connectionClosed(connection) |
+-------------------------------------------------------------------+
```

### 1. `:healthchecker:common`

- [`HealthCheckReq`](healthchecker/common/src/main/java/org/terracotta/healthchecker/HealthCheckReq.java:21): Ping request containing sequence token (`ping-<iteration>`).
- [`HealthCheckRsp`](healthchecker/common/src/main/java/org/terracotta/healthchecker/HealthCheckRsp.java:21): Ping response echoing the sequence payload.
- [`HealthCheckerCodec`](healthchecker/common/src/main/java/org/terracotta/healthchecker/HealthCheckerCodec.java:28): High-performance Runnel binary codec.

### 2. `:healthchecker:client`

- [`HealthCheckerFactory`](healthchecker/client/src/main/java/org/terracotta/healthchecker/HealthCheckerFactory.java:43): Factory that fetches the `"staticHealthChecker"` entity and attaches a [`TimeoutManager`](healthchecker/client/src/main/java/org/terracotta/healthchecker/TimeoutManager.java:23) to a `Connection`.
- [`HealthCheckerClient`](healthchecker/client/src/main/java/org/terracotta/healthchecker/HealthCheckerClient.java:30): Client entity invoking heartbeat calls.
- [`TimeoutListener`](healthchecker/client/src/main/java/org/terracotta/healthchecker/TimeoutListener.java:21): Callback interface with `probeFailed(Connection)` and `connectionClosed(Connection)` hooks.

### 3. `:healthchecker:server`

- [`HealthCheckerServer`](healthchecker/server/src/main/java/org/terracotta/healthchecker/HealthCheckerServer.java:24): Active server entity that instantly returns responses for incoming ping messages.
- [`HealthCheckServerEntityService`](healthchecker/server/src/main/java/org/terracotta/healthchecker/HealthCheckServerEntityService.java:26): Terracotta entity service registering `"staticHealthChecker"`.

## Interactions & Dependencies

- **Dependencies**: `common:runnel`, Terracotta Entity API (`org.terracotta:entity-api`).
- **Consumed By**: Terracotta client connection factories (Ehcache Clustered, Store Clustered) to continuously verify cluster liveness.
