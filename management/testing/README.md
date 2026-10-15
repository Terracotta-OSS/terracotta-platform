# Management Testing Suite (`:management:testing`)

## Overview

The `management:testing` suite provides mock entities, example manageable applications, and end-to-end integration test suites for validating the Terracotta management subsystem.

## Submodules

- **`entity`**: Test entity definitions and mock Voltron entities used to verify management provider bindings and event flows.
- **`examples`**: Sample applications demonstrating how to instrument clustered applications, expose JMX/management capabilities, and consume statistics.
- **`integration-tests`**: Angela-based integration tests verifying NMS hub routing, NMS agent connectivity, notification broadcasting, and failover survivability.
