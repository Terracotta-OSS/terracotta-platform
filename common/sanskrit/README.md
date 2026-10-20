# Common Sanskrit (`:common:sanskrit`)

## Overview

`sanskrit` is a durable, append-only, checksum-verified key-value persistence store developed for Terracotta. It serves as the durable backing store for configuration state, Nomad transaction logs, and metadata on disk, guaranteeing atomic writes, corruption detection via SHA-256 hashes, file locking, and crash consistency.

## Storage Architecture & Mechanics

```
sanskrit-directory/
├── append.log      # Append-only journal containing serialized JSON change lines
├── hash0           # SHA-256 rolling hash file (ping)
├── hash1           # SHA-256 rolling hash file (pong)
└── lock            # Directory lock ensuring single-process ownership
```

1. **Dual-Hash Ping-Pong Mechanism**: To prevent corruption if a power loss occurs during a hash write, Sanskrit maintains two rolling hash files (`hash0` and `hash1`). When updating the rolling hash of `append.log`, Sanskrit alternately writes to `hash0` and `hash1`. [`HashChecker`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/HashChecker.java:25) compares the hashes on startup.
2. **Crash Recovery & Journal Replay**: On startup, [`SanskritImpl`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/SanskritImpl.java:48) reads `append.log` line by line using [`MarkableLineParser`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/MarkableLineParser.java:27) and validates the running SHA-256 hash. If an uncompleted line exists at the end of the file due to an unclean shutdown, it is safely truncated.
3. **Atomic Changes**: State mutations are packaged into [`SanskritChange`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/change/SanskritChange.java:22) records via [`SanskritChangeBuilder`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/change/SanskritChangeBuilder.java:26), allowing multi-key insertions, updates, and deletions to commit atomically in a single write.

## Key Components

- [`Sanskrit`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/Sanskrit.java:23): Primary interface for persistent key-value access (`applyChange`, `getString`, `getLong`, `getObject`).
- [`SanskritImpl`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/SanskritImpl.java:48): Main engine managing in-memory state snapshots and appending changes to disk.
- [`FilesystemDirectory`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/FilesystemDirectory.java:27) / [`FileBasedFilesystemDirectory`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/file/FileBasedFilesystemDirectory.java:32): File channel abstraction for underlying file system operations.
- [`FileLockManager`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/file/lock/FileLockManager.java:27): Cross-process file locking ensuring mutual exclusion on repository directories.
- [`SanskritMapper`](common/sanskrit/src/main/java/org/terracotta/persistence/sanskrit/SanskritMapper.java:20): JSON object-to-string mapper integrating with `common:json`.

## Interactions & Dependencies

- **Dependencies**: `common:json` for JSON serialization of objects and metadata.
- **Consumed By**:
  - `dynamic-config:repository` for reading and writing node configuration files and state repositories.
  - `dynamic-config:server:services` for persisting Nomad 2PC transactions and active topology models on Terracotta servers.
