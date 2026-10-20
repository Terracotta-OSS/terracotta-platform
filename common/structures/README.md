# Common Structures (`:common:structures`)

## Overview

The `terracotta-structures` module provides typed representations of units of measure, time budgets, tuples, and semantic version identifiers used across all Terracotta Platform modules.

## Key Components

- [`Measure<T extends Unit<T>>`](common/structures/src/main/java/org/terracotta/common/struct/Measure.java:31): Strongly typed measure with value and unit. Supports parsing human-readable strings (e.g. `"512MB"`, `"10s"`), mathematical conversions, comparisons, and arithmetic.
- [`MemoryUnit`](common/structures/src/main/java/org/terracotta/common/struct/MemoryUnit.java:23): Standard byte magnitude units (`B`, `KB`, `MB`, `GB`, `TB`) with conversion routines.
- [`TimeUnit`](common/structures/src/main/java/org/terracotta/common/struct/TimeUnit.java:24): Standard time units (`MILLISECONDS`, `SECONDS`, `MINUTES`, `HOURS`) with conversions to and from standard `java.util.concurrent.TimeUnit`.
- [`TimeBudget`](common/structures/src/main/java/org/terracotta/common/struct/TimeBudget.java:25): Tracks remaining execution time for timeouts across multi-step distributed operations.
- [`Tuple2`](common/structures/src/main/java/org/terracotta/common/struct/Tuple2.java:23) / [`Tuple3`](common/structures/src/main/java/org/terracotta/common/struct/Tuple3.java:23): Generic 2-element and 3-element immutable tuple containers.
- [`Version`](common/structures/src/main/java/org/terracotta/common/struct/Version.java:25): Semantic versioning parser and comparator for component compatibility checks.

## How It Works

1. **Measurement Parsing**:
   ```java
   Measure<MemoryUnit> offheap = Measure.parse("2GB", MemoryUnit.class);
   long bytes = offheap.getQuantity(MemoryUnit.B); // 2147483648L
   ```
2. **Time Budgeting**:
   ```java
   TimeBudget budget = new TimeBudget(30, TimeUnit.SECONDS);
   // In loop or across calls:
   if (budget.remaining(TimeUnit.MILLISECONDS) <= 0) {
     throw new TimeoutException();
   }
   ```

## Interactions & Dependencies

- **Dependencies**: None.
- **Consumed By**:
  - `dynamic-config:model` for cluster configuration settings (memory limits, client lease duration, reconnect windows).
  - `resources:offheap` for offheap resource sizing and allocation limits.
  - `lease:common` and `diagnostic:client` for timeout handling.
