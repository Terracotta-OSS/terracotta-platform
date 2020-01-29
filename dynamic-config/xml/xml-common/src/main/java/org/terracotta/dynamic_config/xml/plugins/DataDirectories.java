/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml.plugins;

import org.terracotta.data.config.DataRootMapping;
import org.terracotta.data.config.ObjectFactory;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.xml.Utils;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import java.nio.file.Path;
import java.util.Map;

public class DataDirectories {
  private static final ObjectFactory FACTORY = new ObjectFactory();
  static final String META_DATA_ROOT_NAME = "meta-data-root";

  private final Map<String, Path> dataDirs;
  private final Path nodeMetadataDir;
  private final PathResolver pathResolver;

  public DataDirectories(Map<String, Path> dataDirs, Path nodeMetadataDir, PathResolver pathResolver) {
    this.dataDirs = dataDirs;
    this.nodeMetadataDir = nodeMetadataDir;
    this.pathResolver = pathResolver;
  }

  public Element toElement() {
    org.terracotta.data.config.DataDirectories dataDirectories = createDataDirectories();
    JAXBElement<org.terracotta.data.config.DataDirectories> jaxbElement = FACTORY.createDataDirectories(dataDirectories);
    return Utils.createElement(jaxbElement);
  }

  org.terracotta.data.config.DataDirectories createDataDirectories() {
    try {
      org.terracotta.data.config.DataDirectories dataDirectories = FACTORY.createDataDirectories();
      boolean metadataRootFound = false;
      if (dataDirs != null) {
        for (Map.Entry<String, Path> entry : dataDirs.entrySet()) {
          boolean isPlatformRoot = false;
          if (nodeMetadataDir != null &&
              !metadataRootFound &&
              nodeMetadataDir.toFile().getCanonicalPath().equals(entry.getValue().toFile().getCanonicalPath())) {
            metadataRootFound = true;
            isPlatformRoot = true;
          }
          dataDirectories.getDirectory().add(createDataRootMapping(entry.getKey(), pathResolver.resolve(entry.getValue()).toString(), isPlatformRoot));
        }
      }

      if (nodeMetadataDir != null && !metadataRootFound) {
        dataDirectories.getDirectory()
            .add(createDataRootMapping(META_DATA_ROOT_NAME, pathResolver.resolve(nodeMetadataDir).toString(), true));
      }

      return dataDirectories;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static DataRootMapping createDataRootMapping(String key, String value, boolean isPlatformRoot) {
    DataRootMapping dataRootMapping = FACTORY.createDataRootMapping();

    dataRootMapping.setName(key);
    dataRootMapping.setValue(value);
    dataRootMapping.setUseForPlatform(isPlatformRoot);

    return dataRootMapping;
  }
}
