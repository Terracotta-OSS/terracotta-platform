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
package org.terracotta.config.data_roots;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigExtension;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.entity.PlatformConfiguration;

import java.nio.file.Path;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * @author Mathieu Carbou
 */
public class DataRootsDynamicConfigExtension implements DynamicConfigExtension {
  @Override
  public void configure(Registrar registrar, PlatformConfiguration platformConfiguration) {
    IParameterSubstitutor parameterSubstitutor = findService(platformConfiguration, IParameterSubstitutor.class);
    PathResolver pathResolver = findService(platformConfiguration, PathResolver.class);
    TopologyService topologyService = findService(platformConfiguration, TopologyService.class);
    ConfigChangeHandlerManager configChangeHandlerManager = findService(platformConfiguration, ConfigChangeHandlerManager.class);

    NodeContext nodeContext = topologyService.getRuntimeNodeContext();
    Path nodeMetadataDir = nodeContext.getNode().getMetadataDir().orDefault().toPath();
    Map<String, Path> dataDirs = nodeContext.getNode().getDataDirs().orDefault().entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().toPath()));
    DataDirectoriesConfigImpl dataDirectoriesConfig = new DataDirectoriesConfigImpl(parameterSubstitutor, pathResolver, nodeMetadataDir, dataDirs);
    configChangeHandlerManager.set(Setting.DATA_DIRS, new DataDirectoryConfigChangeHandler(dataDirectoriesConfig, parameterSubstitutor, pathResolver));

    registrar.registerExtendedConfiguration(dataDirectoriesConfig);
  }
}
