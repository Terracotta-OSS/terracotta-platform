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
