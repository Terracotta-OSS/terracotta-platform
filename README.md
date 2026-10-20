# Terracotta Platform

Collections of libraries, Voltron entities, server services, and administrative CLI tools powering the Terracotta Server platform.

---

## Architectural Map & Module Directory

Every Gradle module has a dedicated `README.md` at its root explaining its purpose, internal architecture, core classes, and interactions with other modules.

```
terracotta-platform/
├── common/                              # Foundational low-level shared Java libraries
│   ├── inet/                            # Host/IP validation, socket and URI parsing
│   ├── json/                            # Modular Gson serialization framework
│   ├── nomad/                           # Two-Phase Commit (2PC) consensus and recovery engine
│   ├── output-service/                  # Formatted CLI/log message output abstraction
│   ├── runnel/                          # Zero-reflection binary schema serialization
│   ├── sanskrit/                        # Durable append-only storage with SHA-256 integrity
│   ├── structures/                      # Units of measure (Measure, MemoryUnit, TimeUnit), time budgets
│   └── test-utilities/                  # Shared JUnit rules, matchers, and process runners
│
├── voltron-proxy/                       # RPC and dynamic proxy framework for Voltron entities
├── communicator/                        # Bidirectional server-to-client message routing & ACKs
├── healthchecker/                       # Client-server heartbeat and connection watchdog
├── lease/                               # Client connection leasing & split-brain prevention
├── diagnostic/                          # Out-of-band server diagnostics, MBean RPC, and status queries
│
├── resources/                           # Server hardware resource managers
│   ├── data-root/                       # Named durable disk storage roots (data-dirs)
│   └── offheap/                         # Named direct offheap memory pools (offheap-resources)
│
├── security/                            # Server security and audit logging services
├── client-message-tracker/              # Once-and-Only-Once (OOO) message deduplication service
├── platform-base/                       # Server runtime metadata and node identification
├── voter/                               # Standalone quorum tie-breaker daemon for 2-node failover
│
├── management/                          # Distributed metrics, capabilities, and monitoring framework
│   ├── sequence-generator/              # Monotonically increasing sequence IDs
│   ├── model/                           # Capabilities, context containers, descriptors, statistics
│   ├── registry/                        # In-process registry for manageable Java objects
│   ├── server/                          # Server-side management services and event ring buffers
│   ├── entities/nms/                    # Centralized NMS management gateway entity
│   ├── entities/nms-agent/              # Distributed NMS agent entity
│   └── testing/                         # Management test harnesses, examples, and Angela ITs
│
├── dynamic-config/                      # Dynamic configuration and Nomad 2PC orchestration
│   ├── model/                           # Cluster, Stripe, Node, Setting immutable domain models
│   ├── api/                             # Dynamic config services, listeners, and change events
│   ├── json/                            # JSON serialization module for cluster models
│   ├── repository/                      # Sanskrit-backed on-disk configuration repository
│   ├── server/                          # Bootstrap provider, services, and change handlers
│   ├── entities/nomad/                  # Voltron entity exposing Nomad 2PC over RPC
│   ├── entities/topology/               # Voltron entity broadcasting cluster topology to clients
│   ├── entities/management/             # Server entity exposing configuration to NMS/TMS
│   ├── cli/                             # CLI administration suite (config-tool, upgrade-tool)
│   └── testing/                         # Dynamic config test harnesses and Angela ITs
│
├── platform-layout/                     # Kit layout packaging and distribution assembly
└── docs/                                # Technical specifications and AsciiDoctor documentation
```

---

## Detailed Module Documentation

### Common Libraries
- [`:common`](common/README.md)
  - [`:common:inet`](common/inet/README.md)
  - [`:common:json`](common/json/README.md)
  - [`:common:nomad`](common/nomad/README.md)
  - [`:common:output-service`](common/output-service/README.md)
  - [`:common:runnel`](common/runnel/README.md)
  - [`:common:sanskrit`](common/sanskrit/README.md)
  - [`:common:structures`](common/structures/README.md)
  - [`:common:test-utilities`](common/test-utilities/README.md)

### Core Voltron Services & Entities
- [`:voltron-proxy`](voltron-proxy/README.md)
- [`:communicator`](communicator/README.md)
- [`:healthchecker`](healthchecker/README.md)
- [`:lease`](lease/README.md)
- [`:diagnostic`](diagnostic/README.md)
- [`:resources`](resources/README.md)
  - [`:resources:data-root`](resources/data-root/README.md)
  - [`:resources:offheap`](resources/offheap/README.md)
- [`:security`](security/README.md)
- [`:client-message-tracker`](client-message-tracker/README.md)
- [`:platform-base`](platform-base/README.md)
- [`:voter`](voter/README.md)

### Management Framework
- [`:management`](management/README.md)
  - [`:management:sequence-generator`](management/sequence-generator/README.md)
  - [`:management:model`](management/model/README.md)
  - [`:management:registry`](management/registry/README.md)
  - [`:management:server`](management/server/README.md)
  - [`:management:entities:nms`](management/entities/nms/README.md)
  - [`:management:entities:nms-agent`](management/entities/nms-agent/README.md)
  - [`:management:testing`](management/testing/README.md)

### Dynamic Configuration Framework
- [`:dynamic-config`](dynamic-config/README.md)
  - [`:dynamic-config:model`](dynamic-config/model/README.md)
  - [`:dynamic-config:api`](dynamic-config/api/README.md)
  - [`:dynamic-config:json`](dynamic-config/json/README.md)
  - [`:dynamic-config:repository`](dynamic-config/repository/README.md)
  - [`:dynamic-config:server`](dynamic-config/server/README.md)
  - [`:dynamic-config:entities:nomad`](dynamic-config/entities/nomad/README.md)
  - [`:dynamic-config:entities:topology`](dynamic-config/entities/topology/README.md)
  - [`:dynamic-config:entities:management`](dynamic-config/entities/management/README.md)
  - [`:dynamic-config:cli`](dynamic-config/cli/README.md)
  - [`:dynamic-config:testing`](dynamic-config/testing/README.md)

### Packaging & Documentation
- [`:platform-layout`](platform-layout/README.md)
- [`:docs`](docs/README.md)
