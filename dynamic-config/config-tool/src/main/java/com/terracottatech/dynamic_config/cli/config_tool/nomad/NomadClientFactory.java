/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.config_tool.nomad;

import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import com.terracottatech.nomad.NomadEnvironment;
import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.client.NomadEndpoint;
import com.terracottatech.nomad.server.NomadServer;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class NomadClientFactory<T> {

  private final MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  private final NomadEnvironment environment;

  public NomadClientFactory(MultiDiagnosticServiceProvider multiDiagnosticServiceProvider, NomadEnvironment environment) {
    this.multiDiagnosticServiceProvider = multiDiagnosticServiceProvider;
    this.environment = environment;
  }

  @SuppressWarnings("unchecked")
  public CloseableNomadClient<T> createClient(Collection<InetSocketAddress> expectedOnlineNodes) {
    String host = environment.getHost();
    String user = environment.getUser();
    Clock clock = environment.getClock();

    // connect and concurrently open a diagnostic connection
    DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(expectedOnlineNodes);

    // build a list of endpoints, keeping the same order wanted by user
    List<NomadEndpoint<T>> nomadEndpoints = expectedOnlineNodes.stream()
        .map(address -> diagnosticServices.getDiagnosticService(address)
            .map(diagnosticService -> (NomadServer<T>) diagnosticService.getProxy(NomadServer.class))
            .map(nomadServer -> new NomadEndpoint<>(address, nomadServer))
            .get())
        .collect(toList());

    NomadClient<T> client = new NomadClient<>(nomadEndpoints, host, user, clock);

    return new CloseableNomadClient<>(client, diagnosticServices);
  }
}
