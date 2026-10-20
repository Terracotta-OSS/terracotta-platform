# Client Message Tracker (`:client-message-tracker`)

## Overview

The `client-message-tracker` module provides message deduplication, tracking, and Once-and-Only-Once (OOO) invocation guarantees for Terracotta Voltron entities. When clients reconnect or retry requests after network timeouts, this service verifies whether an incoming message was already executed and returns the previously recorded response instead of executing the mutation twice.

## Key Components

- [`Tracker`](client-message-tracker/src/main/java/org/terracotta/client/message/tracker/Tracker.java:23) / [`TrackerImpl`](client-message-tracker/src/main/java/org/terracotta/client/message/tracker/TrackerImpl.java:27): Global registry tracking active client sessions and message sequences.
- [`ClientTracker`](client-message-tracker/src/main/java/org/terracotta/client/message/tracker/ClientTracker.java:23) / [`ClientTrackerImpl`](client-message-tracker/src/main/java/org/terracotta/client/message/tracker/ClientTrackerImpl.java:24): Tracks messages, inflight transactions, and completed results for a single client identifier (`ClientSourceId`).
- [`OOOMessageHandler`](client-message-tracker/src/main/java/org/terracotta/client/message/tracker/OOOMessageHandler.java:36): Once-and-Only-Once message handler managing duplicate detection windows, cached responses, and active-to-passive message synchronization.
- [`OOOMessageHandlerProvider`](client-message-tracker/src/main/java/org/terracotta/client/message/tracker/OOOMessageHandlerProvider.java:26): Voltron server service provider exposing message tracker services to server entities.

## How It Works

1. **Message Submission**: An active server entity receives a client message containing a sequence ID.
2. **Duplicate Check**: The entity calls `OOOMessageHandler.invoke(message, execution)`. If the message sequence was already processed, the cached response is immediately returned.
3. **Execution & Recording**: If new, the message is executed and its result is recorded in `ClientTracker`.
4. **Replication & Sync**: Recorded message states are synchronized from active to passive servers so failover does not lose transaction history.

## Interactions & Dependencies

- **Dependencies**: Terracotta Entity and Service APIs (`org.terracotta:entity-api`, `org.terracotta:standard-cluster-services`).
- **Consumed By**: Voltron server entities requiring idempotency and exactly-once execution guarantees (e.g. key-value stores, distributed maps, transactional caches).
