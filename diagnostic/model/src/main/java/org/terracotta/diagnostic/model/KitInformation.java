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
package org.terracotta.diagnostic.model;

import java.time.Instant;

/**
 * @author Mathieu Carbou
 */
public class KitInformation {

  private final String version;
  private final String revision;
  private final String branch;
  private final Instant timestamp;

  public KitInformation(String version, String revision, String branch, Instant timestamp) {
    this.version = version;
    this.revision = revision;
    this.branch = branch;
    this.timestamp = timestamp;
  }

  public String getVersion() {
    return version;
  }

  public String getRevision() {
    return revision;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getBranch() {
    return branch;
  }

  @Override
  public String toString() {
    return "KitInformation{" +
        "version='" + version + '\'' +
        ", revision='" + revision + '\'' +
        ", branch='" + branch + '\'' +
        ", timestamp=" + timestamp +
        '}';
  }
}
