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

import static org.terracotta.dynamic_config.api.model.SettingName.AUTO_ACTIVATE;
import static org.terracotta.dynamic_config.api.model.SettingName.CLIENT_LEASE_DURATION;
import static org.terracotta.dynamic_config.api.model.SettingName.CLIENT_RECONNECT_WINDOW;
import static org.terracotta.dynamic_config.api.model.SettingName.CLUSTER_NAME;
import static org.terracotta.dynamic_config.api.model.SettingName.CONFIG_FILE;
import static org.terracotta.dynamic_config.api.model.SettingName.DATA_DIRS;
import static org.terracotta.dynamic_config.api.model.SettingName.FAILOVER_PRIORITY;
import static org.terracotta.dynamic_config.api.model.SettingName.LICENSE_FILE;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_CONFIG_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_GROUP_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_METADATA_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_NAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_PUBLIC_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_PUBLIC_PORT;
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
  @DeprecatedParameter(names = {"-s", "--" + NODE_HOSTNAME}, description = "node host name")
  @Parameter(names = "-" + NODE_HOSTNAME, description = "node host name")
  private String nodeHostname;

  @DeprecatedParameter(names = {"-S", "--" + NODE_PUBLIC_HOSTNAME}, description = "public node host name")
  @Parameter(names = "-" + NODE_PUBLIC_HOSTNAME, description = "public node host name")
  private String nodePublicHostname;

  @DeprecatedParameter(names = {"-p", "--" + NODE_PORT}, description = "node port")
  @Parameter(names = "-" + NODE_PORT, description = "node port")
  private String nodePort;

  @DeprecatedParameter(names = {"-P", "--" + NODE_PUBLIC_PORT}, description = "public node port")
  @Parameter(names = "-" + NODE_PUBLIC_PORT, description = "public node port")
  private String nodePublicPort;

  @DeprecatedParameter(names = {"-g", "--" + NODE_GROUP_PORT}, description = "node port used for intra-stripe communication")
  @Parameter(names = "-" + NODE_GROUP_PORT, description = "node port used for intra-stripe communication")
  private String nodeGroupPort;

  @DeprecatedParameter(names = {"-n", "--" + NODE_NAME}, description = "node name")
  @Parameter(names = "-" + NODE_NAME, description = "node name")
  private String nodeName;

  @DeprecatedParameter(names = {"-a", "--" + NODE_BIND_ADDRESS}, description = "node bind address for port")
  @Parameter(names =  "-" + NODE_BIND_ADDRESS, description = "node bind address for port")
  private String nodeBindAddress;

  @DeprecatedParameter(names = {"-A", "--" + NODE_GROUP_BIND_ADDRESS}, description = "node bind address for group port")
  @Parameter(names = "-" + NODE_GROUP_BIND_ADDRESS, description = "node bind address for group port")
  private String nodeGroupBindAddress;

  @DeprecatedParameter(names = {"-r", "--" + NODE_CONFIG_DIR}, description = "node configuration directory")
  @Parameter(names = "-" + NODE_CONFIG_DIR, description = "node configuration directory")
  private String nodeConfigDir;

  @DeprecatedParameter(names = {"-m", "--" + NODE_METADATA_DIR}, description = "node metadata directory")
  @Parameter(names = "-" + NODE_METADATA_DIR, description = "node metadata directory")
  private String nodeMetadataDir;

  @DeprecatedParameter(names = {"-L", "--" + NODE_LOG_DIR}, description = "node log directory")
  @Parameter(names = "-" + NODE_LOG_DIR, description = "node log directory")
  private String nodeLogDir;

  @DeprecatedParameter(names = {"-b", "--" + NODE_BACKUP_DIR}, description = "node backup directory")
  @Parameter(names = "-" + NODE_BACKUP_DIR, description = "node backup directory")
  private String nodeBackupDir;

  @DeprecatedParameter(names = {"-x", "--" + SECURITY_DIR}, description = "security root directory")
  @Parameter(names = "-" + SECURITY_DIR, description = "security root directory")
  private String securityDir;

  @DeprecatedParameter(names = {"-u", "--" + SECURITY_AUDIT_LOG_DIR}, description = "security audit log directory")
  @Parameter(names = "-" + SECURITY_AUDIT_LOG_DIR, description = "security audit log directory")
  private String securityAuditLogDir;

  @DeprecatedParameter(names = {"-z", "--" + SECURITY_AUTHC}, description = "security authentication setting (file|ldap|certificate)")
  @Parameter(names = "-" + SECURITY_AUTHC, description = "security authentication setting (file|ldap|certificate)")
  private String securityAuthc;

  @DeprecatedParameter(names = {"-t", "--" + SECURITY_SSL_TLS}, description = "ssl-tls setting (true|false)")
  @Parameter(names = "-" + SECURITY_SSL_TLS, description = "ssl-tls setting (true|false)")
  private String securitySslTls;

  @DeprecatedParameter(names = {"-w", "--" + SECURITY_WHITELIST}, description = "security whitelist (true|false)")
  @Parameter(names = "-" + SECURITY_WHITELIST, description = "security whitelist (true|false)")
  private String securityWhitelist;

  @DeprecatedParameter(names = {"-y", "--" + FAILOVER_PRIORITY}, description = "failover priority setting (availability|consistency)")
  @Parameter(names = "-" + FAILOVER_PRIORITY, description = "failover priority setting (availability|consistency)")
  private String failoverPriority;

  @DeprecatedParameter(names = {"-R", "--" + CLIENT_RECONNECT_WINDOW}, description = "client reconnect window")
  @Parameter(names = "-" + CLIENT_RECONNECT_WINDOW, description = "client reconnect window")
  private String clientReconnectWindow;

  @DeprecatedParameter(names = {"-i", "--" + CLIENT_LEASE_DURATION}, description = "client lease duration")
  @Parameter(names = "-" + CLIENT_LEASE_DURATION, description = "client lease duration")
  private String clientLeaseDuration;

  @DeprecatedParameter(names = {"-o", "--" + OFFHEAP_RESOURCES}, description = "offheap resources")
  @Parameter(names = "-" + OFFHEAP_RESOURCES, description = "offheap resources")
  private String offheapResources;

  @DeprecatedParameter(names = {"-d", "--" + DATA_DIRS}, description = "data directory")
  @Parameter(names = "-" + DATA_DIRS, description = "data directory")
  private String dataDirs;

  @DeprecatedParameter(names = {"-f", "--" + CONFIG_FILE}, description = "configuration properties file")
  @Parameter(names = "-" + CONFIG_FILE, description = "configuration properties file")
  private String configFile;

  @DeprecatedParameter(names = {"-T", "--" + TC_PROPERTIES}, description = "tc-properties")
  @Parameter(names = "-" + TC_PROPERTIES, description = "tc-properties")
  private String tcProperties;

  @DeprecatedParameter(names = {"-N", "--" + CLUSTER_NAME}, description = "cluster name")
  @Parameter(names = "-" + CLUSTER_NAME, description = "cluster name")
  private String clusterName;

  @Parameter(names = "-" + LICENSE_FILE, hidden = true)
  private String licenseFile;

  @DeprecatedParameter(names = {"-D", "--" + REPAIR_MODE}, description = "node repair mode (true|false)")
  @Parameter(names = "-" + REPAIR_MODE, description = "node repair mode (true|false)")
  private boolean wantsRepairMode;

  // so that we can start a pre-activated stripe directly in dev / test.
  // no need to have a short option for this one, this is not public.
  @Parameter(names = {"-" + AUTO_ACTIVATE}, hidden = true)
  private boolean allowsAutoActivation;

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
              && !longestName.equals(addDashDash(AUTO_ACTIVATE))
              && !longestName.equals(addDashDash(NODE_CONFIG_DIR));
        })
        .forEach(pd -> paramValueMap.put(Setting.fromName(ConsoleParamsUtils.stripDashDash(pd.getLongestName())), pd.getParameterized().get(this).toString()));
  }

  private void validateOptions(CustomJCommander jCommander) {
    if (configFile != null) {
      if (nodeName != null && (nodePort != null || nodeHostname != null)) {
        throw new ParameterException("'" + addDashDash(NODE_NAME) + "' parameter cannot be used with '"
            + addDashDash(NODE_HOSTNAME) + "' or '" + addDashDash(NODE_PORT) + "' parameter");
      }

      Set<String> filteredOptions = new HashSet<>(jCommander.getUserSpecifiedOptions());
      filteredOptions.remove(addDashDash(AUTO_ACTIVATE));
      filteredOptions.remove(addDashDash(REPAIR_MODE));
      filteredOptions.remove("-D");

      filteredOptions.remove(addDashDash(CONFIG_FILE));
      filteredOptions.remove("-f");

      filteredOptions.remove(addDashDash(LICENSE_FILE));
      filteredOptions.remove("-l");

      filteredOptions.remove(addDashDash(NODE_HOSTNAME));
      filteredOptions.remove("-s");

      filteredOptions.remove(addDashDash(NODE_PORT));
      filteredOptions.remove("-p");

      filteredOptions.remove(addDashDash(NODE_NAME));
      filteredOptions.remove("-n");

      filteredOptions.remove(addDashDash(NODE_CONFIG_DIR));
      filteredOptions.remove("-r");

      if (filteredOptions.size() != 0) {
        throw new ParameterException(
            String.format(
                "'%s' parameter can only be used with '%s', '%s', '%s', '%s' and '%s' parameters",
                addDashDash(CONFIG_FILE),
                addDashDash(REPAIR_MODE),
                addDashDash(NODE_NAME),
                addDashDash(NODE_HOSTNAME),
                addDashDash(NODE_PORT),
                addDashDash(NODE_CONFIG_DIR)
            )
        );
      }
    } else {
      // when using CLI parameters
      if (licenseFile != null) {
        if (clusterName == null) {
          throw new ParameterException("'" + addDashDash(LICENSE_FILE) + "' parameter must be used with '" + addDashDash(CLUSTER_NAME) + "' parameter");
        }

        if (!allowsAutoActivation) {
          throw new ParameterException("'" + addDashDash(LICENSE_FILE) + "' parameter must be used with '" + addDashDash(AUTO_ACTIVATE) + "' parameter");
        }
      }
    }
  }

  public String getFailoverPriority() {
    return failoverPriority;
  }

  public String getNodeHostname() {
    return nodeHostname;
  }

  public String getNodePort() {
    return nodePort;
  }

  public String getNodeName() {
    return nodeName;
  }

  public String getNodeConfigDir() {
    return nodeConfigDir;
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

  public boolean allowsAutoActivation() {
    return allowsAutoActivation;
  }
}
