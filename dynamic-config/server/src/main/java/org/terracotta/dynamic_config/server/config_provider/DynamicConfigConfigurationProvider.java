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
package org.terracotta.dynamic_config.server.config_provider;

import com.tc.classloader.OverrideService;
import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import com.tc.exception.ZapDirtyDbServerNodeException;
import org.terracotta.configuration.Configuration;
import org.terracotta.configuration.ConfigurationException;
import org.terracotta.configuration.ConfigurationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.ConfigRepositoryMapper;
import org.terracotta.dynamic_config.api.service.ConfigRepositoryMapperDiscovery;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.server.TerracottaNode;
import org.terracotta.dynamic_config.server.nomad.NomadBootstrapper;
import org.terracotta.dynamic_config.server.sync.DynamicConfigSyncData;
import org.terracotta.dynamic_config.server.sync.DynamicConfigurationPassiveSync;
import org.terracotta.dynamic_config.server.sync.Require;
import org.terracotta.nomad.server.NomadException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static com.tc.util.Assert.assertNotNull;
import static java.lang.System.lineSeparator;
import static java.nio.file.Files.newInputStream;
import static java.util.Collections.emptySet;
import static org.terracotta.dynamic_config.server.sync.Require.RESTART_REQUIRED;
import static org.terracotta.dynamic_config.server.sync.Require.ZAP_REQUIRED;

@OverrideService("com.tc.config.DefaultConfigurationProvider")
public class DynamicConfigConfigurationProvider implements ConfigurationProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigConfigurationProvider.class);

  private volatile DynamicConfigurationPassiveSync dynamicConfigurationPassiveSync;
  private volatile DynamicConfigConfiguration configuration;

  @SuppressWarnings("ConstantConditions")
  @Override
  public void initialize(List<String> args) throws ConfigurationException {
    // Nomad system must have been bootstrapped BEFORE any call to TCServerMain
    NomadBootstrapper.NomadServerManager nomadServerManager = NomadBootstrapper.getNomadServerManager();
    assertNotNull(nomadServerManager);

    this.dynamicConfigurationPassiveSync = new DynamicConfigurationPassiveSync(
        nomadServerManager.getConfiguration().orElse(null),
        nomadServerManager.getNomadServer(),
        nomadServerManager.getDynamicConfigService(),
        () -> nomadServerManager.getDynamicConfigService().getLicenseContent().orElse(null));

    // we do not need a complex parer since the CLI is controlled by the StartupManager class\
    // --node-repository-dir <dir>
    // --node-name <name>
    // --diagnostic-mode
    // --tc-config-file <file>
    Path nodeRepositoryDir = null;
    Path tcConfigFile = null;
    String nodeName = null;
    boolean diagnosticMode = false;
    for (int i = 0; i < args.size(); i++) {
      String arg = args.get(i);
      switch (arg) {
        case "--node-repository-dir":
          nodeRepositoryDir = Paths.get(args.get(++i));
          break;
        case "--node-name":
          nodeName = args.get(++i);
          break;
        case "--diagnostic-mode":
          diagnosticMode = true;
          break;
        case "--tc-config-file":
          tcConfigFile = Paths.get(args.get(++i));
          break;
        default:
          throw new AssertionError("Unrecognized option: " + arg);
      }
    }

    assertNotNull(nodeRepositoryDir);
    assertNotNull(nodeName);

    TcConfiguration tcConfiguration;
    try {
      tcConfiguration = createdTcConfiguration(nomadServerManager, tcConfigFile);
    } catch (IOException | SAXException e) {
      throw new ConfigurationException("Unable to created TcConfiguration with startup parameters " + args, e);
    }

    LOGGER.info("Startup configuration of the node: {}{}{}", lineSeparator(), lineSeparator(), tcConfiguration);
    configuration = new DynamicConfigConfiguration(tcConfiguration, diagnosticMode || tcConfigFile != null);

    if (diagnosticMode) {
      // If diagnostic mode is ON:
      // - the node won't be activated (Nomad 2 phase commit system won't be available)
      // - the diagnostic port will be available for the repair command to be able to rewrite the append log
      // - the TcConfig created will be stripped to make platform think this node is alone
      final String serverName = nodeName;
      configuration.getPlatformConfiguration().getServers().getServer().removeIf(server -> !server.getName().equals(serverName));
    }
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public String getConfigurationParamsDescription() {
    throw new UnsupportedOperationException("Please start the node by using the new script start-node.sh or start-node.bat or the main class " + TerracottaNode.class);
  }

  @Override
  public byte[] getSyncData() {
    return dynamicConfigurationPassiveSync != null ? dynamicConfigurationPassiveSync.getSyncData().encode() : new byte[0];
  }

  @Override
  public void sync(byte[] bytes) {
    if (dynamicConfigurationPassiveSync != null) {

      Set<Require> requires;
      try {
        requires = dynamicConfigurationPassiveSync.sync(DynamicConfigSyncData.decode(bytes));
      } catch (NomadException | RuntimeException e) {
        // shutdown the server because of a unrecoverable error
        throw new TCShutdownServerException("Shutdown because of sync failure: " + e.getMessage(), e);
      }

      if (requires.contains(RESTART_REQUIRED)) {
        if (requires.contains(ZAP_REQUIRED)) {
          throw new ZapDirtyDbServerNodeException("Zapping server");
        } else {
          throw new TCServerRestartException("Restarting server");
        }
      }
    }
  }

  @Override
  public void close() {
    // Do nothing
  }

  private static TcConfiguration createdTcConfiguration(NomadBootstrapper.NomadServerManager nomadServerManager, Path temporaryTcConfigFile) throws IOException, SAXException {
    IParameterSubstitutor parameterSubstitutor = nomadServerManager.getParameterSubstitutor();
    PathResolver userDirResolver = new PathResolver(Paths.get("%(user.dir)"), parameterSubstitutor::substitute);

    TcConfiguration tcConfiguration;
    if (temporaryTcConfigFile != null) {
      String source = parameterSubstitutor.substitute(userDirResolver.getBaseDir()).toString();
      tcConfiguration = TCConfigurationParser.parse(newInputStream(temporaryTcConfigFile), emptySet(), source);

    } else {
      // Sadly platform does not support anything else from XML to load so we have no choice but to re-generate on fly this XML data
      NodeContext nodeContext = nomadServerManager.getConfiguration()
          .orElseThrow(() -> new IllegalStateException("Node has not been activated or migrated properly: unable find the latest committed configuration to use at startup. Please delete the repository folder and try again."));
      // This path resolver is used when converting a model to XML.
      // It makes sure to resolve any relative path to absolute ones based on the working directory.
      // This is necessary because if some relative path ends up in the XML exactly like they are in the model,
      // then platform will rebase these paths relatively to the config XML file which is inside a sub-folder in
      // the config repository: repository/config.
      // So this has the effect of putting all defined directories inside such as repository/config/logs, repository/config/user-data, repository/metadata, etc
      // That is why we need to force the resolving within the XML relatively to the user directory.
      ConfigRepositoryMapper configRepositoryMapper = new ConfigRepositoryMapperDiscovery(userDirResolver).find()
          .orElseThrow(() -> new AssertionError("No " + ConfigRepositoryMapper.class.getName() + " service implementation found on classpath"));
      String xml = configRepositoryMapper.toXml(nodeContext);
      // TCConfigurationParser substitutes values for platform parameters, so anything known to platform needn't be substituted before this
      tcConfiguration = TCConfigurationParser.parse(xml);
    }
    return tcConfiguration;
  }
}
