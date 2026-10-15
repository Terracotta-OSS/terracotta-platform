# Platform Layout (`:platform-layout`)

## Overview

The `platform-layout` module defines the final packaging, distribution structure, and classpath assembly for the Terracotta Platform kit. It bundles all server plugins, service APIs, entities, runtime libraries, and CLI tools into a consistent directory hierarchy ready for deployment.

## Kit Structure

```
terracotta-platform-kit/
├── server/
│   ├── plugins/
│   │   ├── api/       # Shared server plugin API JARs (visible to all plugins)
│   │   └── lib/       # Server plugin implementations and Voltron entities
│   └── lib/           # Core Terracotta server runtime libraries
└── tools/
    ├── config-tool/   # Dynamic configuration CLI (config-tool.sh / config-tool.bat)
    └── voter/         # Split-brain quorum voter CLI (start-tc-voter.sh / start-tc-voter.bat)
```

## How It Works

1. **Plugin API Assembly (`server/plugins/api`)**: Collects shared API libraries (`:common:json`, `:common:nomad`, `:diagnostic:server:api`, `:dynamic-config:server:api`, `:management:server:api`, `:security:logger:server:api`).
2. **Plugin Implementation Assembly (`server/plugins/lib`)**: Packages services, resources (`data-root`, `offheap`), entities (`nomad`, `topology`, `management`, `nms`, `nms-agent`), and server listeners with strict dependency conflict resolution.
3. **Tools Packaging (`tools/`)**: Includes executable tools (`:dynamic-config:cli:config-tool`, `:voter`).
4. **Archive Generation**: Produces `.tar.gz` and `.zip` distribution packages and exploded kit directories for integration testing.

## Interactions & Dependencies

- **Dependencies**: Pulls together all server APIs, service providers, entities, and CLI tools across `terracotta-platform`.
- **Consumed By**: `:kit` for test execution, integration test suites (Angela, Galvan), and release deployment artifacts.
