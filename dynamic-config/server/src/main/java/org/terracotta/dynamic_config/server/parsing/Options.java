/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.parsing;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.service.ConfigChangeHandlerManagerImpl;
import org.terracotta.dynamic_config.server.service.ParameterSubstitutor;
import org.terracotta.dynamic_config.server.startup.NodeProcessor;
import org.terracotta.dynamic_config.server.startup.StartupManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.terracotta.dynamic_config.api.model.SettingName.CLIENT_LEASE_DURATION;
import static org.terracotta.dynamic_config.api.model.SettingName.CLIENT_RECONNECT_WINDOW;
import static org.terracotta.dynamic_config.api.model.SettingName.CLUSTER_NAME;
import static org.terracotta.dynamic_config.api.model.SettingName.DATA_DIRS;
import static org.terracotta.dynamic_config.api.model.SettingName.FAILOVER_PRIORITY;
import static org.terracotta.dynamic_config.api.model.SettingName.LICENSE_FILE;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_GROUP_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_METADATA_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_NAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_PUBLIC_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_PUBLIC_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_REPOSITORY_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_AUDIT_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_AUTHC;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_SSL_TLS;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_WHITELIST;
import static org.terracotta.dynamic_config.api.model.SettingName.TC_PROPERTIES;
import static org.terracotta.dynamic_config.server.parsing.ConsoleParamsUtils.addDashDash;
import static org.terracotta.dynamic_config.server.parsing.ConsoleParamsUtils.stripDashDash;

@Parameters(separators = "=")
public class Options {
  @Parameter(names = {"-s", "--" + NODE_HOSTNAME})
  private String nodeHostname;

  @Parameter(names = {"-S", "--" + NODE_PUBLIC_HOSTNAME})
  private String nodePublicHostname;

  @Parameter(names = {"-p", "--" + NODE_PORT})
  private String nodePort;

  @Parameter(names = {"-P", "--" + NODE_PUBLIC_PORT})
  private String nodePublicPort;

  @Parameter(names = {"-g", "--" + NODE_GROUP_PORT})
  private String nodeGroupPort;

  @Parameter(names = {"-n", "--" + NODE_NAME})
  private String nodeName;

  @Parameter(names = {"-a", "--" + NODE_BIND_ADDRESS})
  private String nodeBindAddress;

  @Parameter(names = {"-A", "--" + NODE_GROUP_BIND_ADDRESS})
  private String nodeGroupBindAddress;

  @Parameter(names = {"-r", "--" + NODE_REPOSITORY_DIR})
  private String nodeRepositoryDir;

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

  @Parameter(names = {"-R", "--" + CLIENT_RECONNECT_WINDOW})
  private String clientReconnectWindow;

  @Parameter(names = {"-i", "--" + CLIENT_LEASE_DURATION})
  private String clientLeaseDuration;

  @Parameter(names = {"-o", "--" + OFFHEAP_RESOURCES})
  private String offheapResources;

  @Parameter(names = {"-d", "--" + DATA_DIRS})
  private String dataDirs;

  @Parameter(names = {"-f", "--config-file"})
  private String configFile;

  @Parameter(names = {"-T", "--" + TC_PROPERTIES})
  private String tcProperties;

  @Parameter(names = {"-h", "--help"}, help = true)
  private boolean help;

  @Parameter(names = {"-N", "--" + CLUSTER_NAME})
  private String clusterName;

  @Parameter(names = {"-l", "--" + LICENSE_FILE})
  private String licenseFile;

  private Collection<String> specifiedOptions;

  public void process(CustomJCommander jCommander) {
    if (help) {
      jCommander.usage();
      return;
    }

    // get specified options but remove the ones related to functional stuff
    specifiedOptions = jCommander.getUserSpecifiedOptions();
    validateOptions();

    ParameterSubstitutor parameterSubstitutor = new ParameterSubstitutor();
    ConfigChangeHandlerManager changeHandlerManager = new ConfigChangeHandlerManagerImpl();

    NodeProcessor nodeProcessor = new NodeProcessor(
        this,
        buildParamValueMap(jCommander),
        new ClusterFactory(),
        new StartupManager(parameterSubstitutor, changeHandlerManager),
        parameterSubstitutor
    );
    nodeProcessor.process();
  }

  /**
   * Constructs a {@code Map} containing only the parameters relevant to {@code Node} object with longest parameter name
   * as the key and user-specified-value as the value.
   *
   * @param jCommander jCommander instance
   * @return the constructed map
   */
  private Map<Setting, String> buildParamValueMap(CustomJCommander jCommander) {
    Predicate<ParameterDescription> isSpecified =
        pd -> Arrays.stream(pd.getNames().split(","))
            .map(String::trim)
            .anyMatch(specifiedOptions::contains);

    return jCommander.getParameters()
        .stream()
        .filter(isSpecified)
        .filter(pd -> {
          String longestName = pd.getLongestName();
          return !longestName.equals(addDashDash(LICENSE_FILE)) && !longestName.equals("--config-file") && !longestName.equals(addDashDash(NODE_REPOSITORY_DIR));
        })
        .collect(Collectors.toMap(pd -> Setting.fromName(stripDashDash(pd.getLongestName())), pd -> pd.getParameterized().get(this).toString()));
  }

  private void validateOptions() {
    if (configFile == null) {
      // when using CLI parameters

      if (licenseFile != null && clusterName == null) {
        throw new ParameterException("'--license-file' parameter must be used with '--cluster-name' parameter");
      }

    } else {
      // when using config file

      Set<String> filteredOptions = new HashSet<>(specifiedOptions);
      filteredOptions.remove("-f");
      filteredOptions.remove("-l");
      filteredOptions.remove("-N");
      filteredOptions.remove("-s");
      filteredOptions.remove("-p");
      filteredOptions.remove("-r");

      filteredOptions.remove("--config-file");
      filteredOptions.remove(addDashDash(LICENSE_FILE));
      filteredOptions.remove(addDashDash(CLUSTER_NAME));
      filteredOptions.remove(addDashDash(NODE_HOSTNAME));
      filteredOptions.remove(addDashDash(NODE_PORT));
      filteredOptions.remove(addDashDash(NODE_REPOSITORY_DIR));

      if (filteredOptions.size() != 0) {
        throw new ParameterException(
            String.format(
                "'--config-file' parameter can only be used with '%s', '%s', '%s', '%s' and '%s' parameters",
                addDashDash(LICENSE_FILE),
                addDashDash(CLUSTER_NAME),
                addDashDash(NODE_HOSTNAME),
                addDashDash(NODE_PORT),
                addDashDash(NODE_REPOSITORY_DIR)
            )
        );
      }
    }
  }

  public String getNodeHostname() {
    return nodeHostname;
  }

  public String getNodePublicHostname() {
    return nodePublicHostname;
  }

  public String getNodePort() {
    return nodePort;
  }

  public String getNodePublicPort() {
    return nodePublicPort;
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

  public String getNodeRepositoryDir() {
    return nodeRepositoryDir;
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

  public String getTcProperties() {
    return tcProperties;
  }
}
