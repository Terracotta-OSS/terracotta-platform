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
package org.terracotta.dynamic_config.server.configuration.startup.parsing.deprecated;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.server.configuration.startup.ConsoleParamsUtils;
import org.terracotta.dynamic_config.server.configuration.startup.CustomJCommander;
import org.terracotta.dynamic_config.server.configuration.startup.Options;
import org.terracotta.dynamic_config.server.configuration.startup.parsing.OptionsParsing;

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
import static org.terracotta.dynamic_config.server.configuration.startup.ConsoleParamsUtils.addDashDash;

@Parameters(separators = "=")
public class DeprecatedOptionsParsingImpl implements OptionsParsing {

  @Parameter(names = {"-s", "--" + NODE_HOSTNAME}, description = "node host name")
  private String hostname;

  @Parameter(names = {"-S", "--" + NODE_PUBLIC_HOSTNAME}, description = "public node host name")
  private String publicHostname;

  @Parameter(names = {"-p", "--" + NODE_PORT}, description = "node port")
  private String port;

  @Parameter(names = {"-P", "--" + NODE_PUBLIC_PORT}, description = "public node port")
  private String publicPort;

  @Parameter(names = {"-g", "--" + NODE_GROUP_PORT}, description = "node port used for intra-stripe communication")
  private String groupPort;

  @Parameter(names = {"-n", "--" + NODE_NAME}, description = "node name")
  private String nodeName;

  @Parameter(names = {"-E", "--" + STRIPE_NAME}, description = "stripe name")
  private String stripeName;

  @Parameter(names = {"-a", "--" + NODE_BIND_ADDRESS}, description = "node bind address for port")
  private String bindAddress;

  @Parameter(names = {"-A", "--" + NODE_GROUP_BIND_ADDRESS}, description = "node bind address for group port")
  private String groupBindAddress;

  @Parameter(names = {"-r", "--" + NODE_CONFIG_DIR}, description = "node configuration directory")
  private String configDir;

  @Parameter(names = {"-m", "--" + NODE_METADATA_DIR}, description = "node metadata directory")
  private String metadataDir;

  @Parameter(names = {"-L", "--" + NODE_LOG_DIR}, description = "node log directory")
  private String logDir;

  @Parameter(names = {"-b", "--" + NODE_BACKUP_DIR}, description = "node backup directory")
  private String backupDir;

  @Parameter(names = {"-x", "--" + SECURITY_DIR}, description = "security root directory")
  private String securityDir;

  @Parameter(names = {"-u", "--" + SECURITY_AUDIT_LOG_DIR}, description = "security audit log directory")
  private String securityAuditLogDir;

  @Parameter(names = {"-z", "--" + SECURITY_AUTHC}, description = "security authentication setting (file|ldap|certificate)")
  private String securityAuthc;

  @Parameter(names = {"-t", "--" + SECURITY_SSL_TLS}, description = "ssl-tls setting (true|false)")
  private String securitySslTls;

  @Parameter(names = {"-w", "--" + SECURITY_WHITELIST}, description = "security whitelist (true|false)")
  private String securityWhitelist;

  @Parameter(names = {"-y", "--" + FAILOVER_PRIORITY}, description = "failover priority setting (availability|consistency)")
  private String failoverPriority;

  @Parameter(names = {"-R", "--" + CLIENT_RECONNECT_WINDOW}, description = "client reconnect window")
  private String clientReconnectWindow;

  @Parameter(names = {"-i", "--" + CLIENT_LEASE_DURATION}, description = "client lease duration")
  private String clientLeaseDuration;

  @Parameter(names = {"-o", "--" + OFFHEAP_RESOURCES}, description = "offheap resources")
  private String offheapResources;

  @Parameter(names = {"-d", "--" + DATA_DIRS}, description = "data directory")
  private String dataDirs;

  @Parameter(names = {"-f", "--" + CONFIG_FILE}, description = "configuration properties file")
  private String configFile;

  @Parameter(names = {"-T", "--" + TC_PROPERTIES}, description = "tc-properties")
  private String tcProperties;

  @Parameter(names = {"-N", "--" + CLUSTER_NAME}, description = "cluster name")
  private String clusterName;

  @Parameter(names = {"-l", "--" + LICENSE_FILE}, hidden = true)
  private String licenseFile;

  @Parameter(names = {"-D", "--" + REPAIR_MODE}, description = "node repair mode (true|false)")
  private boolean wantsRepairMode;

  @Parameter(names = {"--" + NODE_HOME_DIR}, hidden = true)
  private String serverHome;

  // hidden option that won't appear in the help file,
  // so that we can start a pre-activated stripe directly in dev / test.
  // no need to have a short option for this one, this is not public.
  @Parameter(names = {"--" + AUTO_ACTIVATE}, hidden = true)
  private boolean allowsAutoActivation;

  @Parameter(names = {"-h", "--" + HELP}, description = "provide usage information")
  private boolean help;
  
  private final Map<Setting, String> paramValueMap = new HashMap<>();

  @Override
  public Options process(CustomJCommander jCommander) {
    validateOptions(jCommander);
    extractTopologyOptions(jCommander);
    Options options = new Options(paramValueMap);
    // set settings not in paramValueMap
    options.setConfigDir(configDir);
    options.setConfigFile(configFile);
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
          return !longestName.equals(addDashDash(LICENSE_FILE))
              && !longestName.equals(addDashDash(CONFIG_FILE))
              && !longestName.equals(addDashDash(NODE_HOME_DIR))
              && !longestName.equals(addDashDash(REPAIR_MODE))
              && !longestName.equals(addDashDash(AUTO_ACTIVATE))
              && !longestName.equals(addDashDash(NODE_CONFIG_DIR))
              && !longestName.equals(addDashDash(HELP));
        })
        .forEach(pd -> paramValueMap.put(Setting.fromName(ConsoleParamsUtils.stripDashDash(pd.getLongestName())), pd.getParameterized().get(this).toString()));
  }

  private void validateOptions(CustomJCommander jCommander) {
    if (configFile != null) {
      if (nodeName != null && (port != null || hostname != null)) {
        throw new IllegalArgumentException("'" + addDashDash(NODE_NAME) + "' parameter cannot be used with '"
            + addDashDash(NODE_HOSTNAME) + "' or '" + addDashDash(NODE_PORT) + "' parameter");
      }

      Set<String> filteredOptions = new HashSet<>(jCommander.getUserSpecifiedOptions());
      filteredOptions.remove(addDashDash(AUTO_ACTIVATE));
      filteredOptions.remove(addDashDash(REPAIR_MODE));
      filteredOptions.remove("-D");

      filteredOptions.remove(addDashDash(CONFIG_FILE));
      filteredOptions.remove("-f");

      filteredOptions.remove(addDashDash(NODE_HOME_DIR));

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

      if (!filteredOptions.isEmpty()) {
        throw new IllegalArgumentException(
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
          throw new IllegalArgumentException("'" + addDashDash(LICENSE_FILE) + "' parameter must be used with '" + addDashDash(CLUSTER_NAME) + "' parameter");
        }

        if (!allowsAutoActivation) {
          throw new IllegalArgumentException("'" + addDashDash(LICENSE_FILE) + "' parameter must be used with '" + addDashDash(AUTO_ACTIVATE) + "' parameter");
        }
      }
    }
  }
}
