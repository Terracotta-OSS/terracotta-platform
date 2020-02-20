/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml.oss;

import org.terracotta.config.Server;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
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
              .setNodeMetadataDir(null)
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

  private static Map<String, Path> toUserDataDirs(Map<Class<?>, List<Object>> plugins) {
    return MapperUtils.toDataDirs(plugins, mapping -> !mapping.isUseForPlatform());
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
