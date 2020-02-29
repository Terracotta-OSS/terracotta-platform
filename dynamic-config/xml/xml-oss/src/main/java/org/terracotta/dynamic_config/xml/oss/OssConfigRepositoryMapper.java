/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml.oss;

import org.terracotta.config.Server;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.data.config.DataRootMapping;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.ConfigRepositoryMapper;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.xml.MapperUtils;
import org.terracotta.dynamic_config.xml.NonSubstitutingTCConfigurationParser;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcCluster;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcNode;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcStripe;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

/**
 * @author Mathieu Carbou
 */
public class OssConfigRepositoryMapper implements ConfigRepositoryMapper {

  private PathResolver pathResolver;

  @Override
  public void init(PathResolver pathResolver) {
    this.pathResolver = pathResolver;
  }

  @Override
  public String toXml(NodeContext nodeContext) {
    if (pathResolver == null) {
      throw new IllegalStateException("Missing PathResolver");
    }
    return new OssXmlConfiguration(
        nodeContext.getCluster(),
        nodeContext.getStripeId(),
        nodeContext.getNodeName(),
        pathResolver
    ).toString();
  }

  @Override
  public NodeContext fromXml(String nodeName, String xml) {
    try {
      TcConfiguration xmlTcConfiguration = NonSubstitutingTCConfigurationParser.parse(xml);
      TcConfig platformConfiguration = xmlTcConfiguration.getPlatformConfiguration();
      Map<Class<?>, List<Object>> xmlPlugins = MapperUtils.parsePlugins(xml, platformConfiguration);
      TcCluster xmlCluster = (TcCluster) xmlPlugins.get(TcCluster.class).get(0);
      int stripeId = xmlCluster.getCurrentStripeId();
      Cluster cluster = new Cluster(
          xmlCluster.getName(),
          xmlCluster.getStripes().stream().map(tcStripe -> toStripe(xml, tcStripe)).collect(toList()));
      return new NodeContext(cluster, stripeId, nodeName);
    } catch (IOException e) {
      // should never occur since we parse a string
      throw new UncheckedIOException(e);
    } catch (SAXException e) {
      throw new IllegalStateException("Invalid config repository XML file: " + e.getMessage(), e);
    }
  }

  private static Stripe toStripe(String xml, TcStripe xmlStripe) {
    return new Stripe(xmlStripe.getNodes().stream().map(tcNode -> toNode(xml, tcNode)).collect(toList()));
  }

  private static Node toNode(String xml, TcNode xmlNode) {
    TcConfig xmlTcConfig = xmlNode.getServerConfig().getTcConfig();
    NonSubstitutingTCConfigurationParser.applyPlatformDefaults(xmlTcConfig);
    Server xmlServer = xmlTcConfig.getServers().getServer()
        .stream()
        .filter(server -> server.getName().equals(xmlNode.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Unable to find server node " + xmlNode.getName()));
    Map<Class<?>, List<Object>> xmlPlugins = MapperUtils.parsePlugins(xml, xmlTcConfig);
    return Node.empty()
        .setNodeName(xmlNode.getName())
        .setNodeHostname(xmlServer.getHost())
        .setNodePort(xmlServer.getTsaPort().getValue())
        .setNodePublicHostname(xmlNode.getPublicHostname())
        .setNodePublicPort(xmlNode.getPublicPort())
        .setNodeBindAddress(MapperUtils.moreRestrictive(xmlServer.getTsaPort().getBind(), xmlServer.getBind()))
        .setNodeGroupPort(xmlServer.getTsaGroupPort().getValue())
        .setNodeGroupBindAddress(MapperUtils.moreRestrictive(xmlServer.getTsaGroupPort().getBind(), xmlServer.getBind()))
        .setNodeLogDir(Paths.get(xmlServer.getLogs()))
        .setFailoverPriority(MapperUtils.toFailoverPriority(xmlTcConfig.getFailoverPriority()))
        .setClientReconnectWindow(xmlTcConfig.getServers().getClientReconnectWindow(), SECONDS)
        .setTcProperties(MapperUtils.toProperties(xmlTcConfig))
        .setNodeLoggerOverrides(MapperUtils.toLoggers(xmlNode))
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
        .setSecurityAuthc(null);
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
}
