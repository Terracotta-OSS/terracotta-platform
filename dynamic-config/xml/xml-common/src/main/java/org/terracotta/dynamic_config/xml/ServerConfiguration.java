/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml;

import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.xml.plugins.DataDirectories;
import org.terracotta.dynamic_config.xml.plugins.Lease;
import org.terracotta.dynamic_config.xml.plugins.OffheapResources;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.Level;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.Logger;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.Loggers;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcNode;
import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcServerConfig;
import org.terracotta.config.Config;
import org.terracotta.config.Consistency;
import org.terracotta.config.ObjectFactory;
import org.terracotta.config.Property;
import org.terracotta.config.Servers;
import org.terracotta.config.Service;
import org.terracotta.config.Services;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcProperties;
import org.terracotta.config.Voter;
import org.w3c.dom.Element;

import javax.xml.bind.JAXB;
import java.io.StringWriter;

public class ServerConfiguration {
  protected static final ObjectFactory FACTORY = new ObjectFactory();

  protected final PathResolver pathResolver;

  private final Node node;
  private final TcConfig tcConfig;

  public ServerConfiguration(Node node, Servers servers, PathResolver pathResolver) {
    this.node = node;
    this.pathResolver = pathResolver;
    this.tcConfig = createTcConfig(node, servers);
  }

  public TcConfig getTcConfig() {
    return tcConfig;
  }

  @Override
  public String toString() {
    StringWriter sw = new StringWriter();
    JAXB.marshal(tcConfig, sw);

    return sw.toString();
  }

  private TcConfig createTcConfig(Node node, Servers servers) {
    TcConfig tcConfig = FACTORY.createTcConfig();

    tcConfig.setServers(servers);

    TcProperties tcProperties =
        node.getTcProperties().entrySet()
            .stream()
            .map(entry -> {
              Property property = new Property();
              property.setName(entry.getKey());
              property.setValue(entry.getValue());
              return property;
            })
            .collect(
                TcProperties::new,
                (result, property) -> result.getProperty().add(property),
                (result1, result2) -> result1.getProperty().addAll(result2.getProperty())
            );
    tcConfig.setTcProperties(tcProperties);

    Services services = FACTORY.createServices();

    addOffheapResources(node, services);
    addLeaseConfig(node, services);
    addDataDirectories(node, services);
    addEnterpriseServices(node, services);
    addFailOverPriority(node, tcConfig);

    tcConfig.setPlugins(services);

    return tcConfig;
  }

  protected void addEnterpriseServices(Node node, Services services) {
  }

  private static void addOffheapResources(Node node, Services services) {
    if (node.getOffheapResources() == null) {
      return;
    }

    Config offheapConfig = FACTORY.createConfig();
    offheapConfig.setConfigContent(new OffheapResources(node.getOffheapResources()).toElement());
    services.getConfigOrService().add(offheapConfig);
  }

  private static void addLeaseConfig(Node node, Services services) {
    if (node.getClientLeaseDuration() == null) {
      return;
    }

    Service leaseService = FACTORY.createService();
    leaseService.setServiceContent(new Lease(node.getClientLeaseDuration()).toElement());
    services.getConfigOrService().add(leaseService);
  }

  private void addDataDirectories(Node node, Services services) {
    if (node.getDataDirs() == null && node.getNodeMetadataDir() == null) {
      return;
    }

    Config dataRootConfig = FACTORY.createConfig();
    dataRootConfig.setConfigContent(new DataDirectories(node.getDataDirs(), node.getNodeMetadataDir(), pathResolver).toElement());
    services.getConfigOrService().add(dataRootConfig);
  }

  private static void addFailOverPriority(Node node, TcConfig tcConfig) {
    if (node.getFailoverPriority() == null) {
      return;
    }

    org.terracotta.config.FailoverPriority failOverPriorityConfig = FACTORY.createFailoverPriority();
    FailoverPriority failoverPriority = node.getFailoverPriority();
    if (failoverPriority == null) {
      return;
    }

    if (failoverPriority.getType() == FailoverPriority.Type.AVAILABILITY) {
      failOverPriorityConfig.setAvailability("");
    } else if (failoverPriority.getType() == FailoverPriority.Type.CONSISTENCY) {
      int voterCount = failoverPriority.getVoters();

      Consistency consistency = FACTORY.createConsistency();
      if (voterCount != 0) {
        Voter voter = FACTORY.createVoter();
        voter.setCount(voterCount);
        consistency.setVoter(voter);
      }
      failOverPriorityConfig.setConsistency(consistency);
    }

    tcConfig.setFailoverPriority(failOverPriorityConfig);
  }

  public void addClusterTopology(Element clusterTopology) {
    Config config = FACTORY.createConfig();
    config.setConfigContent(clusterTopology);

    this.tcConfig.getPlugins().getConfigOrService().add(config);
  }

  public TcNode getClusterConfigNode(org.terracotta.dynamic_config.xml.topology.config.xmlobjects.ObjectFactory factory) {
    TcNode tcNode = factory.createTcNode();

    tcNode.setName(node.getNodeName());
    tcNode.setPublicHostname(node.getNodePublicHostname());
    tcNode.setPublicPort(node.getNodePublicPort());

    if (!node.getNodeLoggerOverrides().isEmpty()) {
      Loggers loggers = new Loggers();
      node.getNodeLoggerOverrides().forEach((name, level) -> {
        Logger logger = new Logger();
        logger.setName(name);
        logger.setLevel(Level.valueOf(level.name()));
        loggers.getLogger().add(logger);
      });
      tcNode.setLoggerOverrides(loggers);
    }

    TcServerConfig serverConfig = factory.createTcServerConfig();
    serverConfig.setTcConfig(this.tcConfig);
    tcNode.setServerConfig(serverConfig);

    return tcNode;
  }
}
