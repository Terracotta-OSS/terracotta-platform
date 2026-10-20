# Common JSON (`:common:json`)

## Overview

The `terracotta-json` module provides a flexible, modular JSON serialization and deserialization layer built on top of Google Gson. It standardizes type adapters, polymorphism handling, Java 8 time (JSR-310) support, path serializers, and cycle-safe module composition for Terracotta subsystems.

## Key Components

- [`Json`](common/json/src/main/java/org/terracotta/json/Json.java:31): Factory and builder entry point creating pre-configured `Gson` instances with standard Terracotta extensions.
- [`GsonModule`](common/json/src/main/java/org/terracotta/json/gson/GsonModule.java:23): Plugin interface allowing components to register custom type adapters and type hierarchies into the JSON builder.
- [`RuntimeTypeAdapterFactory`](common/json/src/main/java/org/terracotta/json/gson/RuntimeTypeAdapterFactory.java:37): Enables runtime polymorphism by injecting and reading `@class` or custom discriminator properties in JSON payloads.
- [`JdkJsonModule`](common/json/src/main/java/org/terracotta/json/JdkJsonModule.java:25): Built-in module providing adapters for JDK types (e.g., `Optional`, `Path`, `Class`, floating points).
- [`Jsr310JsonModule`](common/json/src/main/java/org/terracotta/json/Jsr310JsonModule.java:25): Built-in module providing adapters for Java 8 Date and Time classes (`Instant`, `Duration`, `LocalDate`, `LocalDateTime`).
- [`TerracottaJsonModule`](common/json/src/main/java/org/terracotta/json/TerracottaJsonModule.java:25): Module binding Terracotta-specific common structures.

## How It Works

1. **Module Composition**: Modules implement [`GsonModule`](common/json/src/main/java/org/terracotta/json/gson/GsonModule.java:23). When [`DefaultJsonFactory`](common/json/src/main/java/org/terracotta/json/DefaultJsonFactory.java:28) builds a `Json` instance, it builds a dependency graph with [`DirectedGraph`](common/json/src/main/java/org/terracotta/json/util/DirectedGraph.java:23) to order modules topologically.
2. **Polymorphic Serialization**: Subclasses registered with `RuntimeTypeAdapterFactory` automatically serialize discriminator fields to allow precise round-trip reconstruction.
3. **Safe Serialization**: Provides null-safe adapters, path normalizing adapters, and exclusion strategies to prevent leaking internal fields.

## Interactions & Dependencies

- **Dependencies**: Google `gson`.
- **Consumed By**:
  - `common:sanskrit` for state object serialization to disk.
  - `diagnostic:common` for diagnostic protocol JSON message encoding.
  - `dynamic-config:json` for configuration topology and change serialization.
  - `management:model` for REST/JSON serialization of management entities.
