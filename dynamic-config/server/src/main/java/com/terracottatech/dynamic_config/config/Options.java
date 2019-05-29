/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.Constants;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.management.ClusterTopologyMBeanImpl;
import com.terracottatech.dynamic_config.managers.ClusterManager;
import com.terracottatech.dynamic_config.parsing.CustomJCommander;
import com.terracottatech.dynamic_config.util.ConsoleParamsUtils;
import com.terracottatech.dynamic_config.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.util.ParameterSubstitutor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.config.CommonOptions.CLUSTER_NAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.config.CommonOptions.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_CONFIG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_METADATA_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_NAME;
import static com.terracottatech.dynamic_config.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_DIR;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.config.CommonOptions.SECURITY_WHITELIST;
import static com.terracottatech.dynamic_config.managers.NodeManager.startServer;
import static com.terracottatech.dynamic_config.util.ConfigUtils.findConfigRepo;
import static com.terracottatech.dynamic_config.util.ConfigUtils.getSubstitutedConfigDir;

@Parameters(separators = "=")
public class Options {
  private static final Logger LOGGER = LoggerFactory.getLogger(Options.class);

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

  @Parameter(names = {"-l", "--" + NODE_LOG_DIR})
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

  @Parameter(names = {"-N", "--" + CLUSTER_NAME})
  private String clusterName;

  @Parameter(names = {"-f", "--config-file"})
  private String configFile;

  @Parameter(names = {"-h", "--help"}, help = true)
  private boolean help;

  public void process(CustomJCommander jCommander) {
    if (help) {
      jCommander.usage();
      return;
    }

    Optional<String> configRepo = findConfigRepo(nodeConfigDir);
    if (configRepo.isPresent()) {
      startServer("-r", Paths.get(getSubstitutedConfigDir(nodeConfigDir)).toString(), "-n", extractNodeName(configRepo.get()));
    } else {
      LOGGER.info("Attempting to start the node in UNCONFIGURED state");
      Cluster cluster;
      Node node;
      Set<String> specifiedOptions = jCommander.getUserSpecifiedOptions();
      if (configFile != null) {
        LOGGER.info("Reading cluster config properties file from: {}", configFile);
        validateOptionsForConfigFile(specifiedOptions);
        cluster = ClusterManager.createCluster(configFile);
        node = getMatchingNodeFromConfigFile(cluster, specifiedOptions);
      } else {
        Map<String, String> paramValueMap = buildParamValueMap(jCommander, specifiedOptions);
        cluster = ClusterManager.createCluster(paramValueMap);
        node = cluster.getStripes().get(0).getNodes().iterator().next(); // Cluster object will have only 1 node, just get that
      }

      ClusterTopologyMBeanImpl.init(cluster);
      Path configPath = ConfigUtils.createTempTcConfig(node);
      startServer("--config-consistency", "--config", configPath.toAbsolutePath().toString());
    }
  }

  private Map<String, String> buildParamValueMap(CustomJCommander jCommander, Set<String> specifiedOptions) {
    Predicate<ParameterDescription> isSpecified =
        pd -> Arrays.stream(pd.getNames()
            .split(Constants.MULTI_VALUE_SEP))
            .map(String::trim)
            .anyMatch(specifiedOptions::contains);
    return jCommander.getParameters()
        .stream()
        .filter(isSpecified)
        .collect(Collectors.toMap(pd -> ConsoleParamsUtils.stripDashDash(pd.getLongestName()), pd -> pd.getParameterized().get(this).toString()));
  }

  private void validateOptionsForConfigFile(Set<String> specifiedOptions) {
    Set<String> filteredOptions = new HashSet<>(specifiedOptions);
    filteredOptions.remove("-f");
    filteredOptions.remove("-s");
    filteredOptions.remove("-p");
    filteredOptions.remove("-c");

    filteredOptions.remove("--config-file");
    filteredOptions.remove(ConsoleParamsUtils.addDashDash(NODE_HOSTNAME));
    filteredOptions.remove(ConsoleParamsUtils.addDashDash(NODE_PORT));
    filteredOptions.remove(ConsoleParamsUtils.addDashDash(NODE_CONFIG_DIR));

    if (filteredOptions.size() != 0) {
      throw new ParameterException(
          String.format(
              "'--config-file' parameter can only be used with '%s', '%s', and '%s' parameters",
              ConsoleParamsUtils.addDashDash(NODE_HOSTNAME),
              ConsoleParamsUtils.addDashDash(NODE_PORT),
              ConsoleParamsUtils.addDashDash(NODE_CONFIG_DIR)
          )
      );
    }
  }

  private Node getMatchingNodeFromConfigFile(Cluster cluster, Set<String> specifiedOptions) {
    boolean isHostnameSpecified = specifiedOptions.contains(ConsoleParamsUtils.addDash(NODE_HOSTNAME)) || specifiedOptions.contains(ConsoleParamsUtils.addDashDash(NODE_HOSTNAME));
    boolean isPortSpecified = specifiedOptions.contains(ConsoleParamsUtils.addDash(NODE_PORT)) || specifiedOptions.contains(ConsoleParamsUtils.addDashDash(NODE_PORT));

    String substitutedHost = ParameterSubstitutor.substitute(isHostnameSpecified ? nodeHostname : Constants.DEFAULT_HOSTNAME);
    String port = isPortSpecified ? nodePort : Constants.DEFAULT_PORT;

    List<Node> allNodes = cluster.getStripes().stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .collect(Collectors.toList());
    Optional<Node> matchingNodeOptional = allNodes.stream()
        .filter(node1 -> node1.getNodeHostname().equals(substitutedHost) && node1.getNodePort() == Integer.parseInt(port))
        .findAny();

    Node node;
    // See if we find a match for a node based on the specified params. If not, we see if the config file contains just one node
    if (matchingNodeOptional.isPresent()) {
      LOGGER.info("Found matching node entry from config file based on {}={} and {}={}", NODE_HOSTNAME, substitutedHost, NODE_PORT, port);
      node = matchingNodeOptional.get();
    } else if (!isHostnameSpecified && !isPortSpecified && allNodes.size() == 1) {
      LOGGER.info("Found only one node information in config file");
      node = allNodes.get(0);
    } else {
      throw new RuntimeException(
          String.format(
              "Did not find a matching node entry in config file: %s based on %s=%s and %s=%s",
              configFile,
              NODE_HOSTNAME,
              substitutedHost,
              NODE_PORT,
              port
          )
      );
    }
    return node;
  }

  private String extractNodeName(String configRepo) {
    return configRepo.replaceAll("^" + Constants.REGEX_PREFIX, "").replaceAll(Constants.REGEX_SUFFIX + "$", "");
  }
}
