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
package org.terracotta.dynamic_config.xml.oss;

import org.terracotta.config.Server;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.cli.config_convertor.conversion.AbstractTcConfigMapper;
import org.terracotta.dynamic_config.cli.config_convertor.xml.CommonMapper;
import org.terracotta.dynamic_config.cli.config_convertor.xml.NonSubstitutingTCConfigurationParser;
import org.terracotta.dynamic_config.cli.config_convertor.xml.TcConfigMapper;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Mathieu Carbou
 */
public class OssTcConfigMapper extends AbstractTcConfigMapper implements TcConfigMapper {

  private CommonMapper commonMapper;

  @Override
  public void init(ClassLoader classLoader) {
    super.init(classLoader);
    commonMapper = new CommonMapper(classLoader);
  }

  @Override
  public Stripe getStripe(String xml) {
    try {
      TcConfiguration tcConfiguration = NonSubstitutingTCConfigurationParser.parse(xml, classLoader);
      TcConfig tcConfig = tcConfiguration.getPlatformConfiguration();
      NonSubstitutingTCConfigurationParser.applyPlatformDefaults(tcConfig);
      Map<Class<?>, List<Object>> xmlPlugins = commonMapper.parsePlugins(xml, tcConfig);
      List<Server> servers = tcConfig.getServers().getServer();
      List<org.terracotta.dynamic_config.api.model.Node> nodes = new ArrayList<>();
      servers.forEach(server -> nodes.add(
          org.terracotta.dynamic_config.api.model.Node.empty()
              .setNodeName(server.getName())
              .setNodeHostname(server.getHost())
              .setNodePort(server.getTsaPort().getValue())
              .setNodeBindAddress(commonMapper.moreRestrictive(server.getTsaPort().getBind(), server.getBind()))
              .setNodeGroupPort(server.getTsaGroupPort().getValue())
              .setNodeGroupBindAddress(commonMapper.moreRestrictive(server.getTsaGroupPort().getBind(), server.getBind()))
              .setNodeLogDir(Paths.get(server.getLogs()))
              .setFailoverPriority(commonMapper.toFailoverPriority(tcConfig.getFailoverPriority()))
              .setClientReconnectWindow(tcConfig.getServers().getClientReconnectWindow(), SECONDS)
              .setTcProperties(commonMapper.toProperties(tcConfig))
              // plugins
              .setNodeMetadataDir(null)
              .setDataDirs(commonMapper.toUserDataDirs(xmlPlugins))
              .setOffheapResources(commonMapper.toOffheapResources(xmlPlugins))
              .setNodeBackupDir(null)
              .setClientLeaseDuration(commonMapper.toClientLeaseDuration(xmlPlugins))
      ));
      return new Stripe(nodes);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (SAXException e) {
      throw new IllegalStateException("Invalid config repository XML file: " + e.getMessage(), e);
    }
  }

}
