/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.DynamicConfigConstants;
import com.terracottatech.dynamic_config.managers.NodeManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.model.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_CONFIG_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_NAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_WHITELIST;
import static com.terracottatech.dynamic_config.model.util.ConsoleParamsUtils.addDashDash;
import static com.terracottatech.dynamic_config.model.util.ConsoleParamsUtils.stripDashDash;

@Parameters(separators = "=")
public class Options {
  @Parameter(names = {"-s", "--" + NODE_HOSTNAME})
  private String nodeHostname;

  @Parameter(names = {"-p", "--" + NODE_PORT})
  private String nodePort;

  @Parameter(names = {"-P", "--" + NODE_GROUP_PORT})
  private String nodeGroupPort;

  @Parameter(names = {"-n", "--" + NODE_NAME})
  private String nodeName;

  @Parameter(names = {"-a", "--" + NODE_BIND_ADDRESS})
  private String nodeBindAddress;

  @Parameter(names = {"-A", "--" + NODE_GROUP_BIND_ADDRESS})
  private String nodeGroupBindAddress;

  @Parameter(names = {"-c", "--" + NODE_CONFIG_DIR})
  private String nodeConfigDir;

  @Parameter(names = {"-m", "--" + NODE_METADATA_DIR})
  private String nodeMetadataDir;

  @Parameter(names = {"-L", "--" + NODE_LOG_DIR})
  private String nodeLogDir;

  @Parameter(names = {"-b", "--" + NODE_BACKUP_DIR})
  private String nodeBackupDir;

  @Parameter(names = {"-x", "--" + SECURITY_DIR})
  private String securityDir;

  @Parameter(names = {"-u", "--" + SECURITY_AUDIT_LOG_DIR})
  private String securityAuditLogDir;

  @Parameter(names = {"-z", "--" + SECURITY_AUTHC})
  private String securityAuthc;

  @Parameter(names = {"-t", "--" + SECURITY_SSL_TLS})
  private String securitySslTls;

  @Parameter(names = {"-w", "--" + SECURITY_WHITELIST})
  private String securityWhitelist;

  @Parameter(names = {"-y", "--" + FAILOVER_PRIORITY})
  private String failoverPriority;

  @Parameter(names = {"-r", "--" + CLIENT_RECONNECT_WINDOW})
  private String clientReconnectWindow;

  @Parameter(names = {"-i", "--" + CLIENT_LEASE_DURATION})
  private String clientLeaseDuration;

  @Parameter(names = {"-o", "--" + OFFHEAP_RESOURCES})
  private String offheapResources;

  @Parameter(names = {"-d", "--" + DATA_DIRS})
  private String dataDirs;

  @Parameter(names = {"-N", "--" + CLUSTER_NAME}, hidden = true)
  private String clusterName;

  @Parameter(names = {"-f", "--config-file"})
  private String configFile;

  @Parameter(names = {"-l", "--license-file"}, hidden = true)
  private String licenseFile;

  @Parameter(names = {"-h", "--help"}, help = true)
  private boolean help;

  public void process(CustomJCommander jCommander) {
    if (help) {
      jCommander.usage();
      return;
    }

    Set<String> userSpecifiedOptions = jCommander.getUserSpecifiedOptions();
    validateOptionsForConfigFile(userSpecifiedOptions);
    Map<String, String> paramValueMap = buildParamValueMap(jCommander, userSpecifiedOptions);
    NodeManager nodeManager = new NodeManager(this, userSpecifiedOptions, paramValueMap);
    nodeManager.startNode();
  }

  private Map<String, String> buildParamValueMap(CustomJCommander jCommander, Set<String> specifiedOptions) {
    Predicate<ParameterDescription> isSpecified =
        pd -> Arrays.stream(pd.getNames().split(DynamicConfigConstants.MULTI_VALUE_SEP))
            .map(String::trim)
            .anyMatch(specifiedOptions::contains);
    return jCommander.getParameters()
        .stream()
        .filter(isSpecified)
        .collect(Collectors.toMap(pd -> stripDashDash(pd.getLongestName()), pd -> pd.getParameterized().get(this).toString()));
  }

  private void validateOptionsForConfigFile(Set<String> specifiedOptions) {
    if (configFile == null) {
      return;
    }

    Set<String> filteredOptions = new HashSet<>(specifiedOptions);
    filteredOptions.remove("-f");
    filteredOptions.remove("-l");
    filteredOptions.remove("-s");
    filteredOptions.remove("-p");
    filteredOptions.remove("-c");
    filteredOptions.remove("-N");

    filteredOptions.remove("--config-file");
    filteredOptions.remove("--license-file");
    filteredOptions.remove(addDashDash(NODE_HOSTNAME));
    filteredOptions.remove(addDashDash(NODE_PORT));
    filteredOptions.remove(addDashDash(NODE_CONFIG_DIR));
    filteredOptions.remove(addDashDash(CLUSTER_NAME));

    if (filteredOptions.size() != 0) {
      throw new ParameterException(
          String.format(
              "'--config-file' parameter can only be used with '%s', '%s', '%s', '%s', and '%s' parameters",
              "--license-file",
              addDashDash(CLUSTER_NAME),
              addDashDash(NODE_HOSTNAME),
              addDashDash(NODE_PORT),
              addDashDash(NODE_CONFIG_DIR)
          )
      );
    }
  }

  public String getNodeHostname() {
    return nodeHostname;
  }

  public String getNodePort() {
    return nodePort;
  }

  public String getNodeGroupPort() {
    return nodeGroupPort;
  }

  public String getNodeName() {
    return nodeName;
  }

  public String getNodeBindAddress() {
    return nodeBindAddress;
  }

  public String getNodeGroupBindAddress() {
    return nodeGroupBindAddress;
  }

  public String getNodeConfigDir() {
    return nodeConfigDir;
  }

  public String getNodeMetadataDir() {
    return nodeMetadataDir;
  }

  public String getNodeLogDir() {
    return nodeLogDir;
  }

  public String getNodeBackupDir() {
    return nodeBackupDir;
  }

  public String getSecurityDir() {
    return securityDir;
  }

  public String getSecurityAuditLogDir() {
    return securityAuditLogDir;
  }

  public String getSecurityAuthc() {
    return securityAuthc;
  }

  public String getSecuritySslTls() {
    return securitySslTls;
  }

  public String getSecurityWhitelist() {
    return securityWhitelist;
  }

  public String getFailoverPriority() {
    return failoverPriority;
  }

  public String getClientReconnectWindow() {
    return clientReconnectWindow;
  }

  public String getClientLeaseDuration() {
    return clientLeaseDuration;
  }

  public String getOffheapResources() {
    return offheapResources;
  }

  public String getDataDirs() {
    return dataDirs;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getConfigFile() {
    return configFile;
  }

  public String getLicenseFile() {
    return licenseFile;
  }
}
