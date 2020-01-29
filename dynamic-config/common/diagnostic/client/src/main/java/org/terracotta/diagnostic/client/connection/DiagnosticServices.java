/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.client.connection;

import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.inet.InetSocketAddressUtils;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.terracotta.common.struct.Tuple2.tuple2;

public class DiagnosticServices implements AutoCloseable {
  private final Map<InetSocketAddress, DiagnosticService> onlineEndpoints;
  private final Map<InetSocketAddress, DiagnosticServiceProviderException> offlineEndpoints;

  public DiagnosticServices(Map<InetSocketAddress, DiagnosticService> onlineEndpoints, Map<InetSocketAddress, DiagnosticServiceProviderException> offlineEndpoints) {
    this.onlineEndpoints = requireNonNull(onlineEndpoints);
    this.offlineEndpoints = requireNonNull(offlineEndpoints);
  }

  public Collection<InetSocketAddress> getOnlineEndpoints() {
    return Collections.unmodifiableSet(onlineEndpoints.keySet());
  }

  public Collection<InetSocketAddress> getOfflineEndpoints() {
    return Collections.unmodifiableSet(offlineEndpoints.keySet());
  }

  public Optional<DiagnosticServiceProviderException> getError(InetSocketAddress address) {
    return offlineEndpoints.entrySet().stream()
        .filter(e -> InetSocketAddressUtils.areEqual(e.getKey(), address))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  public Optional<DiagnosticService> getDiagnosticService(InetSocketAddress address) {
    return onlineEndpoints.entrySet().stream()
        .filter(e -> InetSocketAddressUtils.areEqual(e.getKey(), address))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  public <T> Stream<Tuple2<InetSocketAddress, T>> map(BiFunction<InetSocketAddress, DiagnosticService, T> fn) {
    return onlineEndpoints.entrySet().stream().map(e -> tuple2(e.getKey(), fn.apply(e.getKey(), e.getValue())));
  }

  @Override
  public void close() {
    onlineEndpoints.values().forEach(DiagnosticService::close);
  }
}
