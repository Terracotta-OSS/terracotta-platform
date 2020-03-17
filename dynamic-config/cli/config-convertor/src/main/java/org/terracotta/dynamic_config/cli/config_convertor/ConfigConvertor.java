/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.dynamic_config.cli.config_convertor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.SettingName;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.cli.config_convertor.xml.TcConfigMapper;
import org.terracotta.dynamic_config.cli.config_convertor.xml.TcConfigMapperDiscovery;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.terracotta.dynamic_config.api.service.IParameterSubstitutor.containsSubstitutionParams;

public class ConfigConvertor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigConvertor.class);


  private final Consumer<Cluster> postConversionProcessor;
  private final boolean acceptRelativePaths;

  public ConfigConvertor(Consumer<Cluster> conversionProcessor) {
    this(conversionProcessor, false);
  }

  public ConfigConvertor(Consumer<Cluster> postConversionProcessor, boolean acceptRelativePaths) {
    this.postConversionProcessor = requireNonNull(postConversionProcessor);
    this.acceptRelativePaths = acceptRelativePaths;
  }

  public void processInput(String clusterName, Path... tcConfigPaths) {

    TcConfigMapper mapper = new TcConfigMapperDiscovery(getClass().getClassLoader()).find().orElseThrow(() ->
        new AssertionError("No " + TcConfigMapper.class.getName() +
            " service implementation found on classpath"));

    Cluster cluster = mapper.parseConfig(clusterName, tcConfigPaths);
    validateAgainstRelativePath(cluster);

    cluster.getNodes().forEach(Node::fillRequiredDefaults);
    new ClusterValidator(cluster).validate();

    postConversionProcessor.accept(cluster);
  }

  protected void validateAgainstRelativePath(Cluster cluster) {
    HashMap<Tuple2<Integer, String>, String> perNodeWarnings = new HashMap<>();
    if (!acceptRelativePaths) {
      cluster.nodeContexts().forEach(nodeContext -> {
        org.terracotta.dynamic_config.api.model.Node node = nodeContext.getNode();
        List<String> placeHolderList = checkPlaceHolders(node);
        if (!placeHolderList.isEmpty()) {
          perNodeWarnings.put(Tuple2.tuple2(nodeContext.getStripeId(), nodeContext.getNodeName()), placeHolderList.toString());
        } else {
          String settingName = containsRelativePaths(node);
          if (settingName != null) {
            throw new RuntimeException("The config: " + settingName + " for server: " + node.getNodeName() +
                " in stripe: " + nodeContext.getStripeId() + " contains relative paths, which will not work as intended" +
                " after config conversion. Use absolute paths instead.");
          }
        }
      });
    }
    if (!perNodeWarnings.isEmpty()) {
      LOGGER.warn("{}WARNING:{}The following nodes were found to have placeholders in paths, which may not work as intended on new hosts after config conversion: {}{}{}",
          lineSeparator(),
          lineSeparator(),
          lineSeparator(),
          perNodeWarnings.entrySet().stream()
              .map(e -> " - Server: " + e.getKey().getT2() + " in stripe: " + e.getKey().getT1() + ". Configs containing placeholders: " + e.getValue())
              .collect(Collectors.joining(lineSeparator())),
          lineSeparator()
      );
    }
  }

  private List<String> checkPlaceHolders(org.terracotta.dynamic_config.api.model.Node node) {
    List<String> placeHolders = new ArrayList<>();
    node.getDataDirs().values().stream().map(Path::toString).filter(IParameterSubstitutor::containsSubstitutionParams).findAny().ifPresent(path -> placeHolders.add(SettingName.DATA_DIRS));
    ofNullable(node.getNodeBackupDir()).filter(path -> containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SettingName.NODE_BACKUP_DIR));
    ofNullable(node.getNodeLogDir()).filter(path -> containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SettingName.NODE_LOG_DIR));
    ofNullable(node.getNodeMetadataDir()).filter(path -> containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SettingName.NODE_METADATA_DIR));
    ofNullable(node.getSecurityDir()).filter(path -> containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SettingName.SECURITY_DIR));
    ofNullable(node.getSecurityAuditLogDir()).filter(path -> containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SettingName.SECURITY_AUDIT_LOG_DIR));

    return placeHolders;
  }

  private String containsRelativePaths(org.terracotta.dynamic_config.api.model.Node node) {
    if (node.getDataDirs().values().stream().anyMatch(path -> !path.isAbsolute())) {
      return SettingName.DATA_DIRS;
    }

    if (node.getNodeBackupDir() != null && !node.getNodeBackupDir().isAbsolute()) {
      return SettingName.NODE_BACKUP_DIR;
    }

    if (node.getNodeLogDir() != null && !node.getNodeLogDir().isAbsolute()) {
      return SettingName.NODE_LOG_DIR;
    }

    if (node.getNodeMetadataDir() != null && !node.getNodeMetadataDir().isAbsolute()) {
      return SettingName.NODE_METADATA_DIR;
    }

    if (node.getSecurityDir() != null && !node.getSecurityDir().isAbsolute()) {
      return SettingName.SECURITY_DIR;
    }

    if (node.getSecurityAuditLogDir() != null && !node.getSecurityAuditLogDir().isAbsolute()) {
      return SettingName.SECURITY_AUDIT_LOG_DIR;
    }
    return null;
  }
}