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
package org.terracotta.dynamic_config.server.nomad.persistence;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public interface ClusterConfigFilename {

  String getNodeName();

  long getVersion();

  default String getFilename() {
    return getNodeName() + "." + getVersion() + ".xml";
  }

  static ClusterConfigFilename with(String nodeName, long version) {
    requireNonNull(nodeName);
    if (version <= 0) {
      throw new IllegalArgumentException("Bad version: " + version);
    }
    return new ClusterConfigFilename() {
      @Override
      public String getNodeName() {
        return nodeName;
      }

      @Override
      public long getVersion() {
        return version;
      }
    };
  }

  static Optional<ClusterConfigFilename> from(String fileName) {
    String nodeName = null;
    long version = 0;
    if (fileName.endsWith(".xml")) {
      int pos_xml = fileName.lastIndexOf(".xml");
      if (pos_xml >= 3) { // 3 because shortest filename is "a.1.xml"
        String base = fileName.substring(0, pos_xml); // a.1
        int pos_digits = base.lastIndexOf('.');
        if (pos_digits >= 1) { // 0 is invalid, means no node name...
          nodeName = base.substring(0, pos_digits).trim();
          if (nodeName.isEmpty()) {
            return Optional.empty();
          }
          try {
            version = Long.parseLong(base.substring(pos_digits + 1));
          } catch (NumberFormatException e) {
            return Optional.empty();
          }
        }
      }
    }
    if (nodeName == null || version == 0) {
      return Optional.empty();
    }
    final String n = nodeName;
    final long v = version;
    return Optional.of(new ClusterConfigFilename() {
      @Override
      public String getNodeName() {
        return n;
      }

      @Override
      public long getVersion() {
        return v;
      }

      @Override
      public String getFilename() {
        return fileName;
      }
    });
  }
}
