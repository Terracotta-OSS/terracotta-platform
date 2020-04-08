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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.api.model.Setting;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.terracotta.dynamic_config.api.model.SettingName.CLIENT_LEASE_DURATION;
import static org.terracotta.dynamic_config.api.model.SettingName.CLIENT_RECONNECT_WINDOW;
import static org.terracotta.dynamic_config.api.model.SettingName.CLUSTER_NAME;
import static org.terracotta.dynamic_config.api.model.SettingName.CONFIG_FILE;
import static org.terracotta.dynamic_config.api.model.SettingName.DATA_DIRS;
import static org.terracotta.dynamic_config.api.model.SettingName.FAILOVER_PRIORITY;
import static org.terracotta.dynamic_config.api.model.SettingName.LICENSE_FILE;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_GROUP_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_METADATA_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_NAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_PUBLIC_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_PUBLIC_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_REPOSITORY_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.SettingName.REPAIR_MODE;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_AUDIT_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_AUTHC;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_SSL_TLS;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_WHITELIST;
import static org.terracotta.dynamic_config.api.model.SettingName.TC_PROPERTIES;
import static org.terracotta.dynamic_config.server.configuration.startup.ConsoleParamsUtils.addDashDash;

@Parameters(separators = "=")
public class Options {
  @Parameter(names = {"-s", "--" + NODE_HOSTNAME})
  private String nodeHostname;

  @Parameter(names = {"-S", "--" + NODE_PUBLIC_HOSTNAME})
  private String nodePublicHostname;

  @Parameter(names = {"-p", "--" + NODE_PORT})
  private String nodePort;

  @Parameter(names = {"-P", "--" + NODE_PUBLIC_PORT})
  private String nodePublicPort;

  @Parameter(names = {"-g", "--" + NODE_GROUP_PORT})
  private String nodeGroupPort;

  @Parameter(names = {"-n", "--" + NODE_NAME})
  private String nodeName;

  @Parameter(names = {"-a", "--" + NODE_BIND_ADDRESS})
  private String nodeBindAddress;

  @Parameter(names = {"-A", "--" + NODE_GROUP_BIND_ADDRESS})
  private String nodeGroupBindAddress;

  @Parameter(names = {"-r", "--" + NODE_REPOSITORY_DIR})
  private String nodeRepositoryDir;

  @Parameter(names = {"-m", "--" + NODE_METADATA_DIR})
  private String nodeMetadataDir;

  @Parameter(names = {"-L", "--" + NODE_LOG_DIR})
  private String nodeLogDir;

  @Parameter(names = {"-b", "--" + NODE_BACKUP_DIR})
  private String nodeBackupDir;

  @Parameter(names = {"-x", "--" + SECURITY_DIR})
  private String securityDir;

  @Parameter(names = {"-u", "--" + SECURITY_AUDIT_LOG_DIR})
  private String securityAuditLogDir;

  @Parameter(names = {"-z", "--" + SECURITY_AUTHC})
  private String securityAuthc;

  @Parameter(names = {"-t", "--" + SECURITY_SSL_TLS})
  private String securitySslTls;

  @Parameter(names = {"-w", "--" + SECURITY_WHITELIST})
  private String securityWhitelist;

  @Parameter(names = {"-y", "--" + FAILOVER_PRIORITY})
  private String failoverPriority;

  @Parameter(names = {"-R", "--" + CLIENT_RECONNECT_WINDOW})
  private String clientReconnectWindow;

  @Parameter(names = {"-i", "--" + CLIENT_LEASE_DURATION})
  private String clientLeaseDuration;

  @Parameter(names = {"-o", "--" + OFFHEAP_RESOURCES})
  private String offheapResources;

  @Parameter(names = {"-d", "--" + DATA_DIRS})
  private String dataDirs;

  @Parameter(names = {"-f", "--" + CONFIG_FILE})
  private String configFile;

