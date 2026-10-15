# Management Sequence Generator (`:management:sequence-generator`)

## Overview

The `sequence-generator` module produces globally unique, ordered 64-bit sequence tokens and IDs used across distributed management events, statistics sampling, and notification logs in Terracotta.

## Key Components

- `SequenceGenerator`: Interface generating sequential, ordered token identifiers.
- `TimeSequenceGenerator`: Generates time-based monotonically increasing sequence IDs incorporating timestamp, node/JVM identifier, and local counter.

## Interactions & Dependencies

- **Dependencies**: None.
- **Consumed By**: `management:model` to timestamp and sequence management events and notifications.
