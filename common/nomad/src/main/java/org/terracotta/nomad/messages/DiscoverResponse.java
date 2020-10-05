/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.nomad.messages;

import org.terracotta.nomad.server.NomadServerMode;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

public class DiscoverResponse<T> {
  private final NomadServerMode mode;
  private final long mutativeMessageCount;
  private final String lastMutationHost;
  private final String lastMutationUser;
  private final Instant lastMutationTimestamp;
  private final long currentVersion;
  private final long highestVersion;
  private final ChangeDetails<T> latestChange;

  public DiscoverResponse(NomadServerMode mode,
                          long mutativeMessageCount,
                          String lastMutationHost,
                          String lastMutationUser,
                          Instant lastMutationTimestamp,
                          long currentVersion,
                          long highestVersion,
                          ChangeDetails<T> latestChange) {
    this.mode = requireNonNull(mode);
    this.mutativeMessageCount = mutativeMessageCount;
    this.lastMutationHost = lastMutationHost;
    this.lastMutationUser = lastMutationUser;
    this.lastMutationTimestamp = lastMutationTimestamp;
    this.currentVersion = currentVersion;
    this.highestVersion = highestVersion;
    this.latestChange = latestChange;
  }

  public NomadServerMode getMode() {
    return mode;
  }

  public long getMutativeMessageCount() {
    return mutativeMessageCount;
  }

  public String getLastMutationHost() {
    return lastMutationHost;
  }

  public String getLastMutationUser() {
    return lastMutationUser;
  }

  public Instant getLastMutationTimestamp() {
    return lastMutationTimestamp;
  }

  public long getCurrentVersion() {
    return currentVersion;
  }

  public long getHighestVersion() {
    return highestVersion;
  }

  public ChangeDetails<T> getLatestChange() {
    return latestChange;
  }
}
