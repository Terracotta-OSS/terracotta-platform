/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml;

import org.terracotta.config.Config;
import org.terracotta.config.Consistency;
import org.terracotta.config.FailoverPriority;
import org.terracotta.config.ObjectFactory;
import org.terracotta.config.Servers;
import org.terracotta.config.Service;
import org.terracotta.config.Services;
import org.terracotta.config.TcConfig;
import org.terracotta.config.Voter;
import org.w3c.dom.Element;

import com.terracottatech.dynamic_config.config.Node;
import com.terracottatech.dynamic_config.xml.plugins.BackupRestore;
import com.terracottatech.dynamic_config.xml.plugins.DataDirectories;
import com.terracottatech.dynamic_config.xml.plugins.Lease;
import com.terracottatech.dynamic_config.xml.plugins.OffheapResources;
import com.terracottatech.dynamic_config.xml.plugins.Security;
import com.terracottatech.topology.config.xmlobjects.ServerConfig;

import java.io.StringWriter;

import javax.xml.bind.JAXB;

public class ServerConfiguration {
  private static final ObjectFactory FACTORY = new ObjectFactory();

  private final String serverName;
  private final TcConfig tcConfig;

  ServerConfiguration(Node node, Servers servers) {
    this.serverName = node.getNodeName();
    this.tcConfig = createTcConfig(node, servers);
  }

  TcConfig getTcConfig() {
    return tcConfig;
  }

  @Override
  public String toString() {
    StringWriter sw = new StringWriter();
    JAXB.marshal(tcConfig, sw);

    return sw.toString();
  }

  private static TcConfig createTcConfig(Node node, Servers servers) {
    TcConfig tcConfig = FACTORY.createTcConfig();

    tcConfig.setServers(servers);

    Services services = FACTORY.createServices();

    addOffheapResources(node, services);
    addLeaseConfig(node, services);
    addDataDirectories(node, services);
    addBackupConfig(node, services);
    addSecurityConfig(node, services);
    addFailOverPriority(node, tcConfig);

    tcConfig.setPlugins(services);

    return tcConfig;
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

  private static void addSecurityConfig(Node node, Services services) {
    if (node.getSecurityDir() == null) {
      return;
    }

    Service securityConfig = FACTORY.createService();
    securityConfig.setServiceContent(new Security(node).toElement());
    services.getConfigOrService().add(securityConfig);
  }

  private static void addBackupConfig(Node node, Services services) {
    if (node.getNodeBackupDir() == null) {
      return;
    }

    Service backupConfig = FACTORY.createService();
    backupConfig.setServiceContent(new BackupRestore(node.getNodeBackupDir()).toElement());
    services.getConfigOrService().add(backupConfig);
  }

  private static void addDataDirectories(Node node, Services services) {
    if (node.getDataDirs() == null && node.getNodeMetadataDir() == null) {
      return;
    }

    Config dataRootConfig = FACTORY.createConfig();
    dataRootConfig.setConfigContent(new DataDirectories(node.getDataDirs(), node.getNodeMetadataDir()).toElement());
    services.getConfigOrService().add(dataRootConfig);
  }

  private static void addFailOverPriority(Node node, TcConfig tcConfig) {
    if (node.getFailoverPriority() == null) {
      return;
    }

    FailoverPriority failOverPriorityConfig = FACTORY.createFailoverPriority();
    String failOverPriority = node.getFailoverPriority();
    if (failOverPriority == null) {
      return;
    }

    if ("availability".equals(failOverPriority)) {
      failOverPriorityConfig.setAvailability("");
    } else if (failOverPriority.contains("consistency")) {
      String[] tokens = failOverPriority.split(":");
      int voterCount = 0;
      if (tokens.length == 2) {
        voterCount = Integer.parseInt(tokens[1]);
      }

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

  void addClusterTopology(Element clusterTopology) {
    Config config = FACTORY.createConfig();
    config.setConfigContent(clusterTopology);

    this.tcConfig.getPlugins().getConfigOrService().add(config);
  }

  com.terracottatech.topology.config.xmlobjects.Node
  getClusterConfigNode(com.terracottatech.topology.config.xmlobjects.ObjectFactory factory) {
    com.terracottatech.topology.config.xmlobjects.Node node = factory.createNode();
    node.setName(this.serverName);
    ServerConfig serverConfig = factory.createServerConfig();
    serverConfig.setTcConfig(this.tcConfig);
    node.setServerConfig(serverConfig);

    return node;
  }
}
