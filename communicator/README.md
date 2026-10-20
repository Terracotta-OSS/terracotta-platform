# Communicator (`:communicator`)

## Overview

The `communicator` module provides a reliable bidirectional communication layer between Terracotta Voltron server entities and connected client endpoints. It allows server entities to push messages, notifications, or commands to specific clients or groups of clients with delivery tracking, acknowledgements (ACKs), and retries.

The module is divided into three parts:
- **`common`**: Message protocol, Runnel binary serialization codec, and request types.
- **`client`**: Client-side receiver manager dispatching server messages to registered client handlers.
- **`server`**: Server-side manager allowing active server entities to send messages to connected `ClientDescriptor`s.

---

## Submodules & Architecture

```
+------------------------------------+
|        Server-Side Entity          |
+-----------------+------------------+
                  | sends notification / message
                  v
+------------------------------------+
|       :communicator:server         |
|  ClientCommunicatorServerManager   |
+-----------------+------------------+
                  | (Runnel-encoded message over entity endpoint)
                  v
+------------------------------------+
|       :communicator:client         |
|  ClientCommunicatorClientManager   |
+-----------------+------------------+
                  | invokes handler
                  v
+------------------------------------+
|      Registered Client Handler     |
+------------------------------------+
```

### 1. `:communicator:common`

- [`ClientCommunicatorRequest`](communicator/common/src/main/java/org/terracotta/clientcommunicator/support/ClientCommunicatorRequest.java:23): Envelope containing message payload, request ID, sequence numbers, and request type.
- [`ClientCommunicatorRequestCodec`](communicator/common/src/main/java/org/terracotta/clientcommunicator/support/ClientCommunicatorRequestCodec.java:27): High-speed binary serializer powered by `common:runnel`.
- [`ClientCommunicatorRequestType`](communicator/common/src/main/java/org/terracotta/clientcommunicator/support/ClientCommunicatorRequestType.java:18): Enumerates message categories (e.g. data push, ACK).

### 2. `:communicator:client`

- [`ClientCommunicatorClientManager`](communicator/client/src/main/java/org/terracotta/clientcommunicator/support/ClientCommunicatorClientManager.java:24): Registry interface where client components register handlers for server-initiated events.
- [`ClientCommunicatorClientManagerImpl`](communicator/client/src/main/java/org/terracotta/clientcommunicator/support/ClientCommunicatorClientManagerImpl.java:26): Unpacks binary frames and dispatches them to matching [`ClientCommunicatorMessageHandler`](communicator/client/src/main/java/org/terracotta/clientcommunicator/support/ClientCommunicatorMessageHandler.java:23) instances.

### 3. `:communicator:server`

- [`ClientCommunicatorServerManager`](communicator/server/src/main/java/org/terracotta/clientcommunicator/support/ClientCommunicatorServerManager.java:25): Server-side API allowing entities to target specific `ClientDescriptor` instances with asynchronous or synchronous calls.
- [`ClientCommunicatorServerManagerImpl`](communicator/server/src/main/java/org/terracotta/clientcommunicator/support/ClientCommunicatorServerManagerImpl.java:31): Manages client routing, message sequencing, timeouts, and delivery confirmations.

## Interactions & Dependencies

- **Dependencies**: `common:runnel` for zero-reflection binary serialization, Terracotta Entity APIs (`org.terracotta:entity-api`).
- **Consumed By**:
  - `management:server:services` and `management:entities:nms-agent` to stream management notifications and statistics from server to client or vice versa.
  - Clustered services requiring out-of-band server-to-client notifications.
