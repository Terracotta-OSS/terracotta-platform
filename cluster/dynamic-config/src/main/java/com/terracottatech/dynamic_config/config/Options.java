/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.tc.server.TCServerMain;
import com.terracottatech.dynamic_config.Constants;
import com.terracottatech.dynamic_config.managers.ClusterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.util.ParameterSubstitutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.Constants.CONFIG_REPO_FILENAME_REGEX;
import static com.terracottatech.dynamic_config.Constants.NOMAD_CONFIG_DIR;
import static com.terracottatech.dynamic_config.Constants.REGEX_PREFIX;
import static com.terracottatech.dynamic_config.Constants.REGEX_SUFFIX;
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

@Parameters(separators = "=")
public class Options {
  private static final Logger LOGGER = LoggerFactory.getLogger(Options.class);

  @Parameter(names = "--" + NODE_HOSTNAME)
  private String nodeHostname;

  @Parameter(names = "--" + NODE_PORT)
  private int nodePort;

  @Parameter(names = "--" + NODE_GROUP_PORT)
  private int nodeGroupPort;

  @Parameter(names = "--" + NODE_NAME)
  private String nodeName;

  @Parameter(names = "--" + NODE_BIND_ADDRESS)
  private String nodeBindAddress;

  @Parameter(names = "--" + NODE_GROUP_BIND_ADDRESS)
  private String nodeGroupBindAddress;

  @Parameter(names = "--" + NODE_CONFIG_DIR)
  private String nodeConfigDir;

  @Parameter(names = "--" + NODE_METADATA_DIR)
  private String nodeMetadataDir;

  @Parameter(names = "--" + NODE_LOG_DIR)
  private String nodeLogDir;

  @Parameter(names = "--" + NODE_BACKUP_DIR)
  private String nodeBackupDir;

  @Parameter(names = "--" + SECURITY_DIR)
  private String securityDir;

  @Parameter(names = "--" + SECURITY_AUDIT_LOG_DIR)
  private String securityAuditLogDir;

  @Parameter(names = "--" + SECURITY_AUTHC)
  private String securityAuthc;

  @Parameter(names = "--" + SECURITY_SSL_TLS)
  private boolean securitySslTls;

  @Parameter(names = "--" + SECURITY_WHITELIST)
  private boolean securityWhitelist;

  @Parameter(names = "--" + FAILOVER_PRIORITY)
  private String failoverPriority;

  @Parameter(names = "--" + CLIENT_RECONNECT_WINDOW)
  private int clientReconnectWindow;

  @Parameter(names = "--" + CLIENT_LEASE_DURATION)
  private int clientLeaseDuration;

  @Parameter(names = "--" + OFFHEAP_RESOURCES)
  private List<String> offheapResources = new ArrayList<>();

  @Parameter(names = "--" + DATA_DIRS)
  private List<String> dataDirs = new ArrayList<>();

  @Parameter(names = "--" + CLUSTER_NAME)
  private String clusterName;

  @Parameter(names = "--config-file")
  private String configFile;

  @Parameter(names = "--help", help = true)
  private boolean help;

  public void process(JCommander jCommander) {
    if (help) {
      jCommander.usage();
      return;
    }

    Optional<String> configRepo = findConfigRepo();
    if (configRepo.isPresent()) {
      LOGGER.info("Reading cluster config repository from: {}", configRepo.get());
      TCServerMain.main(new String[]{"-r", Paths.get(nodeConfigDir).toString(), "-n", getNodeName(configRepo.get())});
    } else {
      if (configFile != null) {
        if (nodeName != null || nodeGroupPort != 0 || nodeBindAddress != null || nodeGroupBindAddress != null || nodeBackupDir != null ||
            nodeLogDir != null || nodeMetadataDir != null || failoverPriority != null || clientLeaseDuration != 0 ||
            clientReconnectWindow != 0 || clusterName != null || !offheapResources.isEmpty() || !dataDirs.isEmpty() ||
            securitySslTls || securityWhitelist || securityAuditLogDir != null || securityAuthc != null || securityDir != null) {
          throw new ParameterException(
              String.format(
                  "'--config-file' parameter can only be used with '--%s', '--%s', and '--%s' parameters",
                  NODE_HOSTNAME,
                  NODE_PORT,
                  NODE_CONFIG_DIR)
          );
        }
        LOGGER.info("Reading cluster config properties file from: " + configFile);
        ClusterManager.createCluster(configFile);
        //TODO: Expose this cluster object to an MBean
        //TODO: Identify the correct server from the config file and start server - depends on TDB-4483
      } else {
        Map<String, String> paramValueMap = jCommander.getParameters().stream()
            .filter(pd -> !pd.getLongestName().equals("--config-file"))
            .filter(pd -> !pd.getParameter().getAssignment().equals(""))
            .collect(Collectors.toMap(ParameterDescription::getLongestName, pd -> pd.getParameter().getAssignment()));
        ClusterManager.createCluster(paramValueMap);
        //TODO: Expose this cluster object to an MBean
        //TODo: Start the server now - depends on TDB-4483
      }
    }
  }

  private Optional<String> findConfigRepo() {
    String specifiedOrDefaultConfigDir = nodeConfigDir == null ? Constants.DEFAULT_CONFIG_DIR : nodeConfigDir;
    String substitutedConfigDir = ParameterSubstitutor.substitute(specifiedOrDefaultConfigDir);

    try (Stream<Path> stream = Files.list(Paths.get(substitutedConfigDir).resolve(NOMAD_CONFIG_DIR))) {
      return stream.map(path -> path.getFileName().toString()).filter(fileName -> fileName.matches(CONFIG_REPO_FILENAME_REGEX)).findAny();
    } catch (IOException e) {
      LOGGER.debug("Reading cluster config repository from: {} resulted in exception: {}", substitutedConfigDir, e);
    }
    return Optional.empty();
  }

  private String getNodeName(String configRepo) {
    return configRepo.replaceAll("^" + REGEX_PREFIX, "").replaceAll(REGEX_SUFFIX + "$", "");
  }
}
