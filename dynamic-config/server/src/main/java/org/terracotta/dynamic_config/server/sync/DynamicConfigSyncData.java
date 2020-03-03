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
package org.terracotta.dynamic_config.server.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import org.terracotta.nomad.server.NomadChangeInfo;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.terracotta.json.Json.parse;
import static org.terracotta.json.Json.toJson;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigSyncData {

  private final List<NomadChangeInfo> nomadChanges;
  private final String license;

  @JsonCreator
  public DynamicConfigSyncData(@JsonProperty(value = "nomadChanges", required = true) List<NomadChangeInfo> nomadChanges,
                               @JsonProperty(value = "license") String license) {
    this.nomadChanges = nomadChanges;
    this.license = license;
  }

  public List<NomadChangeInfo> getNomadChanges() {
    return nomadChanges;
  }

  public String getLicense() {
    return license;
  }

  public static DynamicConfigSyncData decode(byte[] bytes) {
    return parse(new String(bytes, UTF_8), new TypeReference<DynamicConfigSyncData>() {});
  }

  public byte[] encode() {
    return toJson(this).getBytes(UTF_8);
  }
}
