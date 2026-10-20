# Security Suite (`:security`)

## Overview

The `security` module suite provides enterprise security logging and auditing infrastructure for Terracotta server nodes. It provides standardized audit logging for authentication, authorization, node join/leave, dynamic configuration modifications, and administrative operations.

The suite contains two submodules:
- **`logger:server:api`**: Defines the [`SecurityLogger`](security/logger/server/api/src/main/java/org/terracotta/security/logger/SecurityLogger.java:20) interface for emitting security audit events.
- **`logger:server:services`**: Implements the [`SecurityLoggerServiceProvider`](security/logger/server/services/src/main/java/org/terracotta/security/logger/SecurityLoggerServiceProvider.java:26) Voltron service provider and dynamic configuration extension.

## Interactions & Dependencies

- **Dependencies**: Terracotta Service APIs, `dynamic-config:server:api`.
- **Consumed By**: Security plugins, authentication providers, and administrative diagnostic endpoints across the Terracotta server runtime.
