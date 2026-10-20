# Common Nomad (`:common:nomad`)

## Overview

The `nomad-engine` module implements a distributed Two-Phase Commit (2PC) protocol with consensus, recovery, and conflict-resolution mechanisms tailored for Terracotta dynamic configuration. Nomad guarantees that cluster-wide configuration changes either apply atomically across all nodes or safely roll back upon partial failure or crash.

## Protocol Mechanics & Lifecycle

Nomad uses a structured 4-phase transaction model:

```
           CLIENT                                                SERVERS
             |                                                      |
    [1. Double Discovery]                                           |
             |--- DiscoverMessage --------------------------------->| (reads state)
             |<-- DiscoverResponse (current version, state) --------|
             |--- Second DiscoverMessage -------------------------->| (re-verifies)
             |<-- DiscoverResponse ---------------------------------|
             |   (ClusterConsistencyChecker validates consensus)   |
             |                                                      |
    [2. Prepare Phase]                                              |
             |--- PrepareMessage (change UUID, NomadChange) ------->| (validates via
             |<-- AcceptRejectResponse (ACCEPT / REJECT) -----------|  ChangeApplicator)
             |                                                      |
    [3. Commit / Rollback Phase]                                    |
             |--- CommitMessage (or RollbackMessage) -------------->| (applies change to
             |<-- AcceptRejectResponse -----------------------------|  live model & disk)
             |                                                      |
    [4. Recovery / Takeover Phase (if in-doubt/client crash)]       |
             |--- TakeoverMessage --------------------------------->| (assumes control
             |<-- DiscoverResponse ---------------------------------|  of in-doubt tx)
```

1. **Two-Round Discovery (`doubleDiscovery`)**: The client queries all nodes twice (`sendDiscovers` then `sendSecondDiscovers`). [`ClusterConsistencyChecker`](common/nomad/src/main/java/org/terracotta/nomad/client/ClusterConsistencyChecker.java:27) verifies that all target nodes are in a consistent state before any mutation is attempted.
2. **Prepare (`prepare`)**: The client sends a proposed [`NomadChange`](common/nomad/src/main/java/org/terracotta/nomad/client/change/NomadChange.java:18). Each server validates the change against its live state using its registered [`ChangeApplicator`](common/nomad/src/main/java/org/terracotta/nomad/server/ChangeApplicator.java:23) and transitions into `PREPARED` state.
3. **Commit / Rollback**: If all nodes accept, the client issues `CommitMessage` making the change effective (`COMMITTED`). If any node rejects (reasons: `BAD`, `DEAD`, `UNACCEPTABLE`), the client issues `RollbackMessage` (`ROLLED_BACK`).
4. **Takeover & Recovery**: If a client crashes mid-transaction leaving prepared state on servers, subsequent clients can initiate a [`RecoveryProcess`](common/nomad/src/main/java/org/terracotta/nomad/client/recovery/RecoveryProcess.java:31) using [`TakeoverMessage`](common/nomad/src/main/java/org/terracotta/nomad/messages/TakeoverMessage.java:22) to resolve in-doubt transactions.

## Key Components

### Client Components
- [`NomadClient`](common/nomad/src/main/java/org/terracotta/nomad/client/NomadClient.java:33): Facade for triggering changes, recoveries, and discoveries.
- [`NomadClientProcess`](common/nomad/src/main/java/org/terracotta/nomad/client/NomadClientProcess.java:28): Abstract workflow orchestrating discovery, decision logic, and completion.
- [`ChangeProcess`](common/nomad/src/main/java/org/terracotta/nomad/client/change/ChangeProcess.java:29): Concrete 2PC executor applying new configuration changes.
- [`RecoveryProcess`](common/nomad/src/main/java/org/terracotta/nomad/client/recovery/RecoveryProcess.java:31): Resolves in-doubt transactions across cluster nodes.
- [`DiscoveryProcess`](common/nomad/src/main/java/org/terracotta/nomad/client/status/DiscoveryProcess.java:29): Discovers configuration versions and uncommitted states.
- [`NomadEndpoint`](common/nomad/src/main/java/org/terracotta/nomad/client/NomadEndpoint.java:22): Abstract connection handle to a single Nomad server node.

### Server Components
- [`NomadServer`](common/nomad/src/main/java/org/terracotta/nomad/server/NomadServer.java:28): Server interface receiving 2PC messages.
- [`NomadServerImpl`](common/nomad/src/main/java/org/terracotta/nomad/server/NomadServerImpl.java:46): Server implementation managing state transitions and coordinating with `ChangeApplicator`.
- [`NomadServerState`](common/nomad/src/main/java/org/terracotta/nomad/server/state/NomadServerState.java:25): Interface for persisting transaction states, mode (`ACCEPTING`, `PREPARED`), and mutation history.
- [`ChangeApplicator`](common/nomad/src/main/java/org/terracotta/nomad/server/ChangeApplicator.java:23): Subsystem hook validating proposed changes and applying them to the runtime configuration upon commit.

## Interactions & Dependencies

- **Dependencies**: None (pure protocol and algorithmic engine).
- **Consumed By**:
  - `dynamic-config:entities:nomad` for exposing Nomad 2PC over Voltron entity RPC.
  - `dynamic-config:server:services` for implementing Nomad server persistence with Sanskrit.
  - `dynamic-config:cli:config-tool` for applying live cluster configuration mutations.
