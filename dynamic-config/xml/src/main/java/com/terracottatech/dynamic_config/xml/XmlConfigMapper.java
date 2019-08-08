/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import com.terracottatech.br.config.BackupRestoreConfigurationParser;
import com.terracottatech.config.br.BackupRestore;
import com.terracottatech.config.data_roots.DataRootConfigParser;
import com.terracottatech.config.security.Security;
import com.terracottatech.data.config.DataRootMapping;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.TcCluster;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.TcNode;
import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.TcStripe;
import com.terracottatech.security.authentication.AuthenticationScheme;
import com.terracottatech.security.server.config.SecurityConfigurationParser;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.PathResolver;
import com.terracottatech.utilities.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.Config;
import org.terracotta.config.FailoverPriority;
import org.terracotta.config.Server;
import org.terracotta.config.Service;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.config.service.ExtendedConfigParser;
import org.terracotta.config.service.ServiceConfigParser;
import org.terracotta.lease.service.config.LeaseConfigurationParser;
import org.terracotta.lease.service.config.LeaseElement;
import org.terracotta.offheapresource.OffHeapResourceConfigurationParser;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * @author Mathieu Carbou
 */
public class XmlConfigMapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlConfigMapper.class);
  private static final String WILDCARD_IP = "0.0.0.0";
  private static Map<String, ExtendedConfigParser> CONFIG_PARSERS = new HashMap<>();
  private static Map<String, ServiceConfigParser> SERVICE_PARSERS = new HashMap<>();

  static {
    for (ExtendedConfigParser parser : ServiceLoader.load(ExtendedConfigParser.class, XmlConfigMapper.class.getClassLoader())) {
      CONFIG_PARSERS.put(parser.getNamespace().toString(), parser);
    }
    for (ServiceConfigParser parser : ServiceLoader.load(ServiceConfigParser.class, XmlConfigMapper.class.getClassLoader())) {
      SERVICE_PARSERS.put(parser.getNamespace().toString(), parser);
    }
  }

  private final PathResolver pathResolver;

  public XmlConfigMapper(PathResolver pathResolver) {
    // allow null values sine path resolver is optional: not used in fromXml()
    this.pathResolver = pathResolver;
  }

  public String toXml(NodeContext nodeContext) {
    if (pathResolver == null) {
      throw new IllegalStateException("Missing PathResolver");
    }
    return new XmlConfiguration(
        nodeContext.getCluster(),
        nodeContext.getStripeId(),
        nodeContext.getNodeName(),
        pathResolver
    ).toString();
  }

  public NodeContext fromXml(String nodeName, String xml) {
    LOGGER.trace("Parsing:\n{}", xml);
    try {
      TcConfiguration xmlTcConfiguration = CustomTCConfigurationParser.parse(xml);
      TcConfig platformConfiguration = xmlTcConfiguration.getPlatformConfiguration();
      Map<Class<?>, List<Object>> xmlPlugins = parsePlugins(xml, platformConfiguration);
      TcCluster xmlCluster = (TcCluster) xmlPlugins.get(TcCluster.class).get(0);
      int stripeId = xmlCluster.getCurrentStripeId();
      Cluster cluster = new Cluster(
          xmlCluster.getName(),
          xmlCluster.getStripes().stream().map(tcStripe -> XmlConfigMapper.toStripe(xml, tcStripe)).collect(toList()));
      return new NodeContext(cluster, stripeId, nodeName);
    } catch (IOException e) {
      // should never occur since we parse a string
      throw new UncheckedIOException(e);
    } catch (SAXException e) {
      throw new IllegalStateException("Invalid config repository XML file: " + e.getMessage(), e);
    }
  }

  private static Stripe toStripe(String xml, TcStripe xmlStripe) {
    return new Stripe(xmlStripe.getNodes().stream().map(tcNode -> XmlConfigMapper.toNode(xml, tcNode)).collect(toList()));
  }

  private static Node toNode(String xml, TcNode xmlNode) {
    TcConfig xmlTcConfig = xmlNode.getServerConfig().getTcConfig();
    CustomTCConfigurationParser.applyPlatformDefaults(xmlTcConfig);
    Server xmlServer = xmlTcConfig.getServers().getServer()
        .stream()
        .filter(server -> server.getName().equals(xmlNode.getName()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Unable to find server node " + xmlNode.getName()));
    Map<Class<?>, List<Object>> xmlPlugins = parsePlugins(xml, xmlTcConfig);
    Optional<Security> security = toSecurity(xmlPlugins);
    return new Node()
        .setNodeName(xmlNode.getName())
        .setNodeHostname(xmlServer.getHost())
        .setNodePort(xmlServer.getTsaPort().getValue())
        .setNodeBindAddress(moreRestrictive(xmlServer.getTsaPort().getBind(), xmlServer.getBind()))
        .setNodeGroupPort(xmlServer.getTsaGroupPort().getValue())
        .setNodeGroupBindAddress(moreRestrictive(xmlServer.getTsaGroupPort().getBind(), xmlServer.getBind()))
        .setNodeLogDir(Paths.get(xmlServer.getLogs()))
        .setFailoverPriority(toFailoverPriority(xmlTcConfig.getFailoverPriority()))
        .setClientReconnectWindow(xmlTcConfig.getServers().getClientReconnectWindow(), SECONDS)
        // plugins
        .setNodeMetadataDir(toNodeMetadataDir(xmlPlugins))
        .setDataDirs(toDataDirs(xmlPlugins))
        .setOffheapResources(toOffheapResources(xmlPlugins))
        .setNodeBackupDir(toNodeBackupDir(xmlPlugins))
        .setClientLeaseDuration(toClientLeaseDuration(xmlPlugins))
        // security
        .setSecurityDir(security.map(Security::getSecurityRootDirectory).map(Paths::get).orElse(null))
        .setSecurityAuditLogDir(security.map(Security::getAuditDirectory).map(Paths::get).orElse(null))
        .setSecurityWhitelist(security.map(s -> s.getWhiteList() != null || s.getWhitelist() != null).orElse(false))
        .setSecuritySslTls(security.map(Security::getSslTls).isPresent())
        .setSecurityAuthc(security
            .map(Security::getAuthentication)
            .map(authentication -> authentication.getFile() != null ? AuthenticationScheme.FILE : authentication.getLdap() != null ? AuthenticationScheme.LDAP : AuthenticationScheme.CERTIFICATE)
            .map(Enum<AuthenticationScheme>::name)
            .map(String::toLowerCase)
            .orElse(null));
  }

  private static Optional<Security> toSecurity(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(Security.class, Collections.emptyList())
        .stream()
        .map(Security.class::cast)
        .findFirst();
  }

  private static Measure<TimeUnit> toClientLeaseDuration(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(LeaseElement.class, Collections.emptyList())
        .stream()
        .map(LeaseElement.class::cast)
        .map(leaseElement -> Measure.of(Long.parseLong(leaseElement.getLeaseValue()), TimeUnit.valueOf(leaseElement.getTimeUnit().toUpperCase())))
        .findFirst()
        .orElse(null);
  }

  private static Path toNodeBackupDir(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(BackupRestore.class, Collections.emptyList())
        .stream()
        .map(BackupRestore.class::cast)
        .map(BackupRestore::getBackupLocation)
        .map(BackupRestore.BackupLocation::getPath)
        .map(Paths::get)
        .findFirst()
        .orElse(null);
  }

  private static Path toNodeMetadataDir(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(DataRootMapping.class, Collections.emptyList())
        .stream()
        .map(DataRootMapping.class::cast)
        .filter(DataRootMapping::isUseForPlatform)
        .map(DataRootMapping::getValue)
        .map(Paths::get)
        .findFirst()
        .orElse(null);
  }

  private static Map<String, Path> toDataDirs(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(DataRootMapping.class, Collections.emptyList())
        .stream()
        .map(DataRootMapping.class::cast)
        .filter(mapping -> !mapping.isUseForPlatform())
        .collect(toMap(DataRootMapping::getName, mapping -> Paths.get(mapping.getValue())));
  }

  private static Map<String, Measure<MemoryUnit>> toOffheapResources(Map<Class<?>, List<Object>> plugins) {
    return plugins.getOrDefault(ResourceType.class, Collections.emptyList())
        .stream()
        .map(ResourceType.class::cast)
        .collect(toMap(ResourceType::getName, r -> Measure.of(r.getValue().longValue(), MemoryUnit.parse(r.getUnit().value()))));
  }

  private static String toFailoverPriority(FailoverPriority failoverPriority) {
    requireNonNull(failoverPriority);
    return failoverPriority.getConsistency() != null ?
        "consistency:" + failoverPriority.getConsistency().getVoter().getCount() :
        "availability";
  }

  private static String moreRestrictive(String specific, String general) {
    // same behavior as in com.tc.config.ServerConfiguration
    if (WILDCARD_IP.equals(specific) && !WILDCARD_IP.equals(general)) {
      return general;
    }
    return specific;
  }

  private static Map<Class<?>, List<Object>> parsePlugins(String xml, TcConfig tcConfig) {
    Map<Class<?>, List<Object>> plugins = tcConfig
        .getPlugins()
        .getConfigOrService()
        .stream()
        .flatMap(o -> XmlConfigMapper.parsePlugin(xml, o))
        .collect(groupingBy(Object::getClass));
    LOGGER.trace("parsePlugins: {}", plugins);
    return plugins;
  }

  @SuppressWarnings("unchecked")
  // TODO [DYNAMIC-CONFIG]: TDB-4644: Update parsers (i.e. offheap, etc) to be able to keep user input such as units and placeholders
  private static Stream<?> parsePlugin(String xml, Object o) {
    try {
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
          // offheap parser is not public
          Method _parser = OffHeapResourceConfigurationParser.class.getDeclaredMethod("parser");
          _parser.setAccessible(true);
          Function<Element, OffheapResourcesType> fn = (Function<Element, OffheapResourcesType>) _parser.invoke(parser);
          return fn.apply(element).getResource().stream();
        }
        // default case
        return Stream.of(parser.parse(element, xml));
      } else if (o instanceof Service) {
        Element element = ((Service) o).getServiceContent();
        ServiceConfigParser parser = SERVICE_PARSERS.get(element.getNamespaceURI());
        // xml is expected to be valid
        if (parser == null) {
          throw new AssertionError("ServiceConfigParser not found for namespace " + element.getNamespaceURI());
        }
        // special handling for backup
        if (parser instanceof BackupRestoreConfigurationParser) {
          return Stream.of(((BackupRestoreConfigurationParser) parser).parseBackupRestore(element, xml));
        }
        // lease special handling
        if (parser instanceof LeaseConfigurationParser) {
          // not public
          Method _parser = LeaseConfigurationParser.class.getDeclaredMethod("parser");
          _parser.setAccessible(true);
          Function<Element, LeaseElement> fn = (Function<Element, LeaseElement>) _parser.invoke(parser);
          return Stream.of(fn.apply(element));
        }
        if (parser instanceof SecurityConfigurationParser) {
          return Stream.of(((SecurityConfigurationParser) parser).parser().apply(element));
        }
        // default case
        return Stream.of(parser.parse(element, xml));
      } else {
        throw new AssertionError("Unsupported type: " + o.getClass());
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(e.getCause());
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }
}