  @Parameter(names = {"-T", "--" + TC_PROPERTIES})
  private String tcProperties;

  @Parameter(names = {"-N", "--" + CLUSTER_NAME})
  private String clusterName;

  @Parameter(names = {"-l", "--" + LICENSE_FILE})
  private String licenseFile;

  @Parameter(names = {"-D", "--" + REPAIR_MODE})
  private boolean wantsRepairMode;

  private final Map<Setting, String> paramValueMap = new HashMap<>();

  public Map<Setting, String> getTopologyOptions() {
    return paramValueMap;
  }

  public void process(CustomJCommander jCommander) {
    validateOptions(jCommander);
    extractTopologyOptions(jCommander);
  }

  /**
   * Constructs a {@code Map} containing only the parameters relevant to {@code Node} object with longest parameter name
   * as the key and user-specified-value as the value.
   *
   * @param jCommander jCommander instance
   * @return the constructed map
   */
  private void extractTopologyOptions(CustomJCommander jCommander) {
    Collection<String> userSpecifiedOptions = jCommander.getUserSpecifiedOptions();
    Predicate<ParameterDescription> isSpecified =
        pd -> Arrays.stream(pd.getNames().split(","))
            .map(String::trim)
            .anyMatch(userSpecifiedOptions::contains);

    jCommander.getParameters()
        .stream()
        .filter(isSpecified)
        .filter(pd -> {
          String longestName = pd.getLongestName();
          return !longestName.equals(addDashDash(LICENSE_FILE))
              && !longestName.equals(addDashDash(CONFIG_FILE))
              && !longestName.equals(addDashDash(REPAIR_MODE))
              && !longestName.equals(addDashDash(NODE_REPOSITORY_DIR));
        })
        .forEach(pd -> paramValueMap.put(Setting.fromName(ConsoleParamsUtils.stripDashDash(pd.getLongestName())), pd.getParameterized().get(this).toString()));
  }

  private void validateOptions(CustomJCommander jCommander) {
    if (configFile == null) {
      // when using CLI parameters

      if (licenseFile != null && clusterName == null) {
        throw new ParameterException("'" + addDashDash(LICENSE_FILE) + "' parameter must be used with '" + addDashDash(CLUSTER_NAME) + "' parameter");
      }

    } else {
      // when using config file

      Set<String> filteredOptions = new HashSet<>(jCommander.getUserSpecifiedOptions());
      filteredOptions.remove("-f");
      filteredOptions.remove("-l");
      filteredOptions.remove("-s");
      filteredOptions.remove("-p");
      filteredOptions.remove("-r");

      filteredOptions.remove(addDashDash(REPAIR_MODE));
      filteredOptions.remove(addDashDash(CONFIG_FILE));
      filteredOptions.remove(addDashDash(LICENSE_FILE));
      filteredOptions.remove(addDashDash(NODE_HOSTNAME));
      filteredOptions.remove(addDashDash(NODE_PORT));
      filteredOptions.remove(addDashDash(NODE_REPOSITORY_DIR));

      if (filteredOptions.size() != 0) {
        throw new ParameterException(
            String.format(
                "'%s' parameter can only be used with '%s', '%s', '%s', '%s' and '%s' parameters",
                addDashDash(CONFIG_FILE),
                addDashDash(REPAIR_MODE),
                addDashDash(LICENSE_FILE),
                addDashDash(NODE_HOSTNAME),
                addDashDash(NODE_PORT),
                addDashDash(NODE_REPOSITORY_DIR)
            )
        );
      }
    }
  }

  public String getNodeHostname() {
    return nodeHostname;
  }

  public String getNodePort() {
    return nodePort;
  }

  public String getNodeRepositoryDir() {
    return nodeRepositoryDir;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getConfigFile() {
    return configFile;
  }

  public String getLicenseFile() {
    return licenseFile;
  }

  public boolean wantsRepairMode() {
    return wantsRepairMode;
  }
}
