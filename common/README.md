# Terracotta Platform Common Suite (`:common`)

## Overview

The `common` directory hosts foundational, low-level Java libraries shared across all client, server, entity, and CLI modules in the Terracotta Platform.

## Submodules

- [`:common:inet`](common/inet/README.md): Host, IP address, socket, and URI validation and conversion utilities.
- [`:common:json`](common/json/README.md): Modular Gson serialization framework supporting custom type adapters, polymorphism, and Java 8 Time (JSR-310).
- [`:common:nomad`](common/nomad/README.md): Consensus and recovery Two-Phase Commit (2PC) engine powering dynamic configuration changes.
- [`:common:output-service`](common/output-service/README.md): Output abstraction decoupling message reporting from console/file/logging channels.
- [`:common:runnel`](common/runnel/README.md): Zero-reflection, high-speed binary schema serialization protocol for entity messaging and wire formats.
- [`:common:sanskrit`](common/sanskrit/README.md): Crash-consistent, append-only persistence log with cryptographic SHA-256 integrity checks.
- [`:common:structures`](common/structures/README.md): Strongly typed units of measure (`MemoryUnit`, `TimeUnit`), time budgets, tuples, and semantic versioning.
- [`:common:test-utilities`](common/test-utilities/README.md): Common JUnit test rules, process runners, and matchers.
