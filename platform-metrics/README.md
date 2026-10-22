# Platform Metrics (`:platform-metrics`)

## Overview

The `platform-metrics` module implements periodic server metric logging for Terracotta server instances (TDB-19513). Every _N_ seconds (default 300 s, configurable via the system property `org.terracotta.platform.metrics.logging.period`) it emits a single pipe-delimited log line summarising the current server state and key runtime counters.

The log line is designed for easy parsing with `awk`, `cut`, and `grep`. All numeric fields are zero-padded and right-aligned; delta fields carry an explicit `+` or `-` sign.

```
STATS:1700000000:300|T:P|A,P|P:0010,+03,-02|S:0002,+01,-00|V:0002,+01,-00|C:0042,+10,-07|F:0125,+50,-45|EIC:+234|CIC:+100|SIC:+134|QIC:+00
```

### Header

`STATS:<unix>:<seconds>|T:<trigger>` — Unix timestamp (seconds), seconds since the last log line, and the trigger code.

| Trigger code | Cause |
|---|---|
| `P` | Normal periodic emission |
| `T` | Topology change (this server or a peer changed state) |
| `D` | On-demand (management request) |

A failover is not a trigger of its own: it surfaces as a `T` line, since the servers' states change.

### Topology field

The segment right after the trigger lists the current [`LogicalServerState`](#topology-state-codes) of every server in the stripe, in alphabetical `hostname:port` order, comma-separated.

```
A,P        ← one active, one passive
A,PS       ← one active, one passive-synchronizing
```

### Metric fields

| Field  | Meaning |
|--------|---------|
| `P`    | Physical connections — gauge / +opened / −closed |
| `S`    | Peer servers — gauge / +joined / −left |
| `V`    | Registered voters — gauge / +joined / −left |
| `C`    | Connected clients — gauge / +joined / −left |
| `F`    | Entity fetches held — gauge / +fetched / −released |
| `EIC`  | Total entity invocations — +delta during period |
| `CIC`  | Ehcache entity invocations — +delta during period |
| `SIC`  | TC Store entity invocations — +delta during period |
| `QIC`  | TC Sequence entity invocations — +delta during period |

---

## Topology state codes

| Code | `LogicalServerState` | Description |
|------|----------------------|-------------|
| `A`  | `ACTIVE`             | Active, ready to receive clients |
| `P`  | `PASSIVE`            | Passive standby, in sync with active |
| `PS` | `SYNCHRONIZING`      | Passive currently synchronizing with active |
| `T`  | `RELAY`              | Relay server (sends data to a replica) |
| `R`  | `REPLICA`            | Replica server (receiving data from relay) |
| `RB` | `REPLICA_SUSPENDED`  | Replica in suspended/starting state |
| `AR` | `ACTIVE_RECONNECTING`| Active with a reconnect window open |
| `AB` | `ACTIVE_SUSPENDED`   | Active blocked by consistency votes |
| `PB` | `PASSIVE_SUSPENDED`  | Passive blocked by consistency votes |
| `SB` | `START_SUSPENDED`    | Start suspended by consistency votes |
| `D`  | `DIAGNOSTIC`         | Server in diagnostic mode |
| `B`  | `BOOTSTRAP`          | Server bootstrapping |
| `S`  | `STARTING`           | Server starting up |
| `C`  | `RECOVERING`         | Server recovering |
| `?`  | `UNINITIALIZED` / `STOPPING` / `UNREACHABLE` / `UNKNOWN` | Server uninitialized or stopping, or state not determinable |

> **Note:** `AR`, `AB`, `PB`, `SB` are valid states in the model but are not normally observable
> via `PEER_STATE` events from peer servers — they are reported through separate management
> MBeans.
>
> **Note:** the core fires `PEER_STATE` on the active only. On a passive, the topology field lists only
> this server (its own state is read live from `Server#getState()`).

---

## Architecture

```
  terracotta-core (standard-cluster-services)
  ┌──────────────────────────┐
  │  MetricService (gauges)  │  ◄── addMetricSupplier() / fireEvent()
  │  EventService  (events)  │      called by server instrumentation points
  └────────────┬─────────────┘
               │ implemented by
               ▼
  ┌──────────────────────────────────────┐
  │   DefaultVoltronMetricCollector      │  stores Counter per Event, Gauge per Metric,
  │   (voltron)                          │  peers map updated by PEER_STATE events
  └────────────┬─────────────────────────┘
               │ wrapped by
               ▼
  ┌──────────────────────────────────────┐
  │   DefaultPlatformMetricService       │  typed getters + getAndResetMeter()
  │   (platform)                         │  + getStripeStates() (peers + this server)
  └────────────┬─────────────────────────┘
               │ read by
               ▼
  ┌──────────────────────────────────────┐
  │   MetricLogger                       │  daemon thread, runs every N seconds
  │   (log)                              │  also accepts logNow() for event-triggered lines
  └────────────┬─────────────────────────┘
               │ formats via
               ▼
  ┌──────────────────────────────────────┐
  │   MetricFormatter                    │  produces the pipe-delimited log line
  │   (log)                              │  including topology codes from getStripeStates()
  └──────────────────────────────────────┘

  MetricServiceProvider  (@BuiltinService)
    └── creates DefaultVoltronMetricCollector
    └── creates DefaultPlatformMetricService   (receives Server + collector)
    └── creates MetricLogger
    └── wraps collector in DelegateEventService (intercepts MEMBER_STATE + PEER_STATE
        to trigger immediate logNow() calls)
    └── listens for DynamicConfig TC_PROPERTIES changes to update the logging period
```

---

## Key Components

### `voltron` package
- [`VoltronMetricCollector`](src/main/java/org/terracotta/platform/metrics/voltron/VoltronMetricCollector.java) — interface for the raw event/gauge store; `count(Event, type)` registers an entity type to count; also exposes `getPeers()` returning the last known state of each peer server.
- [`DefaultVoltronMetricCollector`](src/main/java/org/terracotta/platform/metrics/voltron/DefaultVoltronMetricCollector.java) — implements `VoltronMetricCollector`, `MetricService` (gauges) and `EventService` (events); stores one `Counter` per `Event` (all created in the constructor), one `Gauge` per `Metric`, and updates the peers map on each `PEER_STATE` event (a peer reporting `STOP-STATE` is removed). `PEER_STATE` is a state report, not an occurrence: it only updates the peers map and is not counted. `MEMBER_STATE` describes this server only and is just counted. `fireEvent()` runs on core hot paths and is lock-free: it never creates counters. Per-entity-type counters exist only for types registered through `count(Event, type)` (done by `DefaultPlatformMetricService`'s constructor); events with an unregistered `type` only increment the global counter.
- [`DelegateEventService`](src/main/java/org/terracotta/platform/metrics/voltron/DelegateEventService.java) — thin `EventService` delegation wrapper; `MetricServiceProvider` subclasses it to intercept `MEMBER_STATE` and `PEER_STATE` events and trigger immediate log lines. The core sends the same state event several times, so a state event triggers a `TOPOLOGY_CHANGE` line only if `getStripeStates()` changed after the event was applied.

### `platform` package
- [`PlatformMetricService`](src/main/java/org/terracotta/platform/metrics/platform/PlatformMetricService.java) — interface exposing typed gauge/meter accessors and `getStripeStates()`.
- [`DefaultPlatformMetricService`](src/main/java/org/terracotta/platform/metrics/platform/DefaultPlatformMetricService.java) — wraps a `VoltronMetricCollector` and a `Server` reference; implements only the `getMetered*(boolean reset)` methods (reading a `MeteredCounter`, and atomically resetting it when `reset` is `true`); `getStripeStates()` merges the peers map with this server's live state.

The no-arg `getMetered*()` (read-only) and `getAndResetMetered*()` (read and reset) methods of `PlatformMetricService` are `default` methods delegating to `getMetered*(false)` and `getMetered*(true)`.

### `log` package
- [`MetricLogger`](src/main/java/org/terracotta/platform/metrics/log/MetricLogger.java) — schedules periodic log emission via a single daemon thread; `logNow(MetricLoggingTrigger)` queues an immediate emission without blocking the caller.
- [`MetricFormatter`](src/main/java/org/terracotta/platform/metrics/log/MetricFormatter.java) — stateless formatter producing the fixed-width pipe-delimited line including the topology segment. Field widths grow automatically when values exceed the minimum digit count. Meters are reset only for `PERIODIC` lines; other triggers read the meters without resetting them, so the periodic deltas stay intact.
- [`MetricLoggingTrigger`](src/main/java/org/terracotta/platform/metrics/log/MetricLoggingTrigger.java) — enum identifying the cause of each log line (`PERIODIC`, `TOPOLOGY_CHANGE`, `ON_DEMAND`); its single-character code is embedded in the header.

### `metric` package
- [`Counter`](src/main/java/org/terracotta/platform/metrics/metric/Counter.java) — monotonically increasing `LongAdder`-backed counter, built for many concurrent writers on core hot paths: `increment()` is lock-free, non-blocking and contention-free, while `get()` sums the internal cells (not an atomic snapshot, but never loses a completed increment and never goes backwards). Values stay in `[0, Long.MAX_VALUE]` and wrap to 0 on overflow.
- [`MeteredCounter`](src/main/java/org/terracotta/platform/metrics/metric/MeteredCounter.java) — wraps a `Counter` and tracks a baseline; `get(boolean reset)` returns the delta since the last reset and atomically resets the baseline when `reset` is `true`. `get()` and `getAndReset()` are shortcuts for `get(false)` and `get(true)`.
- [`Gauge`](src/main/java/org/terracotta/platform/metrics/metric/Gauge.java) — live snapshot supplied by a `LongSupplier` registered from the server core.

### Top-level
- [`MetricServiceProvider`](src/main/java/org/terracotta/platform/metrics/MetricServiceProvider.java) — Voltron `@BuiltinService` that wires all components together, provides both `MetricService` and `EventService` to the server service registry, and listens for dynamic-config `TC_PROPERTIES` changes to update the logging period at runtime.

---

## Configuration

| System property | Default | Description |
|---|---|---|
| `org.terracotta.platform.metrics.logging.period` | `300` | Logging period in seconds |

---

## Interactions & Dependencies

- **Depends on**: `terracotta-core:server-spi` (for the `MetricService` and `EventService` SPIs and `Server`), `diagnostic:model` (for `LogicalServerState`), Terracotta Entity API, SLF4J.
- **Consumed by**: The server runtime — `MetricServiceProvider` is auto-discovered as a `@BuiltinService` and the `MetricService` and `EventService` it provides are injected into core instrumentation points (`DistributedObjectServer`, `TCGroupManagerImpl`, `ManagedEntityImpl`, etc.) via the service registry.
