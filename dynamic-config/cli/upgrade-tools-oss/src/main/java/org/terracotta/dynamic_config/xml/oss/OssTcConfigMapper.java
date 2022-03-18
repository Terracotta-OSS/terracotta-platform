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
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.conversion.AbstractTcConfigMapper;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.CommonMapper;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.NonSubstitutingTCConfigurationParser;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.TcConfigMapper;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
  public Cluster getStripe(String xml) {
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
              .setTcProperties(commonMapper.toProperties(tcConfig))
              .setNodeMetadataDir(null)
              .setDataDirs(commonMapper.toDataDirs(xmlPlugins, dataRootMapping -> true))
              .setNodeBackupDir(null)
      ));
      return Cluster.newCluster(new Stripe(nodes))
          .setClientLeaseDuration(commonMapper.toClientLeaseDuration(xmlPlugins))
          .setOffheapResources(commonMapper.toOffheapResources(xmlPlugins))
          .setClientReconnectWindow(commonMapper.toClientReconnectWindow(tcConfig))
          .setFailoverPriority(commonMapper.toFailoverPriority(tcConfig.getFailoverPriority()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (SAXException e) {
      throw new IllegalStateException("Invalid tc-config XML input: " + e.getMessage(), e);
    }
  }

}
