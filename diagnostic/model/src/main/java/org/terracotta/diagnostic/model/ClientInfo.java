/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

public class ClientInfo {

  private final String id;
  private final String name;
  private final String version;
  private final String revision;
  private final String ipAddress;

  public ClientInfo(String[] params) {
    this(params[0], params[1], params[2], params[3], params[4]);
  }

  public ClientInfo(String id, String name, String version, String revision, String ipAddress) {
    this.id = id;
    this.name = name;
    this.version = version;
    this.revision = revision;
    this.ipAddress = ipAddress;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getRevision() {
    return revision;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public String getFormattedVersion() {
    return version + "/" + (revision.isEmpty() ? "revision not available" : revision);
  }
}