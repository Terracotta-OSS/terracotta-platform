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
package org.terracotta.dynamic_config.server.configuration.startup;

import org.terracotta.dynamic_config.api.model.Setting;

import java.util.HashMap;
import java.util.Map;

public class Options {

  private String configDir;
  private String configFile;
  private String licenseFile;
  private boolean wantsRepairMode;
  private boolean allowsAutoActivation;

  private final Map<Setting, String> paramValueMap = new HashMap<>();

  public Options(Map<Setting, String> paramValueMap) {
    this.paramValueMap.putAll(paramValueMap);
  }

  public Map<Setting, String> getTopologyOptions() {
    return paramValueMap;
  }

  public String getFailoverPriority() {
    return paramValueMap.get(Setting.FAILOVER_PRIORITY);
  }

  public String getHostname() {
    return paramValueMap.get(Setting.NODE_HOSTNAME);
  }

  public String getPort() {
    return paramValueMap.get(Setting.NODE_PORT);
  }

  public String getNodeName() {
    return paramValueMap.get(Setting.NODE_NAME);
  }

  public String getConfigDir() {
    return configDir;
  }

  public void setConfigDir(String configDir) {
    this.configDir = configDir;
  }

  public String getClusterName() {
    return paramValueMap.get(Setting.CLUSTER_NAME);
  }

  public void setConfigFile(String configFile) {
    this.configFile = configFile;
  }

  public String getConfigFile() {
    return configFile;
  }

  public void setLicenseFile(String licenseFile) {
    this.licenseFile = licenseFile;
  }

  public String getLicenseFile() {
    return licenseFile;
  }

  public void setWantsRepairMode(boolean wantsRepairMode) {
    this.wantsRepairMode = wantsRepairMode;
  }

  public boolean wantsRepairMode() {
    return wantsRepairMode;
  }

  public void setAllowsAutoActivation(boolean allowsAutoActivation) {
    this.allowsAutoActivation = allowsAutoActivation;
  }

  public boolean allowsAutoActivation() {
    return allowsAutoActivation;
  }
}
