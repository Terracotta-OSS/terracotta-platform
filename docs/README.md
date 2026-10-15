# Platform Documentation (`:docs`)

## Overview

The `docs` module compiles and generates AsciiDoctor technical specifications and architectural documentation for the Terracotta Platform, focusing on dynamic configuration and distributed transaction protocols.

## Documentation Topics

- **Config Tool Specifications**: CLI syntax, commands, and parameter design.
- **Config Repository Structure**: On-disk Sanskrit storage layouts and upgrade specifications.
- **Distributed Transaction Protocol**: TLA+ formal specification and design of the Nomad consensus protocol.
- **Sync & Repair**: Active-to-passive synchronization and split-brain recovery procedures.
- **Consistency**: OSS vs EE configuration consistency models.

## Interactions & Dependencies

- **Build Tooling**: Asciidoctor JVM plugins (`org.asciidoctor.jvm.convert`, `org.asciidoctor.jvm.gems`).
