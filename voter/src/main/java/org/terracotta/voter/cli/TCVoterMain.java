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
package org.terracotta.voter.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.client.connection.CompatibleDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.ConcurrencySizing;
import org.terracotta.diagnostic.client.connection.ConcurrentDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DefaultDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.inet.InetSocketAddressConverter;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.voter.ActiveVoter;
import org.terracotta.voter.TCVoter;
import org.terracotta.voter.TCVoterImpl;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class TCVoterMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCVoterMain.class);

  private static final String ID = UUID.randomUUID().toString();

  public static void main(String[] args) {
    TCVoterMain main = new TCVoterMain();
    writePID();
    main.processArgs(args);
  }

  public void processArgs(String[] args) {
    OptionsParsing optionsParsing = getParsingObject();
    CustomJCommander jCommander = new CustomJCommander(optionsParsing);
    jCommander.parse(args);
    Options options = optionsParsing.process();

    if (options.isHelp()) {
      jCommander.usage();
      return;
    }

    Optional<Properties> connectionProps = getConnectionProperties(options);
    if (options.getServersHostPort() != null) {
      options.getServersHostPort().forEach(this::validateHostPort);
      processServerArg(connectionProps, options);
    } else if (options.getOverrideHostPort() != null) {
      String hostPort = options.getOverrideHostPort();
      validateHostPort(hostPort);
      getVoter(connectionProps).overrideVote(hostPort);
    } else {
      throw new AssertionError("This should not happen");
    }
  }

  protected DiagnosticServiceProvider createDiagnosticServiceProvider(Options options) {
    ObjectMapperFactory objectMapperFactory = createObjectMapperFactory();
    return new CompatibleDiagnosticServiceProvider(new DefaultDiagnosticServiceProvider("Voter", null, null, null, objectMapperFactory));
  }

  protected ObjectMapperFactory createObjectMapperFactory() {
    return new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule());
  }

  private static ConcurrencySizing createConcurrencySizing() {
    return new ConcurrencySizing();
  }

  // concurrently connects to the user-provided servers to fetch the topology and returns as soon as we get one
  protected Cluster fetchTopology(Options options) {
    DiagnosticServiceProvider diagnosticServiceProvider = createDiagnosticServiceProvider(options);
    ConcurrencySizing concurrencySizing = createConcurrencySizing();
    MultiDiagnosticServiceProvider multiDiagnosticServiceProvider = new ConcurrentDiagnosticServiceProvider(diagnosticServiceProvider, null, concurrencySizing);

    final Map<String, InetSocketAddress> addresses = options.getServersHostPort()
        .stream()
        .collect(toMap(identity(), InetSocketAddressConverter::getInetSocketAddress, (value1, value2) -> value1));

    try (final DiagnosticServices<String> diagnosticServices = multiDiagnosticServiceProvider.fetchAnyOnlineDiagnosticService(addresses)) {
      return diagnosticServices.getOnlineEndpoints()
          .values()
          .stream()
          .findFirst()
          .map(ds -> ds.getProxy(TopologyService.class))
          .map(TopologyService::getUpcomingNodeContext)
          .map(NodeContext::getCluster)
          .orElseThrow(() -> new IllegalStateException("All servers are unreachable"));
    }
  }

  protected OptionsParsing getParsingObject() {
    return new OptionsParsingImpl();
  }

  protected Optional<Properties> getConnectionProperties(Options option) {
    return Optional.empty();
  }

  protected void processServerArg(Optional<Properties> connectionProps, Options options) {
    Cluster cluster = fetchTopology(options);
    validateCLuster(cluster);

    // The endpoints we should use (public or internal) based on the user-provided list
    final List<InetSocketAddress> initiators = options.getServersHostPort().stream().map(InetSocketAddressConverter::getInetSocketAddress).collect(toList());
    cluster.determineEndpoints(initiators)
        .stream()
        .collect(groupingBy(
            endpoint -> cluster.getStripeByNode(endpoint.getNodeUID()).get().getUID(),
            mapping(endpoint -> endpoint.getAddress().toString(), toList())))
        .forEach((uid, endpoints) -> startVoter(connectionProps, endpoints.toArray(new String[0])));
  }

  protected TCVoter getVoter(Optional<Properties> connectionProps) {
    return new TCVoterImpl();
  }

  protected void startVoter(Optional<Properties> connectionProps, String... hostPorts) {
    new ActiveVoter(ID, new CompletableFuture<>(), connectionProps, hostPorts).start();
  }

  protected void validateCLuster(Cluster cluster) {
    if (cluster.getStripeCount() > 1) {
      throw new RuntimeException("Usage of multiple stripes is not supported");
    }
  }

  protected void validateHostPort(String hostPort) {
    URI uri;
    try {
      uri = new URI("tc://" + hostPort);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    if (uri.getHost() == null || uri.getPort() == -1) {
      throw new RuntimeException("Invalid host:port combination provided: " + hostPort);
    }
  }

  protected static void writePID() {
    try {
      String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      long pid = Long.parseLong(processName.split("@")[0]);
      LOGGER.info("PID is {}", pid);
    } catch (Throwable t) {
      LOGGER.warn("Unable to fetch the PID of this process.");
    }
  }
}
