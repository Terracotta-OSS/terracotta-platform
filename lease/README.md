# Lease (`:lease`)

## Overview

The `lease` module implements client connection leasing for Terracotta clusters. Connection leasing prevents "split-brain" client scenarios (where a disconnected or paused client continues issuing mutations against stale state) by requiring clients to periodically acquire and renew time-bounded leases from the active server. If a client fails to renew its lease before expiration (e.g. during a prolonged network partition or long GC pause), the client voluntarily kills its connection, and the server closes its session and reclaims locks/resources.

The module is structured into the following submodules:
- **`common`**: Wire protocol messages, Runnel codecs, and time source abstractions.
- **`client`**: Client-side lease maintainer, lease checking proxies, and connection killer threads.
- **`server`**: Server-side lease management service, lease monitor watchdog, connection closer, and dynamic config handlers.
- **`testing`**: Passthrough testing support and end-to-end integration test suites.

---

## Architecture & Submodules

```
+---------------------------------------------------------------+
|                      Client Application                       |
|           - LeasedConnection (wraps Terracotta Connection)    |
+-------------------------------+-------------------------------+
                                |
             +------------------+------------------+
             |                                     |
             v                                     v
+-------------------------------+     +-------------------------------+
|     LeaseMaintenanceThread    |     | LeaseExpiryConnectionKilling  |
|  - Periodically renews lease  |     |  - Kills connection if lease  |
|    before expiry timestamp    |     |    expires without renewal    |
+---------------+---------------+     +-------------------------------+
                |
                | (LeaseRequest via Runnel)
                v
+-------------------------------+
|         :lease:server         |
|  - ActiveLeaseAcquirer        |
|  - LeaseServiceImpl           |
|  - LeaseMonitorThread         |
|  - ClientConnectionCloser     |
+-------------------------------+
```

### 1. `:lease:common`

- [`LeaseMessage`](lease/common/src/main/java/org/terracotta/lease/LeaseMessage.java:23) / [`LeaseResponse`](lease/common/src/main/java/org/terracotta/lease/LeaseResponse.java:21): Data transfer objects for lease requests, grants, and reconnect negotiations.
- [`LeaseAcquirerCodec`](lease/common/src/main/java/org/terracotta/lease/LeaseAcquirerCodec.java:27): Runnel binary codec for lease frames.
- [`TimeSource`](lease/common/src/main/java/org/terracotta/lease/TimeSource.java:20): Time abstraction allowing deterministic time manipulation in unit tests.

### 2. `:lease:client`

- [`Lease`](lease/client/src/main/java/org/terracotta/lease/Lease.java:23): Immutable lease handle with expiration timestamp checking (`isValid()`).
- [`LeaseMaintainer`](lease/client/src/main/java/org/terracotta/lease/LeaseMaintainer.java:23) / [`LeaseMaintainerImpl`](lease/client/src/main/java/org/terracotta/lease/LeaseMaintainerImpl.java:27): Background lifecycle manager renewing leases asynchronously before they expire.
- [`LeasedConnection`](lease/client/src/main/java/org/terracotta/lease/connection/LeasedConnection.java:24): Wrapper around standard Terracotta `Connection` that validates lease validity before executing operations.
- [`LeaseExpiryConnectionKillingThread`](lease/client/src/main/java/org/terracotta/lease/LeaseExpiryConnectionKillingThread.java:24): Emergency watchdog thread that forcefully closes the connection if a lease expires.

### 3. `:lease:server`

- [`LeaseService`](lease/server/src/main/java/org/terracotta/lease/service/LeaseService.java:26) / [`LeaseServiceImpl`](lease/server/src/main/java/org/terracotta/lease/service/LeaseServiceImpl.java:31): Server-side service managing lease state, calculating grant durations, and tracking client timestamps.
- [`LeaseMonitorThread`](lease/server/src/main/java/org/terracotta/lease/service/monitor/LeaseMonitorThread.java:25): Scans active leases and triggers [`ClientConnectionCloser`](lease/server/src/main/java/org/terracotta/lease/service/closer/ClientConnectionCloser.java:23) for timed-out clients.
- [`ActiveLeaseAcquirer`](lease/server/src/main/java/org/terracotta/lease/ActiveLeaseAcquirer.java:28): Voltron active server entity processing lease requests.
- [`LeaseConfigChangeHandler`](lease/server/src/main/java/org/terracotta/lease/service/LeaseConfigChangeHandler.java:28): Dynamic config listener supporting runtime changes to `client-lease-duration`.

### 4. `:lease:testing`

- **`:lease:testing:passthrough`**: [`PassthroughLeasedConnectionService`](lease/testing/passthrough/src/main/java/org/terracotta/passthrough/connection/PassthroughLeasedConnectionService.java:25) provides in-memory simulated connection leasing for fast unit/integration testing without network sockets.
- **`:lease:testing:integration-tests`**: Integration tests verifying cluster lease behavior, network partition handling, and failover scenarios.

## Interactions & Dependencies

- **Dependencies**: `common:runnel`, `common:structures`, Terracotta Entity API (`org.terracotta:entity-api`).
- **Consumed By**:
  - Terracotta Clustered Client libraries (e.g. Ehcache Clustered, TCStore Clustered) when `client-lease-duration` is enabled in cluster configuration.
  - `dynamic-config` subsystem for dynamic lease duration configuration.
