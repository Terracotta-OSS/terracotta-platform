/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.server.configuration.startup.parsing;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.server.configuration.startup.ConsoleParamsUtils;
import org.terracotta.dynamic_config.server.configuration.startup.CustomJCommander;
import org.terracotta.dynamic_config.server.configuration.startup.Options;

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
import static org.terracotta.dynamic_config.api.model.SettingName.HELP;
import static org.terracotta.dynamic_config.api.model.SettingName.LICENSE_FILE;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_CONFIG_DIR;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_GROUP_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_HOME_DIR;
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
import static org.terracotta.dynamic_config.api.model.SettingName.STRIPE_NAME;
import static org.terracotta.dynamic_config.api.model.SettingName.TC_PROPERTIES;
import static org.terracotta.dynamic_config.server.configuration.startup.ConsoleParamsUtils.addDash;

@Parameters(separators = "=")
public class OptionsParsingImpl implements OptionsParsing {

  @Parameter(names = {"-" + NODE_HOSTNAME}, description = "Node host name. Default: %h")
  private String hostname;

  @Parameter(names = {"-" + NODE_PUBLIC_HOSTNAME}, description = "Public node host name. Default: <unset>")
  private String publicHostname;

  @Parameter(names = {"-" + NODE_PORT}, description = "Node port. Default: 9410")
  private String port;

  @Parameter(names = {"-" + NODE_PUBLIC_PORT}, description = "Public node port. Default: <unset>")
  private String publicPort;

  @Parameter(names = {"-" + NODE_GROUP_PORT}, description = "Node port used for intra-stripe communication. Default: 9430")
  private String groupPort;

  @Parameter(names = {"-" + NODE_NAME}, description = "Node name. Default: <generated>")
  private String nodeName;

  @Parameter(names = {"-" + STRIPE_NAME}, description = "Stripe name. Default: <generated>")
  private String stripeName;

  @Parameter(names = {"-" + NODE_BIND_ADDRESS}, description = "Node bind address for node port. Default: 0.0.0.0")
  private String bindAddress;

  @Parameter(names = {"-" + NODE_GROUP_BIND_ADDRESS}, description = "Node bind address for group port. Default: 0.0.0.0")
  private String groupBindAddress;

  @Parameter(names = {"-" + NODE_CONFIG_DIR}, description = "Node configuration directory. Default: %H/terracotta/config")
  private String configDir;

  @Parameter(names = {"-" + NODE_METADATA_DIR}, description = "Node metadata directory. Default: %H/terracotta/metadata")
  private String metadataDir;

  @Parameter(names = {"-" + NODE_LOG_DIR}, description = "Node log directory. Default: %H/terracotta/logs")
  private String logDir;

  @Parameter(names = {"-" + NODE_BACKUP_DIR}, description = "Node backup directory. Default: <unset>")
  private String backupDir;

  @Parameter(names = {"-" + SECURITY_DIR}, description = "Security root directory. Default: <unset>")
  private String securityDir;

  @Parameter(names = {"-" + SECURITY_AUDIT_LOG_DIR}, description = "Security audit log directory. Default: <unset>")
  private String securityAuditLogDir;

  @Parameter(names = {"-" + SECURITY_AUTHC}, description = "Security authentication setting (file|ldap|certificate). Default: <unset>")
  private String securityAuthc;

  @Parameter(names = {"-" + SECURITY_SSL_TLS}, description = "SSL/TLS setting (true|false). Default: false")
  private String securitySslTls;

  @Parameter(names = {"-" + SECURITY_WHITELIST}, description = "Security whitelist (true|false). Default: false")
  private String securityWhitelist;

  @Parameter(names = {"-" + FAILOVER_PRIORITY}, description = "Failover priority setting (availability|consistency), required with more than 1 node. Default: <unset>")
  private String failoverPriority;

  @Parameter(names = {"-" + CLIENT_RECONNECT_WINDOW}, description = "Client reconnect window. Default: 120s")
  private String clientReconnectWindow;

  @Parameter(names = {"-" + CLIENT_LEASE_DURATION}, description = "Client lease duration. Default: 150s")
  private String clientLeaseDuration;

  @Parameter(names = {"-" + OFFHEAP_RESOURCES}, description = "Off-heap resources. Default: main:512MB")
  private String offheapResources;

  @Parameter(names = {"-" + DATA_DIRS}, description = "Data directories. Default: main:%H/terracotta/user-data/main")
  private String dataDirs;

