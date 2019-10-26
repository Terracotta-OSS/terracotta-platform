/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.nomad;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.connection.ConcurrencySizing;
import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import com.terracottatech.nomad.NomadEnvironment;
import com.terracottatech.nomad.client.NamedNomadServer;
import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.server.NomadServer;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class NomadClientFactory<T> {

  private final MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  private final NomadEnvironment environment;
  private final Duration requestTimeout;
  private final ConcurrencySizing concurrencySizing;

  public NomadClientFactory(MultiDiagnosticServiceProvider multiDiagnosticServiceProvider, ConcurrencySizing concurrencySizing,
                            NomadEnvironment environment, Duration requestTimeout) {
    this.multiDiagnosticServiceProvider = multiDiagnosticServiceProvider;
    this.environment = environment;
    this.requestTimeout = requestTimeout;
    this.concurrencySizing = concurrencySizing;
  }

  public CloseableNomadClient<T> createClient(Collection<InetSocketAddress> expectedOnlineNodes) {
    String host = environment.getHost();
    String user = environment.getUser();

    DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(expectedOnlineNodes);

    Collection<NamedNomadServer<T>> servers = diagnosticServices.getOnlineEndpoints().stream()
        .map(endpoint -> this.createNamedNomadServer(endpoint, diagnosticServices.getDiagnosticService(endpoint)
            .orElseThrow(() -> new IllegalStateException("DiagnosticService not found for node " + endpoint))))
        .collect(toList());

    //TODO [DYNAMIC-CONFIG]: TDB-4601: Allows to only connect to the online nodes, return only online nodes (fetchDiagnosticServices is throwing at the moment)
    NomadClient<T> client = new NomadClient<>(servers, host, user);
    int concurrency = concurrencySizing.getThreadCount(servers.size());
    client.setConcurrency(concurrency);
    client.setTimeoutMillis(requestTimeout.toMillis());

    return new CloseableNomadClient<>(client, diagnosticServices);
  }

  @SuppressWarnings("unchecked")
  private NamedNomadServer<T> createNamedNomadServer(InetSocketAddress address, DiagnosticService diagnosticService) {
    NomadServer<T> nomadServerProxy = diagnosticService.getProxy(NomadServer.class);
    // use the <ip>:<port> for the name for nomad because server name as it was before might not be unique across the cluster.
    return new NamedNomadServer<>(address.toString(), nomadServerProxy);
  }
}
