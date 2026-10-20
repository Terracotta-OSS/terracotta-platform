# Management Model (`:management:model`)

## Overview

The `management-model` module defines the core data model, capability descriptors, context structures, notifications, statistic schemas, and remote call abstractions used throughout Terracotta's management and monitoring subsystem.

## Key Components

- **Context & Identification**:
  - `Context`: Key-value map uniquely identifying a manageable object (e.g., `appName=my-app, cacheManagerName=cm1, cacheName=cacheA`).
  - `ContextContainer`: Hierarchical tree describing valid context identifiers for an application or entity.
- **Capabilities & Descriptors**:
  - `Capability`: Declares a set of actions or statistics exposed by a managed component.
  - `Descriptor`: Base descriptor interface with implementations like `ActionDescriptor` (manageable methods) and `StatisticDescriptor` (metrics).
- **Statistics & Metrics**:
  - `Statistic`: Represents a collected metric sample (gauges, counters, rates, latencies, percentiles, histograms).
  - `StatisticConfiguration`: Parameters controlling sampling frequency, window sizes, and historical retention.
- **Notifications & Events**:
  - `ContextualNotification`: Emitted event when topology or lifecycle events occur (e.g. cache cleared, node joined).
  - `ContextualReturn`: Return value from remote management method invocations.
- **JSON Serialization**:
  - `ManagementJsonModule`: Module for `common:json` serializing all management model objects to JSON.

## Interactions & Dependencies

- **Dependencies**: `management:sequence-generator`, `common:json`.
- **Consumed By**:
  - `management:registry` to structure manageable object bindings.
  - `management:entities:nms` and `management:entities:nms-agent` to serialize management data over the wire.
  - Terracotta Management Server (TMS) and Management Console (TMC).
