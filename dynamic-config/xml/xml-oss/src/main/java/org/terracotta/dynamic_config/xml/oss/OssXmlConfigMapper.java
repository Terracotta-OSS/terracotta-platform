/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml.oss;

import org.slf4j.event.Level;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.config.Config;
import org.terracotta.config.Server;
import org.terracotta.config.Service;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.config.TcProperties;
import org.terracotta.config.data_roots.DataRootConfigParser;
import org.terracotta.config.service.ExtendedConfigParser;
import org.terracotta.config.service.ServiceConfigParser;
import org.terracotta.data.config.DataRootMapping;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.api.service.XmlConfigMapper;
import org.terracotta.dynamic_config.xml.CustomTCConfigurationParser;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.Logger;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcCluster;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcNode;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcStripe;
import org.terracotta.lease.service.config.LeaseConfigurationParser;
import org.terracotta.lease.service.config.LeaseElement;
import org.terracotta.offheapresource.OffHeapResourceConfigurationParser;
import org.terracotta.offheapresource.config.ResourceType;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author Mathieu Carbou
 */
public class OssXmlConfigMapper implements XmlConfigMapper {

  private static final String WILDCARD_IP = "0.0.0.0";
  private static Map<String, ExtendedConfigParser> CONFIG_PARSERS = new HashMap<>();
  private static Map<String, ServiceConfigParser> SERVICE_PARSERS = new HashMap<>();

