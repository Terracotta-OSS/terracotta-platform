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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.server.NomadChangeInfo;

import java.io.UncheckedIOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigSyncData {

  private final List<NomadChangeInfo> nomadChanges;
  private final String license;
  private final Cluster cluster;

  @JsonCreator
  public DynamicConfigSyncData(@JsonProperty(value = "nomadChanges", required = true) List<NomadChangeInfo> nomadChanges,
                               @JsonProperty(value = "cluster", required = true) Cluster cluster,
                               @JsonProperty(value = "license") String license) {
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
    private final ObjectMapper objectMapper;

    public Codec(ObjectMapperFactory objectMapperFactory) {
      this.objectMapper = objectMapperFactory.create();
    }

    public byte[] encode(DynamicConfigSyncData o) {
      try {
        return objectMapper.writeValueAsString(o).getBytes(UTF_8);
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    }

    public DynamicConfigSyncData decode(byte[] bytes) {
      try {
        return objectMapper.readValue(new String(bytes, UTF_8), DynamicConfigSyncData.class);
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