  @Parameter(names = {"-" + CONFIG_FILE}, description = "Configuration file to load ('*.properties', '*.cfg' or '-'' for stdin)")
  private String configSource;

  @Parameter(names = {"-" + TC_PROPERTIES}, description = "Node properties. Default: <unset>")
  private String tcProperties;

  @Parameter(names = {"-" + CLUSTER_NAME}, description = "Cluster name. Default: <unset>")
  private String clusterName;

  @Parameter(names = {"-" + LICENSE_FILE}, hidden = true)
  private String licenseFile;

  @Parameter(names = {"-" + NODE_HOME_DIR, "--" + NODE_HOME_DIR}, hidden = true)
  private String serverHome;

  @Parameter(names = {"-" + REPAIR_MODE}, description = "Start node in repair mode")
  private boolean wantsRepairMode;

  @Parameter(names = {"-" + HELP}, description = "Command-line help")
  private boolean help;

  @Parameter(names = {"-" + AUTO_ACTIVATE}, description = "Automatically activate the node so that it becomes active or joins a stripe")
  private boolean allowsAutoActivation;

  private final Map<Setting, String> paramValueMap = new HashMap<>();

  @Override
  public Options process(CustomJCommander jCommander) {
    validateOptions(jCommander);
    extractTopologyOptions(jCommander);
    Options options = new Options(paramValueMap);
    // set settings not in paramValueMap
    options.setConfigDir(configDir);
    options.setConfigSource(configSource);
    options.setLicenseFile(licenseFile);
    options.setServerHome(serverHome);
    options.setWantsRepairMode(wantsRepairMode);
    options.setAllowsAutoActivation(allowsAutoActivation);
    options.setHelp(help);
    return options;
  }

  /**
   * Constructs a {@code Map} containing only the parameters relevant to {@code Node} object with the longest parameter name
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
          return !longestName.equals(addDash(LICENSE_FILE))
              && !longestName.equals(addDash(CONFIG_FILE))
              && !longestName.equals(addDash(NODE_HOME_DIR))
              && !longestName.equals(addDash(REPAIR_MODE))
              && !longestName.equals(addDash(AUTO_ACTIVATE))
              && !longestName.equals(addDash(NODE_CONFIG_DIR))
              && !longestName.equals(addDash(HELP));
        })
        .forEach(pd -> paramValueMap.put(Setting.fromName(ConsoleParamsUtils.stripDash(pd.getLongestName())), pd.getParameterized().get(this).toString()));
  }

  private void validateOptions(CustomJCommander jCommander) {
    if (configSource != null) {
      if (nodeName != null && (port != null || hostname != null)) {
        throw new IllegalArgumentException("'" + addDash(NODE_NAME) + "' parameter cannot be used with '"
            + addDash(NODE_HOSTNAME) + "' or '" + addDash(NODE_PORT) + "' parameter");
      }

      Set<String> filteredOptions = new HashSet<>(jCommander.getUserSpecifiedOptions());
      filteredOptions.remove(addDash(AUTO_ACTIVATE));
      filteredOptions.remove(addDash(REPAIR_MODE));
      filteredOptions.remove(addDash(CONFIG_FILE));
      filteredOptions.remove(addDash(NODE_HOME_DIR));
      filteredOptions.remove(addDash(LICENSE_FILE));
      filteredOptions.remove(addDash(NODE_HOSTNAME));
      filteredOptions.remove(addDash(NODE_PORT));
      filteredOptions.remove(addDash(NODE_NAME));
      filteredOptions.remove(addDash(NODE_CONFIG_DIR));

      if (!filteredOptions.isEmpty()) {
        throw new IllegalArgumentException(
            String.format(
                "'%s' parameter can only be used with '%s', '%s', '%s', '%s' and '%s' parameters",
                addDash(CONFIG_FILE),
                addDash(REPAIR_MODE),
                addDash(NODE_NAME),
                addDash(NODE_HOSTNAME),
                addDash(NODE_PORT),
                addDash(NODE_CONFIG_DIR)
            )
        );
      }
    } else {
      // when using CLI parameters
      if (licenseFile != null) {
        if (clusterName == null) {
          throw new IllegalArgumentException("'" + addDash(LICENSE_FILE) + "' parameter must be used with '" + addDash(CLUSTER_NAME) + "' parameter");
        }

        if (!allowsAutoActivation) {
          throw new IllegalArgumentException("'" + addDash(LICENSE_FILE) + "' parameter must be used with '" + addDash(AUTO_ACTIVATE) + "' parameter");
        }
      }
    }
  }
}
