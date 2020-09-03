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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.command;

import com.tc.util.ManagedServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.cli.command.LocalMainCommand;
import org.terracotta.dynamic_config.cli.command.ServiceProvider;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Mathieu Carbou
 */
public class UpgradeToolServiceProvider implements ServiceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeToolServiceProvider.class);

  @Override
  public Collection<Object> createServices(LocalMainCommand mainCommand) {
    return Collections.singletonList(createClusterValidator(mainCommand));
  }

  protected ClusterValidator createClusterValidator(LocalMainCommand mainCommand) {
    Collection<ClusterValidator> services = ManagedServiceLoader.loadServices(ClusterValidator.class, getClass().getClassLoader());
    if (services.size() != 1) {
      throw new AssertionError("expected exactly one service provider, but found :" + services.size());
    }
    final ClusterValidator service = services.iterator().next();
    LOGGER.trace("Discovered implementation: {} for service: {}", service, ClusterValidator.class.getName());
    return service;
  }
}