  static {
    for (ExtendedConfigParser parser : ServiceLoader.load(ExtendedConfigParser.class, OssXmlConfigMapper.class.getClassLoader())) {
      CONFIG_PARSERS.put(parser.getNamespace().toString(), parser);
    }
    for (ServiceConfigParser parser : ServiceLoader.load(ServiceConfigParser.class, OssXmlConfigMapper.class.getClassLoader())) {
      SERVICE_PARSERS.put(parser.getNamespace().toString(), parser);
    }
  }

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
      TcConfiguration xmlTcConfiguration = CustomTCConfigurationParser.parse(xml);
      TcConfig platformConfiguration = xmlTcConfiguration.getPlatformConfiguration();
      Map<Class<?>, List<Object>> xmlPlugins = parsePlugins(xml, platformConfiguration);
      TcCluster xmlCluster = (TcCluster) xmlPlugins.get(TcCluster.class).get(0);
      int stripeId = xmlCluster.getCurrentStripeId();
      Cluster cluster = new Cluster(
          xmlCluster.getName(),
          xmlCluster.getStripes().stream().map(tcStripe -> OssXmlConfigMapper.toStripe(xml, tcStripe)).collect(toList()));
      return new NodeContext(cluster, stripeId, nodeName);
    } catch (IOException e) {
      // should never occur since we parse a string
      throw new UncheckedIOException(e);
    } catch (SAXException e) {
      throw new IllegalStateException("Invalid config repository XML file: " + e.getMessage(), e);
    }
  }

  private static Stripe toStripe(String xml, TcStripe xmlStripe) {
    return new Stripe(xmlStripe.getNodes().stream().map(tcNode -> OssXmlConfigMapper.toNode(xml, tcNode)).collect(toList()));
  }

  private static Node toNode(String xml, TcNode xmlNode) {
    TcConfig xmlTcConfig = xmlNode.getServerConfig().getTcConfig();
    CustomTCConfigurationParser.applyPlatformDefaults(xmlTcConfig, null);
    Server xmlServer = xmlTcConfig.getServers().getServer()
        .stream()
        .filter(server -> server.getName().equals(xmlNode.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Unable to find server node " + xmlNode.getName()));
    Map<Class<?>, List<Object>> xmlPlugins = parsePlugins(xml, xmlTcConfig);
    return Node.empty()
        .setNodeName(xmlNode.getName())
        .setNodeHostname(xmlServer.getHost())
        .setNodePort(xmlServer.getTsaPort().getValue())
        .setNodePublicHostname(xmlNode.getPublicHostname())
        .setNodePublicPort(xmlNode.getPublicPort())
        .setNodeBindAddress(moreRestrictive(xmlServer.getTsaPort().getBind(), xmlServer.getBind()))
        .setNodeGroupPort(xmlServer.getTsaGroupPort().getValue())
        .setNodeGroupBindAddress(moreRestrictive(xmlServer.getTsaGroupPort().getBind(), xmlServer.getBind()))
        .setNodeLogDir(Paths.get(xmlServer.getLogs()))
        .setFailoverPriority(toFailoverPriority(xmlTcConfig.getFailoverPriority()))
        .setClientReconnectWindow(xmlTcConfig.getServers().getClientReconnectWindow(), SECONDS)
        .setTcProperties(toProperties(xmlTcConfig))
        .setNodeLoggerOverrides(toLoggers(xmlNode))
        // plugins
        .setNodeMetadataDir(null)
        .setDataDirs(toUserDataDirs(xmlPlugins))
        .setOffheapResources(toOffheapResources(xmlPlugins))
        .setNodeBackupDir(null)
        .setClientLeaseDuration(toClientLeaseDuration(xmlPlugins))
        // security
        .setSecurityDir(null)
        .setSecurityAuditLogDir(null)
        .setSecurityWhitelist(false)
        .setSecuritySslTls(false)
        .setSecurityAuthc(null);
  }

  private static Map<String, String> toProperties(TcConfig xmlTcConfig) {
    Map<String, String> properties = new HashMap<>();
    TcProperties tcProperties = xmlTcConfig.getTcProperties();
    if (tcProperties != null) {
      tcProperties.getProperty().forEach(p -> properties.put(p.getName(), p.getValue()));
    }
    return properties;
  }

  private static Map<String, Level> toLoggers(TcNode xmlNode) {
    return Optional.ofNullable(xmlNode.getLoggerOverrides())
        .map(loggers -> loggers.getLogger()
            .stream()
            .collect(toMap(
                Logger::getName,
                logger -> Level.valueOf(logger.getLevel().name()),
                (level1, level2) -> level2,
                LinkedHashMap::new)))
        .orElseGet(() -> new LinkedHashMap<>(0));
  }

  private static Measure<TimeUnit> toClientLeaseDuration(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(LeaseElement.class, Collections.emptyList())
        .stream()
        .map(LeaseElement.class::cast)
        .map(leaseElement -> Measure.of(Long.parseLong(leaseElement.getLeaseValue()), TimeUnit.valueOf(leaseElement.getTimeUnit().toUpperCase())))
        .findFirst()
        .orElse(null);
  }

  private static Map<String, Path> toDataDirs(Map<Class<?>, List<Object>> plugins, Predicate<DataRootMapping> filter) {
    return plugins.getOrDefault(DataRootMapping.class, Collections.emptyList())
        .stream()
        .map(DataRootMapping.class::cast)
        .filter(filter)
        .collect(toMap(DataRootMapping::getName, mapping -> Paths.get(mapping.getValue())));
  }

  private static Map<String, Path> toUserDataDirs(Map<Class<?>, List<Object>> plugins) {
    return toDataDirs(plugins, mapping -> !mapping.isUseForPlatform());
  }

  private static Map<String, Measure<MemoryUnit>> toOffheapResources(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(ResourceType.class, Collections.emptyList())
        .stream()
        .map(ResourceType.class::cast)
        .collect(toMap(ResourceType::getName, r -> Measure.of(r.getValue().longValue(), MemoryUnit.parse(r.getUnit().value()))));
  }

  private static FailoverPriority toFailoverPriority(org.terracotta.config.FailoverPriority failoverPriority) {
    requireNonNull(failoverPriority);
    return failoverPriority.getConsistency() != null ?
        failoverPriority.getConsistency().getVoter() == null ? FailoverPriority.consistency() :
            FailoverPriority.consistency(failoverPriority.getConsistency().getVoter().getCount()) :
        FailoverPriority.availability();
  }

  private static String moreRestrictive(String specific, String general) {
    // same behavior as in com.tc.config.ServerConfiguration
    if (WILDCARD_IP.equals(specific) && !WILDCARD_IP.equals(general)) {
      return general;
    }
    return specific;
  }

  private static Map<Class<?>, List<Object>> parsePlugins(String xml, TcConfig tcConfig) {
    return tcConfig
        .getPlugins()
        .getConfigOrService()
        .stream()
        .flatMap(o -> OssXmlConfigMapper.parsePlugin(xml, o))
        .collect(groupingBy(Object::getClass));
  }

  @SuppressWarnings("unchecked")
  private static Stream<?> parsePlugin(String xml, Object o) {
    if (o instanceof Config) {
      Element element = ((Config) o).getConfigContent();
      ExtendedConfigParser parser = CONFIG_PARSERS.get(element.getNamespaceURI());
      // xml is expected to be valid
      if (parser == null) {
        throw new AssertionError("ExtendedConfigParser not found for namespace " + element.getNamespaceURI());
      }
      // special handling for data root to not apply any defaults
      if (parser instanceof DataRootConfigParser) {
        return ((DataRootConfigParser) parser).parser().apply(element).getDirectory().stream();
      }
      // special handling for offheaps to not apply any defaults
      if (parser instanceof OffHeapResourceConfigurationParser) {
        return ((OffHeapResourceConfigurationParser) parser).parser().apply(element).getResource().stream();
      }
      // default case (includes Cluster tag)
      return Stream.of(parser.parse(element, xml));
    } else if (o instanceof Service) {
      Element element = ((Service) o).getServiceContent();
      ServiceConfigParser parser = SERVICE_PARSERS.get(element.getNamespaceURI());
      // xml is expected to be valid
      if (parser == null) {
        throw new AssertionError("ServiceConfigParser not found for namespace " + element.getNamespaceURI());
      }
      // lease special handling
      if (parser instanceof LeaseConfigurationParser) {
        return Stream.of(((LeaseConfigurationParser) parser).parser().apply(element));
      }
      // default case (includes FRSPersistenceConfigurationParser)
      return Stream.of(parser.parse(element, xml));
    } else {
      throw new AssertionError("Unsupported type: " + o.getClass());
    }
  }
}
