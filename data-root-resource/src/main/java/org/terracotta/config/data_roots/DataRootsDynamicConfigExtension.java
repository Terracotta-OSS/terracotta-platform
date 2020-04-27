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

/**
 * @author Mathieu Carbou
 */
public class DataRootsDynamicConfigExtension implements DynamicConfigExtension {
  @Override
  public void configure(Registrar registrar, PlatformConfiguration platformConfiguration) {
    IParameterSubstitutor parameterSubstitutor = platformConfiguration.getExtendedConfiguration(IParameterSubstitutor.class).iterator().next();
    PathResolver pathResolver = platformConfiguration.getExtendedConfiguration(PathResolver.class).iterator().next();
    TopologyService topologyService = platformConfiguration.getExtendedConfiguration(TopologyService.class).iterator().next();
    ConfigChangeHandlerManager configChangeHandlerManager = platformConfiguration.getExtendedConfiguration(ConfigChangeHandlerManager.class).iterator().next();

    NodeContext nodeContext = topologyService.getRuntimeNodeContext();

    Path nodeMetadataDir = nodeContext.getNode().getNodeMetadataDir();
    Map<String, Path> dataDirs = nodeContext.getNode().getDataDirs();
    DataDirectoriesConfigImpl dataDirectoriesConfig = new DataDirectoriesConfigImpl(parameterSubstitutor, pathResolver, nodeMetadataDir, dataDirs);

    configChangeHandlerManager.set(Setting.DATA_DIRS, new DataDirectoryConfigChangeHandler(dataDirectoriesConfig, parameterSubstitutor, pathResolver));

    registrar.registerExtendedConfiguration(dataDirectoriesConfig);
  }
}
