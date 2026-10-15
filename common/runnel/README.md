# Common Runnel (`:common:runnel`)

## Overview

`runnel` is a lightweight, high-performance binary schema-based serialization and deserialization library for Java, designed specifically for Terracotta wire protocols and Voltron entity messages. It provides compact binary encoding with zero reflection during serialization/deserialization, strict field ordering, schema evolution compatibility, and variable-length integer encoding (VLQ).

## Key Components

- [`Struct`](common/runnel/src/main/java/org/terracotta/runnel/Struct.java:31): Represents an immutable binary schema definition containing typed fields, nested structs, and arrays.
- [`StructBuilder`](common/runnel/src/main/java/org/terracotta/runnel/StructBuilder.java:30): Fluent builder used to construct a `Struct` schema.
- [`StructEncoder`](common/runnel/src/main/java/org/terracotta/runnel/encoding/StructEncoder.java:27): High-speed encoder writing typed primitive values, strings, byte buffers, enums, sub-structs, and arrays into binary buffers.
- [`StructDecoder`](common/runnel/src/main/java/org/terracotta/runnel/decoding/StructDecoder.java:27): High-speed decoder extracting fields from encoded binary byte buffers.
- [`EnumMapping`](common/runnel/src/main/java/org/terracotta/runnel/EnumMapping.java:23) / [`EnumMappingBuilder`](common/runnel/src/main/java/org/terracotta/runnel/EnumMappingBuilder.java:24): Maps Java `enum` constants to compact integer values over the wire.
- [`VLQ`](common/runnel/src/main/java/org/terracotta/runnel/utils/VLQ.java:24): Implements Variable-Length Quantity (VLQ) encoding to minimize payload sizes for integer values.

## How It Works

1. **Schema Definition**: A schema is defined statically once:
   ```java
   Struct struct = StructBuilder.newStructBuilder()
       .string("name", 10)
       .int64("id", 20)
       .struct("details", 30, detailsStruct)
       .build();
   ```
2. **Encoding**:
   ```java
   ByteBuffer buffer = struct.encoder()
       .string("name", "node1")
       .int64("id", 1001L)
       .encode();
   ```
3. **Decoding**:
   ```java
   StructDecoder<Void> decoder = struct.decoder(buffer);
   String name = decoder.string("name");
   Long id = decoder.int64("id");
   ```

## Interactions & Dependencies

- **Dependencies**: None (pure zero-dependency Java library).
- **Consumed By**:
  - `lease:common` for connection lease entity message serialization.
  - `healthchecker:common` for health check request/response wire payloads.
  - `communicator:common` for client-server communicator message frames.
  - `dynamic-config:entities:topology` for binary topology updates.
