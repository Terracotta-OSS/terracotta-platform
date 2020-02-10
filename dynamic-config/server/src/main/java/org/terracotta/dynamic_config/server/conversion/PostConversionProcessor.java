/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.SettingName;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.api.service.XmlConfigMapper;
import org.terracotta.dynamic_config.api.service.XmlConfigMapperDiscovery;
import org.terracotta.dynamic_config.server.conversion.xml.XmlUtility;
import org.w3c.dom.Node;

import javax.xml.transform.TransformerException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static java.util.Optional.ofNullable;
import static org.terracotta.dynamic_config.api.model.SettingName.DATA_DIRS;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_METADATA_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_AUDIT_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_DIR;
import static org.terracotta.dynamic_config.api.service.IParameterSubstitutor.containsSubstitutionParams;

public abstract class PostConversionProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigPropertiesProcessor.class);
  private final XmlConfigMapper xmlConfigMapper;

  abstract void process(Map<Tuple2<Integer, String>, Node> nodeNameNodeConfigMap, boolean acceptRelativePaths);

  public PostConversionProcessor() {
    this.xmlConfigMapper = new XmlConfigMapperDiscovery(PathResolver.NOOP)
        .find()
        .orElseThrow(() -> new AssertionError("No " + XmlConfigMapper.class.getName() + " service implementation found on classpath"));
  }

  protected ArrayList<NodeContext> validate(Map<Tuple2<Integer, String>, Node> nodeNameNodeConfigMap, boolean acceptRelativePaths) {
    HashMap<Tuple2<Integer, String>, String> perNodeWarnings = new HashMap<>();
    ArrayList<NodeContext> nodeContexts = new ArrayList<>();
    for (Map.Entry<Tuple2<Integer, String>, Node> entry : nodeNameNodeConfigMap.entrySet()) {
      Tuple2<Integer, String> stripeIdServerName = entry.getKey();
      Node doc = entry.getValue();
      try {
        // create the XML from the manipulated DOM elements
        String xml = XmlUtility.getPrettyPrintableXmlString(doc);
        // convert back the XML to a topology model
        NodeContext nodeContext = xmlConfigMapper.fromXml(stripeIdServerName.t2, xml);
        nodeContexts.add(nodeContext);
        if (!acceptRelativePaths) {
          org.terracotta.dynamic_config.api.model.Node node = nodeContext.getNode();
          List<String> placeHolderList = checkPlaceHolders(node);
          if (!placeHolderList.isEmpty()) {
            perNodeWarnings.put(stripeIdServerName, placeHolderList.toString());
          } else {
            String settingName = containsRelativePaths(node);
            if (settingName != null) {
              throw new RuntimeException("The config: " + settingName + " for server: " + stripeIdServerName.getT2() +
                  " in stripe: " + stripeIdServerName.getT1() + " contains relative paths, which will not work as intended" +
                  " after config conversion. Use absolute paths instead.");
            }
          }
        }
      } catch (TransformerException e) {
        throw new RuntimeException(e);
      }
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
    return nodeContexts;
  }

  private List<String> checkPlaceHolders(org.terracotta.dynamic_config.api.model.Node node) {
    List<String> placeHolders = new ArrayList<>();
    node.getDataDirs().values().stream().map(Path::toString).filter(IParameterSubstitutor::containsSubstitutionParams).findAny().ifPresent(path -> placeHolders.add(DATA_DIRS));
    ofNullable(node.getNodeBackupDir()).filter(path -> containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(NODE_BACKUP_DIR));
    ofNullable(node.getNodeLogDir()).filter(path -> containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(NODE_LOG_DIR));
    ofNullable(node.getNodeMetadataDir()).filter(path -> containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(NODE_METADATA_DIR));
    ofNullable(node.getSecurityDir()).filter(path -> containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SECURITY_DIR));
    ofNullable(node.getSecurityAuditLogDir()).filter(path -> containsSubstitutionParams(path.toString())).ifPresent(path -> placeHolders.add(SECURITY_AUDIT_LOG_DIR));

    return placeHolders;
  }

  private String containsRelativePaths(org.terracotta.dynamic_config.api.model.Node node) {
    if (node.getDataDirs().values().stream().anyMatch(path -> !path.isAbsolute())) {
      return SettingName.DATA_DIRS;
    }

    if (node.getNodeBackupDir() != null && !node.getNodeBackupDir().isAbsolute()) {
      return NODE_BACKUP_DIR;
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
