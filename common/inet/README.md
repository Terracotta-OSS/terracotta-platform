# Common Inet (`:common:inet`)

## Overview

The `terracotta-inet` module provides essential networking utilities for network address parsing, validation, and URI manipulation across Terracotta Platform components and CLI tools.

## Key Components

- [`HostPort`](common/inet/src/main/java/org/terracotta/inet/HostPort.java:23): Represents and parses a host-and-port pair, handling both IPv4, IPv6 (including bracketed syntax `[::1]:9410`), and hostname formats.
- [`HostAndIpValidator`](common/inet/src/main/java/org/terracotta/inet/HostAndIpValidator.java:27): Validates hostnames, IPv4 addresses, and IPv6 addresses according to RFC specifications.
- [`InetSocketAddressConverter`](common/inet/src/main/java/org/terracotta/inet/InetSocketAddressConverter.java:24): Converts string representations and `HostPort` instances into `InetSocketAddress` objects with DNS resolution support.
- [`UriUtils`](common/inet/src/main/java/org/terracotta/inet/UriUtils.java:24): Utilities to construct, parse, and normalize connection URIs (e.g., `terracotta://` URIs).

## How It Works

1. **Address Parsing**: `HostPort.parse("host:port", defaultPort)` safely extracts host and port, handling IPv6 bracketed notations without ambiguity.
2. **Address Validation**: `HostAndIpValidator.isValidHost()` checks syntax before attempting DNS lookups to fail fast on invalid CLI arguments.
3. **Socket Conversion**: `InetSocketAddressConverter` bridges string-based configurations to standard Java networking APIs (`InetSocketAddress`).

## Interactions & Dependencies

- **Dependencies**: Standard Java SE networking libraries (`java.net`).
- **Consumed By**:
  - `dynamic-config:model` and `dynamic-config:cli` for cluster node endpoint configuration.
  - `diagnostic:client` for connecting to server diagnostic ports.
  - `voter` CLI for connecting to cluster nodes.
