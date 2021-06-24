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
package org.terracotta.platform;

import com.tc.productinfo.ExtensionInfo;

import java.util.Map;

import static org.terracotta.platform.ManifestInfo.BUILD_JDK;
import static org.terracotta.platform.ManifestInfo.BUILD_TIMESTAMP;
import static org.terracotta.platform.ManifestInfo.VERSION;
import static org.terracotta.platform.ManifestInfo.getJarManifestInfo;

public class PlatformBaseExtensionInfo implements ExtensionInfo {

  private static final String PLUGIN_NAME = "Service: Server Information";

  @Override
  public String getExtensionInfo() {
    return getValue(DESCRIPTION);
  }

  @Override
  public String getValue(String name) {
    switch (name) {
      case "type":
        return "SERVICE";
      case DESCRIPTION:
        Map<String, String> attributes = getJarManifestInfo(this.getClass());
        return String.format(" * %-35s %-15s (built on %s with JDK %s)", PLUGIN_NAME, attributes.get(VERSION), attributes.get(BUILD_TIMESTAMP), attributes.get(BUILD_JDK));
      case NAME:
        return PLUGIN_NAME;
      default:
        return getJarManifestInfo(this.getClass()).getOrDefault(name, "");
    }
  }
}
