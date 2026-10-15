# Dynamic Config CLI Suite (`:dynamic-config:cli`)

## Overview

The `dynamic-config:cli` module suite contains command-line administration, diagnostic, and migration tools for managing Terracotta 10.x+ dynamic clusters. The primary tool is `config-tool` (`config-tool.sh` / `config-tool.bat`), which provides remote cluster activation, node/stripe topology mutations, runtime property changes, export/import, configuration locking, and Nomad transaction repair. The suite also provides `upgrade-tool` for converting legacy `tc-config.xml` files into dynamic configuration formats.

The suite contains the following submodules:
- **`api`**: Core CLI command interfaces (`Command`), execution contexts, output formatting, and connection handling.
- **`jcommander`**: CLI option parsing, type converters, and parameter validators using JCommander.
- **`config-tool`**: Main administration CLI implementing all cluster lifecycle commands.
- **`upgrade-tool`**: Configuration migration utility converting legacy `tc-config.xml` files to dynamic config property files or Sanskrit repositories.
- **`upgrade-tool-oss`**: Open-source packaged distribution of the repository converter/upgrade tool.

---

## Commands in `config-tool`

| Command | Description |
|---|---|
| `activate` | Activates an unconfigured cluster from property files or activates individual nodes. |
| `attach` | Dynamically attaches a server node to an existing stripe or attaches a new stripe to the cluster. |
| `detach` | Dynamically detaches a node or an entire stripe from a running active cluster. |
| `get` | Queries runtime or upcoming configuration properties across nodes, stripes, or the whole cluster. |
| `set` | Mutates configuration properties dynamically across the cluster using Nomad 2PC. |
| `unset` | Resets or removes configuration properties back to defaults. |
| `export` | Exports the live cluster configuration into a `.properties` file or Sanskrit repository. |
| `import` | Imports configuration properties into a running cluster. |
| `lock-config` | Acquires an exclusive configuration lock on the cluster to prevent concurrent administrative changes. |
| `unlock-config` | Releases a previously acquired configuration lock. |
| `log` | Displays the historical Nomad 2PC change log across cluster nodes. |
| `repair` | Resolves in-doubt, split-brain, or diverged Nomad transactions by forcing commit or rollback. |
| `replica-failover`| Manages replica/backup failover priority settings. |
| `diagnostic` | Inspects low-level diagnostic states of unconfigured or running nodes. |

---

## The Upgrade / Migration Tool (`upgrade-tool`)

The `upgrade-tool` provides `ConfigConverterTool` to migrate legacy Terracotta XML configurations:
- Converts single or multi-stripe `tc-config.xml` files into modern `cluster.properties`.
- Directly converts `tc-config.xml` into a fully initialized Sanskrit on-disk configuration repository (`repository/`).

## How It Works

1. **Connection**: `config-tool` connects to target server nodes via diagnostic endpoints (`:diagnostic:client`) or Voltron Nomad entities (`:dynamic-config:entities:nomad:client`).
2. **Consensus Validation**: Before mutating configuration, it discovers node states, checks cluster consistency, and stages proposed changes.
3. **Execution**: Uses Nomad 2PC to ensure all nodes accept changes atomically.

## Interactions & Dependencies

- **Dependencies**: `dynamic-config:model`, `dynamic-config:api`, `dynamic-config:json`, `common:nomad`, `common:output-service`, `diagnostic:client`.
- **Consumed By**: Administrators, DevOps deployment scripts, and automated test runners (Angela, Galvan).
