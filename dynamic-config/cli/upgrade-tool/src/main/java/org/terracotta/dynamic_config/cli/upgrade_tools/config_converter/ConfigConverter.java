/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.SettingName;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Substitutor;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.TcConfigMapper;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.TcConfigMapperDiscovery;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

public class ConfigConverter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigConverter.class);

  private final Consumer<Cluster> postConversionProcessor;
  private final boolean acceptRelativePaths;

  public ConfigConverter(Consumer<Cluster> conversionProcessor) {
    this(conversionProcessor, false);
  }

  public ConfigConverter(Consumer<Cluster> postConversionProcessor, boolean acceptRelativePaths) {
    this.postConversionProcessor = requireNonNull(postConversionProcessor);
    this.acceptRelativePaths = acceptRelativePaths;
  }

  public void processInput(String clusterName, List<String> stripeNames, Path... tcConfigPaths) {

    TcConfigMapper mapper = new TcConfigMapperDiscovery(getClass().getClassLoader()).find().orElseThrow(() ->
        new AssertionError("No " + TcConfigMapper.class.getName() +
            " service implementation found on classpath"));

    Cluster cluster = mapper.parseConfig(clusterName, stripeNames, tcConfigPaths);
    validateAgainstRelativePath(cluster);

    new ClusterValidator(cluster).validate(ClusterState.ACTIVATED);

    postConversionProcessor.accept(cluster);
  }

  protected void validateAgainstRelativePath(Cluster cluster) {
    HashMap<String, String> perNodeWarnings = new HashMap<>();
    if (!acceptRelativePaths) {
      for (Stripe stripe : cluster.getStripes()) {
        for (Node node : stripe.getNodes()) {
          List<String> placeHolderList = checkPlaceHolders(node);
          if (!placeHolderList.isEmpty()) {
            perNodeWarnings.put(node.getName(), placeHolderList.toString());
          } else {
            String settingName = containsRelativePaths(node);
            if (settingName != null) {
              throw new RuntimeException("The config: " + settingName + " for server: " + node.getName() +
                  " in stripe: " + stripe.toShapeString() + " contains relative paths, which will not work as intended" +
                  " after config conversion. Use absolute paths instead.");
            }
          }
        }
      }
    }
    if (!perNodeWarnings.isEmpty()) {
      LOGGER.warn("{}NOTE: The following nodes were found to have placeholders in paths, which may not work as intended on new hosts after config conversion: {}{}{}",
          lineSeparator(),
          lineSeparator(),
          perNodeWarnings.entrySet().stream()
              .map(e -> " - Node: " + e.getKey() + ". Configs containing placeholders: " + e.getValue())
              .collect(Collectors.joining(lineSeparator())),
          lineSeparator()
      );
    }
  }

  private List<String> checkPlaceHolders(org.terracotta.dynamic_config.api.model.Node node) {
    List<String> placeHolders = new ArrayList<>();
    node.getDataDirs().orDefault().values().stream().map(RawPath::toString).filter(Substitutor::containsSubstitutionParams).findAny().ifPresent(path -> placeHolders.add(SettingName.DATA_DIRS));
    node.getBackupDir().filter(path -> Substitutor.containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SettingName.NODE_BACKUP_DIR));
    node.getLogDir().filter(path -> Substitutor.containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SettingName.NODE_LOG_DIR));
    node.getMetadataDir().filter(path -> Substitutor.containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SettingName.NODE_METADATA_DIR));
    node.getSecurityDir().filter(path -> Substitutor.containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SettingName.SECURITY_DIR));
    node.getSecurityAuditLogDir().filter(path -> Substitutor.containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SettingName.SECURITY_AUDIT_LOG_DIR));

    return placeHolders;
  }

  private String containsRelativePaths(org.terracotta.dynamic_config.api.model.Node node) {
    if (node.getDataDirs().orDefault().values().stream().anyMatch(path -> !path.toPath().isAbsolute())) {
      return SettingName.DATA_DIRS;
    }

    if (node.getBackupDir().isConfigured() && !node.getBackupDir().get().toPath().isAbsolute()) {
      return SettingName.NODE_BACKUP_DIR;
    }

    if (node.getLogDir().isConfigured() && !node.getLogDir().get().toPath().isAbsolute()) {
      return SettingName.NODE_LOG_DIR;
    }

    if (node.getMetadataDir().isConfigured() && !node.getMetadataDir().get().toPath().isAbsolute()) {
      return SettingName.NODE_METADATA_DIR;
    }

    if (node.getSecurityDir().isConfigured() && !node.getSecurityDir().get().toPath().isAbsolute()) {
      return SettingName.SECURITY_DIR;
    }

    if (node.getSecurityAuditLogDir().isConfigured() && !node.getSecurityAuditLogDir().get().toPath().isAbsolute()) {
      return SettingName.SECURITY_AUDIT_LOG_DIR;
    }
    return null;
  }
}