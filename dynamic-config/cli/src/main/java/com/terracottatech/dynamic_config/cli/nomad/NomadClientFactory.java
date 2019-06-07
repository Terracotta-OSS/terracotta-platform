/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.nomad;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.diagnostic.client.connection.NodeInfo;
import com.terracottatech.nomad.client.NamedNomadServer;
import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.server.NomadServer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NomadClientFactory {

  private final MultiDiagnosticServiceConnectionFactory connectionFactory;
  private final NomadEnvironment environment;
  private final long requestTimeoutMillis;
  private final ConcurrencySizing concurrencySizing;

  public NomadClientFactory(MultiDiagnosticServiceConnectionFactory connectionFactory, ConcurrencySizing concurrencySizing, NomadEnvironment environment, long requestTimeoutMillis) {
    this.connectionFactory = connectionFactory;
    this.environment = environment;
    this.requestTimeoutMillis = requestTimeoutMillis;
    this.concurrencySizing = concurrencySizing;
  }

  public CloseableNomadClient createClient(List<String> hostPortList) {
    String host = environment.getHost();
    String user = environment.getUser();

    MultiDiagnosticServiceConnection connection = connectionFactory.createConnection(hostPortList);

    Set<NamedNomadServer> servers = connection.getServers().stream()
        .map(server -> createNamedNomadServer(server, connection.getDiagnosticService(server)))
        .collect(Collectors.toSet());

    NomadClient client = new NomadClient(servers, host, user);
    int concurrency = concurrencySizing.getThreadCount(servers.size());
    client.setConcurrency(concurrency);
    client.setTimeoutMillis(requestTimeoutMillis);

    return new CloseableNomadClient(client, connection);
  }

  private NamedNomadServer createNamedNomadServer(NodeInfo node, DiagnosticService diagnosticService) {
    NomadServer nomadServerProxy = diagnosticService.getProxy(NomadServer.class);
    return new NamedNomadServer(node.getName(), nomadServerProxy);
  }
}
