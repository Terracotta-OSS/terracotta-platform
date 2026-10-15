# Dynamic Config Repository (`:dynamic-config:repository`)

## Overview

The `dynamic-config-repository` module manages the on-disk directory layout and persistence of Terracotta configuration repositories (`config-dir` / `repository`). It uses `common:sanskrit` to persist active and upcoming cluster topologies, Nomad 2PC transaction logs, and node metadata.

## Key Components

- `NomadRepositoryManager`: High-level manager orchestrating reading, creating, and initializing the server's configuration directory structure.
- `RepoLayout`: Defines the directory hierarchy for Sanskrit logs, version metadata, and backup snapshots.
- `ConfigRepositoryUpgrader`: Handles schema migrations and repository format upgrades across Terracotta minor and major versions.

## Interactions & Dependencies

- **Dependencies**: `dynamic-config:model`, `dynamic-config:json`, `common:sanskrit`, `common:nomad`.
- **Consumed By**: `dynamic-config:server:config-provider`, `dynamic-config:server:services`, and `dynamic-config:cli:upgrade-tool`.
