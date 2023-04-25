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
package org.terracotta.dynamic_config.server.configuration.sync.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.dynamic_config.server.configuration.sync.DynamicConfigSyncData;
import org.terracotta.json.Json;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @author Mathieu Carbou
 */
public class SyncJsonModule extends SimpleModule implements Json.Module {
  private static final long serialVersionUID = 1L;

  public SyncJsonModule() {
    super(SyncJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));
    setMixInAnnotation(DynamicConfigSyncData.class, DynamicConfigSyncDataMixin.class);
  }

  @Override
  public Iterable<? extends Module> getDependencies() {
    return singletonList(new DynamicConfigApiJsonModule());
  }

  public static class DynamicConfigSyncDataMixin extends DynamicConfigSyncData {
    @JsonCreator
    protected DynamicConfigSyncDataMixin(@JsonProperty(value = "nomadChanges", required = true) List<NomadChangeInfo> nomadChanges,
                                         @JsonProperty(value = "cluster", required = true) Cluster cluster,
                                         @JsonProperty(value = "license") String license) {
      super(nomadChanges, cluster, license);
    }
  }

}
