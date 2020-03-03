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
import org.terracotta.data.config.DataRootMapping;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.TcConfigMapper;
import org.terracotta.dynamic_config.xml.MapperUtils;
import org.terracotta.dynamic_config.xml.NonSubstitutingTCConfigurationParser;
import org.terracotta.dynamic_config.xml.conversion.AbstractTcConfigMapper;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Mathieu Carbou
 */
public class OssTcConfigMapper extends AbstractTcConfigMapper implements TcConfigMapper {

  @Override
  public Stripe getStripe(String xml) {
    try {
      TcConfiguration tcConfiguration = NonSubstitutingTCConfigurationParser.parse(xml);
      TcConfig tcConfig = tcConfiguration.getPlatformConfiguration();
      NonSubstitutingTCConfigurationParser.applyPlatformDefaults(tcConfig);
      Map<Class<?>, List<Object>> xmlPlugins = MapperUtils.parsePlugins(xml, tcConfig);
      List<Server> servers = tcConfig.getServers().getServer();
      List<org.terracotta.dynamic_config.api.model.Node> nodes = new ArrayList<>();
      servers.forEach(server -> nodes.add(
          org.terracotta.dynamic_config.api.model.Node.empty()
              .setNodeName(server.getName())
              .setNodeHostname(server.getHost())
              .setNodePort(server.getTsaPort().getValue())
              .setNodeBindAddress(MapperUtils.moreRestrictive(server.getTsaPort().getBind(), server.getBind()))
              .setNodeGroupPort(server.getTsaGroupPort().getValue())
              .setNodeGroupBindAddress(MapperUtils.moreRestrictive(server.getTsaGroupPort().getBind(), server.getBind()))
              .setNodeLogDir(Paths.get(server.getLogs()))
              .setFailoverPriority(toFailoverPriority(tcConfig.getFailoverPriority()))
              .setClientReconnectWindow(tcConfig.getServers().getClientReconnectWindow(), SECONDS)
              .setTcProperties(MapperUtils.toProperties(tcConfig))
              // plugins
              .setNodeMetadataDir(toNodeMetadataDir(xmlPlugins).orElse(null))
              .setDataDirs(toUserDataDirs(xmlPlugins))
              .setOffheapResources(MapperUtils.toOffheapResources(xmlPlugins))
              .setNodeBackupDir(null)
              .setClientLeaseDuration(MapperUtils.toClientLeaseDuration(xmlPlugins))
              // security
              .setSecurityDir(null)
              .setSecurityAuditLogDir(null)
              .setSecurityWhitelist(false)
              .setSecuritySslTls(false)
              .setSecurityAuthc(null)
      ));
      return new Stripe(nodes);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (SAXException e) {
      throw new IllegalStateException("Invalid config repository XML file: " + e.getMessage(), e);
    }
  }

  public static Map<String, Path> toUserDataDirs(Map<Class<?>, List<Object>> plugins) {
    Map<String, Path> dataDirs = MapperUtils.toDataDirs(plugins, mapping -> !mapping.isUseForPlatform());
    // If the XML defines the deprecated tag "<persistence:platform-persistence data-directory-id="root1"/>"
    // then we get the data directory ID and remove it from the user data directory list
    // because this ID matches the directory used for platform (node metadata)
    toNodeMetadataDir(plugins).ifPresent(dataDirs::remove);
    return dataDirs;
  }

  public static Optional<Path> toNodeMetadataDir(Map<Class<?>, List<Object>> plugins) {
    // First try to find a deprecated service tag "<persistence:platform-persistence data-directory-id="root1"/>"
    // that will give us the ID of the dataroot to use for platform persistence
    return MapperUtils.toDataDirs(plugins, DataRootMapping::isUseForPlatform).values().stream().findFirst();
  }

  private FailoverPriority toFailoverPriority(org.terracotta.config.FailoverPriority failoverPriority) {
    if (failoverPriority != null) {
      return failoverPriority.getConsistency() != null ?
          failoverPriority.getConsistency().getVoter() == null ? FailoverPriority.consistency() :
              FailoverPriority.consistency(failoverPriority.getConsistency().getVoter().getCount()) :
          FailoverPriority.availability();
    } else {
      return FailoverPriority.availability();
    }
  }
}
