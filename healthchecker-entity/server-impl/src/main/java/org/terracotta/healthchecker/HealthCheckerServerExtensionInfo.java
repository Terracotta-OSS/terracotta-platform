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
package org.terracotta.healthchecker;

import com.tc.productinfo.ExtensionInfo;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;

public class HealthCheckerServerExtensionInfo implements ExtensionInfo {

  private static final String PLUGIN_NAME = "Health Checker Server Plugin";
  private static final String[] BASE_ATTRIBUTES = {"Bundle-Version", "BuildInfo-Timestamp", "Build-Jdk"};
  private Map<String, String> getJarManifestInfo() {
    try {
      File file = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
      if (file.isDirectory()) {
        return Collections.emptyMap();
      }
      try (JarFile jar = new JarFile(file)) {
        return jar.getManifest().getMainAttributes().entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
      }
    } catch (IOException | URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String getExtensionInfo() {
    Map<String, String> attributes = getJarManifestInfo();
    return PLUGIN_NAME + ":" + lineSeparator() + Stream.of(BASE_ATTRIBUTES)
        .filter(attributes::containsKey)
        .map(n -> n + ": " + attributes.get(n))
        .collect(Collectors.joining(lineSeparator())) + lineSeparator();
  }

  @Override
  public String getValue(String name) {
    if (name.equals(DESCRIPTION)) {
      return getExtensionInfo();
    } else {
      return "";
    }
  }
}
