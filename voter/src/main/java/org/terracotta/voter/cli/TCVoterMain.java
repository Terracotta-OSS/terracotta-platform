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
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.connection.CompatibleDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DefaultDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.json.DynamicConfigJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.inet.HostPort;
import org.terracotta.inet.InetSocketAddressConverter;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.voter.TCVoter;
import org.terracotta.voter.TCVoterImpl;
import org.terracotta.voter.VotingGroup;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.terracotta.connection.ConnectionPropertyNames.CONNECTION_NAME;
import static org.terracotta.connection.ConnectionPropertyNames.CONNECTION_TIMEOUT;
import static org.terracotta.connection.DiagnosticsFactory.REQUEST_TIMEOUT;

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

    Properties connectionProps = getConnectionProperties(options);
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
    Json.Factory factory = createJsonFactory();
    return new CompatibleDiagnosticServiceProvider(new DefaultDiagnosticServiceProvider(options.getConnectionName(), options.getConnectionTimeout(), options.getRequestTimeout(), null, factory));
  }

  protected Json.Factory createJsonFactory() {
    return new DefaultJsonFactory().withModule(new DynamicConfigJsonModule());
  }

  // concurrently connects to the user-provided servers to fetch the topology and returns as soon as we get one
  protected Cluster fetchTopology(Options options) {
    DiagnosticServiceProvider diagnosticServiceProvider = createDiagnosticServiceProvider(options);
    final Collection<InetSocketAddress> addresses = options.getServersHostPort()
        .stream()
        .map(input -> InetSocketAddressConverter.parseInetSocketAddress(input, 9410))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    while (!Thread.currentThread().isInterrupted()) {
      for (InetSocketAddress addr : addresses) {
        LOGGER.info("Trying to fetch cluster topology from {}...", addr);
        try (DiagnosticService diagnosticService = diagnosticServiceProvider.fetchDiagnosticService(addr)) {
          final TopologyService topologyService = diagnosticService.getProxy(TopologyService.class);
          if (topologyService.isActivated()) {
            final Cluster cluster = topologyService.getUpcomingNodeContext().getCluster();
            LOGGER.info("Found activated cluster:\n{}", cluster);
            return cluster;
          } else {
            LogicalServerState state = diagnosticService.getLogicalServerState();
            LOGGER.info("Node {} in state {} is not part of an activated cluster. Trying next one in 5s...", addr, state);
            Thread.sleep(5_000);
          }
        } catch (Exception e) {
          LOGGER.info("Error communicating with node {}. Trying next one in 5 s...", addr);
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Error: {}", e.getMessage(), e);
          } else {
            LOGGER.info("Error: {}", e.getMessage());
          }
          try {
            Thread.sleep(5_000);
          } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
    }

    throw new RuntimeException("interrupted");
  }

  protected OptionsParsing getParsingObject() {
    return new OptionsParsingImpl();
  }

  protected Properties getConnectionProperties(Options option) {
    Properties props = new Properties();
    props.setProperty(CONNECTION_NAME, option.getConnectionName());
    props.setProperty(CONNECTION_TIMEOUT, String.valueOf(option.getConnectionTimeout().toMillis()));
    props.setProperty(REQUEST_TIMEOUT, String.valueOf(option.getRequestTimeout().toMillis()));
    return props;
  }

  protected void processServerArg(Properties connectionProps, Options options) {
    Cluster cluster = fetchTopology(options);
    validateCluster(cluster);

    // The endpoints we should use (public or internal) based on the user-provided list
    final List<HostPort> initiators = options.getServersHostPort().stream().map(hostPort -> HostPort.parse(hostPort, 9410)).collect(toList());
    cluster.determineEndpoints(initiators)
        .stream()
        .collect(groupingBy(
            endpoint -> cluster.getStripeByNode(endpoint.getNodeUID()).get().getUID(),
            mapping(endpoint -> endpoint.getHostPort().toString(), toList())))
        .forEach((uid, endpoints) -> startVoter(connectionProps, endpoints.toArray(new String[0])));
  }

  protected TCVoter getVoter(Properties connectionProps) {
    return new TCVoterImpl(connectionProps);
  }

  protected void startVoter(Properties connectionProps, String... hostPorts) {
    new VotingGroup(ID, connectionProps, hostPorts).start();
  }

  protected void validateCluster(Cluster cluster) {
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
