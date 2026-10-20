# Terracotta Voter (`:voter`)

## Overview

The `voter` module provides an external, lightweight quorum tie-breaker daemon for Terracotta active-passive clusters configured with consistency mode (`failover-priority=consistency:N`). In a two-node (1 active + 1 passive) stripe, if the network between nodes breaks, neither node can achieve a majority on its own. The Terracotta Voter acts as an independent 3rd voting entity, allowing the surviving active (or reachable passive) to obtain quorum and avoid split-brain while preventing simultaneous promotion.

## Key Components

- [`TCVoter`](voter/src/main/java/org/terracotta/voter/TCVoter.java:23) / [`TCVoterImpl`](voter/src/main/java/org/terracotta/voter/TCVoterImpl.java:31): Main voter engine managing heartbeats, cluster topology tracking, and vote casting.
- [`ClientVoterManager`](voter/src/main/java/org/terracotta/voter/ClientVoterManager.java:22) / [`ClientVoterManagerImpl`](voter/src/main/java/org/terracotta/voter/ClientVoterManagerImpl.java:26): Handles communication and voting protocol with an individual Terracotta server node.
- [`ClientVoterThread`](voter/src/main/java/org/terracotta/voter/ClientVoterThread.java:27): Background thread polling server health and processing vote requests.
- [`VotingGroup`](voter/src/main/java/org/terracotta/voter/VotingGroup.java:27): Represents a configured group of voting nodes and evaluates quorum conditions.
- [`TCVoterMain`](voter/src/main/java/org/terracotta/voter/cli/TCVoterMain.java:34): CLI executable launching the voter process (`start-tc-voter.sh`).

## How It Works

1. **Registration**: The voter starts as a standalone Java process and registers itself with the servers listed in the CLI arguments (e.g. `start-tc-voter.sh -s server1:9410,server2:9410`).
2. **Heartbeats**: The voter continuously monitors the health of all nodes via periodic pings.
3. **Failover Voting**: When an active server fails or network partitioning occurs, the passive server requests a vote from the voter. If the voter agrees that the previous active is unreachable and the passive is healthy, it casts an affirmative vote, enabling the passive to safely promote to active without violating consistency.

## Interactions & Dependencies

- **Dependencies**: `common:inet`, `diagnostic:client`, `diagnostic:model`, JCommander for CLI argument parsing.
- **Interacts With**: Terracotta Server diagnostic and failover election services.
