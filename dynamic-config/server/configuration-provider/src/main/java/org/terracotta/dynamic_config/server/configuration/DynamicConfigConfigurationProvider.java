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
package org.terracotta.dynamic_config.server.configuration;

import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.configuration.ConfigurationProvider;
import org.terracotta.diagnostic.server.api.DiagnosticServicesHolder;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventService;
import org.terracotta.dynamic_config.server.api.DynamicConfigExtension;
import org.terracotta.dynamic_config.server.api.DynamicConfigNomadServer;
import org.terracotta.dynamic_config.server.api.LicenseParserDiscovery;
import org.terracotta.dynamic_config.server.api.LicenseService;
import org.terracotta.dynamic_config.server.api.NomadPermissionChangeProcessor;
import org.terracotta.dynamic_config.server.api.NomadRoutingChangeProcessor;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.dynamic_config.server.configuration.service.ConfigChangeHandlerManagerImpl;
import org.terracotta.dynamic_config.server.configuration.service.NomadServerManager;
import org.terracotta.dynamic_config.server.configuration.service.ParameterSubstitutor;
import org.terracotta.dynamic_config.server.configuration.startup.CommandLineProcessor;
import org.terracotta.dynamic_config.server.configuration.startup.ConfigurationGeneratorVisitor;
import org.terracotta.dynamic_config.server.configuration.startup.CustomJCommander;
import org.terracotta.dynamic_config.server.configuration.startup.MainCommandLineProcessor;
import org.terracotta.dynamic_config.server.configuration.startup.Options;
import org.terracotta.dynamic_config.server.configuration.startup.StartupConfiguration;
import org.terracotta.dynamic_config.server.configuration.startup.parsing.OptionsParsing;
import org.terracotta.dynamic_config.server.configuration.startup.parsing.OptionsParsingImpl;
import org.terracotta.dynamic_config.server.configuration.startup.parsing.deprecated.DeprecatedOptionsParsingImpl;
import org.terracotta.dynamic_config.server.configuration.sync.DynamicConfigSyncData;
import org.terracotta.dynamic_config.server.configuration.sync.DynamicConfigurationPassiveSync;
import org.terracotta.dynamic_config.server.configuration.sync.Require;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;
import org.terracotta.server.StopAction;

import java.util.List;
import java.util.Set;

import static java.lang.System.lineSeparator;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.terracotta.dynamic_config.server.configuration.sync.Require.RESTART_REQUIRED;
import static org.terracotta.dynamic_config.server.configuration.sync.Require.ZAP_REQUIRED;

