# Dynamic Config Testing Suite (`:dynamic-config:testing`)

## Overview

The `dynamic-config:testing` suite provides test harnesses, mock entities, Galvan/Angela helpers, and integration test suites for verifying dynamic configuration behavior under single-node, multi-node, multi-stripe, failover, and network partition conditions.

## Submodules

- **`galvan`**: Galvan testing framework extensions for launching in-process and subprocess Terracotta clusters with dynamic config.
- **`support`**: Reusable assertion matchers, test repositories, mock Nomad clients, and cluster topology builders.
- **`entity`**: Test Voltron entities used to verify configuration change listeners and notification triggers.
- **`integration-tests`**: Comprehensive Angela test suite executing end-to-end scenarios:
  - Remote cluster activation with properties and CLI flags.
  - Live node and stripe attachment/detachment.
  - Dynamic setting mutations (offheap sizing, client lease durations, log directories).
  - Split-brain healing and Nomad transaction recovery.
