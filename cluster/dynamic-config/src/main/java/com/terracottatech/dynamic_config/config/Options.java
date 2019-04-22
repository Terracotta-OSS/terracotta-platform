/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.tc.server.TCServerMain;
import com.terracottatech.dynamic_config.managers.ClusterManager;
import org.terracotta.config.util.ParameterSubstitutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.config.AllOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.config.AllOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.config.AllOptions.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.config.AllOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.config.AllOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_CONFIG_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_NAME;
import static com.terracottatech.dynamic_config.config.AllOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.config.AllOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.config.AllOptions.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.config.AllOptions.SECURITY_DIR;
import static com.terracottatech.dynamic_config.config.AllOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.config.AllOptions.SECURITY_WHITELIST;
import static com.terracottatech.dynamic_config.util.Constants.CONFIG_REPO_REGEX;
import static com.terracottatech.utilities.HostAndIpValidator.isValidIPv6;

public class Options {
  @Parameter(names = "--" + NODE_HOSTNAME, description = "Node hostname")
  private String nodeHostname;

  @Parameter(names = "--" + NODE_PORT, description = "Node port")
  private int nodePort;

  @Parameter(names = "--" + NODE_GROUP_PORT, description = "Node group port")
  private int nodeGroupPort;

  @Parameter(names = "--" + NODE_NAME, description = "Node name")
  private String nodeName;

  @Parameter(names = "--" + NODE_BIND_ADDRESS, description = "Node bind address")
  private String nodeBindAddress;

  @Parameter(names = "--" + NODE_GROUP_BIND_ADDRESS, description = "Node group bind address")
  private String nodeGroupBindAddress;

  @Parameter(names = "--" + NODE_CONFIG_DIR, description = "Node's config data directory")
  private String nodeConfigDir;

  @Parameter(names = "--" + NODE_METADATA_DIR, description = "Node's metadata directory")
  private String nodeMetadataDir;

  @Parameter(names = "--" + NODE_LOG_DIR, description = "Node's logs directory")
  private String nodeLogDir;

  @Parameter(names = "--" + NODE_BACKUP_DIR, description = "Node's backup directory")
  private String nodeBackupDir;

  @Parameter(names = "--" + SECURITY_DIR, description = "Security directory")
  private String securityDir;

  @Parameter(names = "--" + SECURITY_AUDIT_LOG_DIR, description = "Security audit logs directory")
  private String securityAuditLogDir;

  @Parameter(names = "--" + SECURITY_AUTHC, description = "Security authentication setting")
  private String securityAuthc;

  @Parameter(names = "--" + SECURITY_SSL_TLS, description = "Security SSL/TLS setting")
  private boolean securitySslTls;

  @Parameter(names = "--" + SECURITY_WHITELIST, description = "Security whitelist setting")
  private boolean securityWhitelist;

  @Parameter(names = "--" + FAILOVER_PRIORITY, description = "Failover priority")
  private String failoverPriority;

  @Parameter(names = "--" + CLIENT_RECONNECT_WINDOW, description = "Client reconnect window")
  private int clientReconnectWindow;

  @Parameter(names = "--" + CLIENT_LEASE_DURATION, description = "Client lease duration")
  private int clientLeaseDuration;

  @Parameter(names = "--" + OFFHEAP_RESOURCES, description = "Offheap resources")
  private List<String> offheapResources = new ArrayList<>();

  @Parameter(names = "--" + DATA_DIRS, description = "Data directories")
  private List<String> dataDirs = new ArrayList<>();

  @Parameter(names = "--" + CLUSTER_NAME, description = "Cluster name")
  private String clusterName;

  @Parameter(names = "--config-file", description = "Configuration properties file")
  private String configFile;

  @Parameter(names = "--help", help = true, description = "Display help text")
  private boolean help;

  public void process(JCommander jCommander) {
    if (help) {
      jCommander.usage();
      return;
    }

    if (configRepoFound()) {
      TCServerMain.main(new String[]{"-r", Paths.get(nodeConfigDir).resolve(getNodeName()).toString(), "-n", getNodeName()});
    } else {
      if (configFile != null) {
        if (nodeName != null || nodeGroupPort != 0 || nodeBindAddress != null || nodeGroupBindAddress != null || nodeBackupDir != null ||
            nodeLogDir != null || nodeMetadataDir != null || failoverPriority != null || clientLeaseDuration != 0 ||
            clientReconnectWindow != 0 || clusterName != null || !offheapResources.isEmpty() || !dataDirs.isEmpty() ||
            !securitySslTls || !securityWhitelist || securityAuditLogDir != null || securityAuthc != null || securityDir != null) {
          throw new ParameterException(
              String.format(
                  "'--config-file' parameter can only be used with '--%s', '--%s', and '--%s' parameters",
                  NODE_HOSTNAME,
                  NODE_PORT,
                  NODE_CONFIG_DIR)
          );
        }
        ClusterManager.createCluster(configFile);
        //TODO: Expose this cluster object to an MBean
        //TODO: Identify the correct server from the config file and start server
      } else {
        Function<ParameterDescription, String> valueMapper = pd -> {
          String assignedValue = pd.getParameter().getAssignment();
          return assignedValue != null ? assignedValue : pd.getDefault().toString();
        };
        Map<String, String> paramValueMap = jCommander.getParameters().stream()
            .filter(pd -> !pd.getLongestName().equals("--config-file"))
            .collect(Collectors.toMap(parameterDescription -> parameterDescription.getLongestName().substring(2), valueMapper));
        ClusterManager.createCluster(paramValueMap);
        //TODO: Expose this cluster object to an MBean
        //TODo: Start the server now
      }
    }
  }

  private boolean configRepoFound() {
    try {
      return Files.list(Paths.get(nodeConfigDir).resolve(getNodeName()).resolve("config"))
          .map(path -> path.getFileName().toString())
          .anyMatch(fileName -> fileName.matches(CONFIG_REPO_REGEX));
    } catch (IOException e) {
      return false;
    }
  }

  private String getNodeName() {
    return nodeName != null ? nodeName : getHostPort();
  }

  private String getHostPort() {
    String hostName = ParameterSubstitutor.substitute(nodeHostname);
    return isValidIPv6(hostName) ? ("[" + hostName + "]:" + nodePort) : (hostName + ":" + nodePort);
  }
}