public class DynamicConfigConfigurationProvider implements ConfigurationProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigConfigurationProvider.class);

  private volatile DynamicConfigurationPassiveSync dynamicConfigurationPassiveSync;
  private volatile NomadServerManager nomadServerManager;
  private volatile DynamicConfigSyncData.Codec synCodec;
  private volatile StartupConfiguration configuration;
  private volatile Server server;

  @Override
  public void initialize(List<String> args) {
    withMyClassLoader(() -> {
      server = ServerEnv.getServer();

      ClassLoader serviceClassLoader = getServiceClassLoader(server);

      // substitution service from placeholders
      IParameterSubstitutor parameterSubstitutor = new ParameterSubstitutor();

      // service containing the list of dynamic change handlers per settings
      ConfigChangeHandlerManager configChangeHandlerManager = new ConfigChangeHandlerManagerImpl();

      // service used to create a topology from input CLI or config file
      ClusterFactory clusterFactory = new ClusterFactory();

      // optional service enabling license parsing
      LicenseService licenseService = new LicenseParserDiscovery(serviceClassLoader).find().orElseGet(LicenseService::unsupported);

      // initialize the json system
      ObjectMapperFactory objectMapperFactory = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule());

      // Service used to manage and initialize the Nomad 2PC system
      nomadServerManager = new NomadServerManager(parameterSubstitutor, configChangeHandlerManager, licenseService, objectMapperFactory, server);
      synCodec = new DynamicConfigSyncData.Codec(objectMapperFactory);

      // CLI parsing
      Options options = parseCommandLineOrExit(args);

      // This path resolver is used when converting a model to XML.
      // It makes sure to resolve any relative path to absolute ones based on the working directory.
      // This is necessary because if some relative path ends up in the XML exactly like they are in the model,
      // then platform will rebase these paths relatively to the config XML file which is inside a sub-folder in
      // the config directory: config/cluster.
      // So this has the effect of putting all defined directories inside such as config/config/logs, config/config/user-data, config/metadata, etc
      // That is why we need to force the resolving within the XML relatively to the user directory.
      String serverHome = options.getServerHome();
      if (serverHome == null) serverHome = System.getProperty("user.dir");
      Path baseDir = Paths.get(serverHome);
      PathResolver userDirResolver = new PathResolver(baseDir, parameterSubstitutor::substitute);

      // Configuration generator class
      // Initialized when processing the CLI depending oin the user input, and called to generate a configuration
      ConfigurationGeneratorVisitor configurationGeneratorVisitor = new ConfigurationGeneratorVisitor(parameterSubstitutor, nomadServerManager, serviceClassLoader, userDirResolver, objectMapperFactory, server);

      // processors for the CLI
      CommandLineProcessor commandLineProcessor = new MainCommandLineProcessor(options, clusterFactory, configurationGeneratorVisitor, parameterSubstitutor, server);

      // process the CLI and initialize the Nomad system and ConfigurationGeneratorVisitor
      commandLineProcessor.process();

      // retrieve initialized services
      DynamicConfigNomadServer nomadServer = nomadServerManager.getNomadServer();
      DynamicConfigService dynamicConfigService = nomadServerManager.getDynamicConfigService();
      TopologyService topologyService = nomadServerManager.getTopologyService();
      DynamicConfigEventService eventService = nomadServerManager.getEventRegistrationService();

      // initialize the passive sync service
      dynamicConfigurationPassiveSync = new DynamicConfigurationPassiveSync(
          nomadServerManager.getConfiguration().orElse(null),
          nomadServer,
          dynamicConfigService,
          topologyService, () -> dynamicConfigService.getLicenseContent().orElse(null));

      // generate the configuration wrapper
      configuration = configurationGeneratorVisitor.generateConfiguration();

      //  exposes services through org.terracotta.entity.PlatformConfiguration
      configuration.registerExtendedConfiguration(ObjectMapperFactory.class, objectMapperFactory);
      configuration.registerExtendedConfiguration(IParameterSubstitutor.class, parameterSubstitutor);
      configuration.registerExtendedConfiguration(ConfigChangeHandlerManager.class, configChangeHandlerManager);
      configuration.registerExtendedConfiguration(DynamicConfigEventService.class, eventService);
      configuration.registerExtendedConfiguration(TopologyService.class, topologyService);
      configuration.registerExtendedConfiguration(DynamicConfigService.class, dynamicConfigService);
      configuration.registerExtendedConfiguration(DynamicConfigEventFiring.class, nomadServerManager.getEventFiringService());
      configuration.registerExtendedConfiguration(NomadServer.class, nomadServer);
      configuration.registerExtendedConfiguration(DynamicConfigNomadServer.class, nomadServer);
      configuration.registerExtendedConfiguration(LicenseService.class, licenseService);
      configuration.registerExtendedConfiguration(PathResolver.class, userDirResolver);
      configuration.registerExtendedConfiguration(NomadRoutingChangeProcessor.class, nomadServerManager.getNomadRoutingChangeProcessor());
      configuration.registerExtendedConfiguration(NomadPermissionChangeProcessor.class, nomadServerManager.getNomadPermissionChangeProcessor());

      // discover the dynamic config extensions (offheap, dataroot, lease, etc)
      configuration.discoverExtensions();

      // Expose some services through diagnostic port
      DiagnosticServicesHolder.willRegister(TopologyService.class, topologyService);
      DiagnosticServicesHolder.willRegister(DynamicConfigService.class, dynamicConfigService);
      DiagnosticServicesHolder.willRegister(NomadServer.class, nomadServer);

      LOGGER.info("Startup configuration of the node: {}{}{}", lineSeparator(), lineSeparator(), configuration);

      warnIfPreparedChange();
    });
  }



  @Override
  public StartupConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public String getConfigurationParamsDescription() {
    StringBuilder out = new StringBuilder();
    CustomJCommander jCommander = new CustomJCommander(new OptionsParsingImpl());
    jCommander.getUsageFormatter().usage(out);
    return out.toString();
  }

  @Override
  public byte[] getSyncData() {
    return dynamicConfigurationPassiveSync == null ? new byte[0] : synCodec.encode(dynamicConfigurationPassiveSync.getSyncData());
  }

  @Override
  public void sync(byte[] bytes) {
    if (dynamicConfigurationPassiveSync != null) {

      Set<Require> requires;
      try {
        DynamicConfigSyncData data = synCodec.decode(bytes);
        requires = dynamicConfigurationPassiveSync.sync(data);
      } catch (RuntimeException | NomadException e) {
        // only log the full trace if in trace/debug mode
        LOGGER.debug("Error: {}", e.getMessage(), e);
        server.warn(lineSeparator() + lineSeparator()
                + "==============================================================================================================================================" + lineSeparator()
                + "SERVER WILL STOP: PASSIVE SYNC FAILED WITH ERROR: {}" + lineSeparator()
                + "(please change the logging config to see more details)" + lineSeparator()
                + "==============================================================================================================================================" + lineSeparator(),
            e.getMessage());
        // ask for the server to stop and not restart
        server.stop();
        // do not continue anymore
        return;
      }

      if (requires.contains(RESTART_REQUIRED)) {
        Server server = ServerEnv.getServer();
        if (requires.contains(ZAP_REQUIRED)) {
          server.warn("Zapping server");
          server.stop(StopAction.ZAP, StopAction.RESTART);
        } else {
          server.stop(StopAction.RESTART);
        }
      }

      warnIfPreparedChange();
    }
  }

  @Override
  public void close() {
    nomadServerManager.getNomadServer().close();
  }

  private void withMyClassLoader(Runnable runnable) {
    ClassLoader classLoader = getClass().getClassLoader();
    ClassLoader oldloader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      runnable.run();
    } finally {
      Thread.currentThread().setContextClassLoader(oldloader);
    }
  }

  private void warnIfPreparedChange() {
    if (nomadServerManager.getNomadServer().hasIncompleteChange()) {
      LOGGER.warn(lineSeparator() + lineSeparator()
          + "==============================================================================================================================================" + lineSeparator()
          + "The configuration of this node has not been committed or rolled back. Please run the 'diagnostic' command to diagnose the configuration state." + lineSeparator()
          + "==============================================================================================================================================" + lineSeparator()
      );
    }
  }

  private ClassLoader getServiceClassLoader(Server server) {
    return server.getServiceClassLoader(getClass().getClassLoader(),
        DynamicConfigExtension.class,
        LicenseService.class);
  }

  private static Options parseCommandLineOrExit(List<String> args) {
    try {
      OptionsParsing optionsParsing = new OptionsParsingImpl();
      CustomJCommander jCommander = new CustomJCommander(optionsParsing);
      jCommander.parse(args.toArray(new String[0]));
      return optionsParsing.process(jCommander);
    } catch (ParameterException e) {
      // Fallback to deprecated options
      try {
        DeprecatedOptionsParsingImpl deprecatedOptionsParsing = new DeprecatedOptionsParsingImpl();
        CustomJCommander jCommander = new CustomJCommander(deprecatedOptionsParsing);
        jCommander.parse(args.toArray(new String[0]));
        return deprecatedOptionsParsing.process(jCommander);
      } catch (ParameterException de) {
        throw e;
      }
    }
  }

}
