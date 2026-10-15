# Dynamic Config JSON (`:dynamic-config:json`)

## Overview

The `dynamic-config-json` module provides JSON serialization and deserialization bindings for dynamic configuration models, cluster topologies, Nomad 2PC messages, and configuration change records.

## Key Components

- `DynamicConfigJsonModule`: `common:json` module registering custom type adapters, sub-type mappings, and serializers for `Cluster`, `Stripe`, `Node`, `Setting`, `NomadChange`, and `LogicalServerState`.

## How It Works

- Extends Google Gson with Terracotta polymorphism and model mappings, ensuring exact JSON schema compliance when serializing Nomad transaction logs and diagnostic REST responses.

## Interactions & Dependencies

- **Dependencies**: `dynamic-config:model`, `common:json`, `common:nomad`.
- **Consumed By**: `dynamic-config:repository`, `dynamic-config:server:services`, and `dynamic-config:cli:api`.
