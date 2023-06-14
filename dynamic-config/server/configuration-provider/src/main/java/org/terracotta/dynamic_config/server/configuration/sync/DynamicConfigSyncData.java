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
package org.terracotta.dynamic_config.server.configuration.sync;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.json.Json;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigSyncData {

  private final List<NomadChangeInfo> nomadChanges;
  private final String license;
  private final Cluster cluster;

  // For Json
  private DynamicConfigSyncData() {
    nomadChanges = null;
    license = null;
    cluster = null;
  }

  public DynamicConfigSyncData(List<NomadChangeInfo> nomadChanges, Cluster cluster, String license) {
    this.nomadChanges = nomadChanges;
    this.cluster = cluster;
    this.license = license;
  }

  public List<NomadChangeInfo> getNomadChanges() {
    return nomadChanges;
  }

  public String getLicense() {
    return license;
  }

  public Cluster getCluster() {
    return cluster;
  }

  public static class Codec {
    private final Json json;

    public Codec(Json.Factory jsonFactory) {
      this.json = jsonFactory.create();
    }

    public byte[] encode(DynamicConfigSyncData o) {
      return json.toString(o).getBytes(UTF_8);
    }

    public DynamicConfigSyncData decode(byte[] bytes) {
      return json.parse(new String(bytes, UTF_8), DynamicConfigSyncData.class);
    }
  }
}
