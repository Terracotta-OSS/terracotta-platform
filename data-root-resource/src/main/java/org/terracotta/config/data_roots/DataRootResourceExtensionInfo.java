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
package org.terracotta.config.data_roots;

import com.tc.productinfo.ExtensionInfo;
import org.terracotta.dynamic_config.server.api.ManifestInfo;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;

public class DataRootResourceExtensionInfo implements ExtensionInfo {

  private static final String PLUGIN_NAME = "Data Root Plugin";

  @Override
  public String getExtensionInfo() {
    Map<String, String> attributes = ManifestInfo.getJarManifestInfo(this.getClass());
    return PLUGIN_NAME + ":" + lineSeparator() + Stream.of(ManifestInfo.BASE_ATTRIBUTES)
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
