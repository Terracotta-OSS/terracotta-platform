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
package org.terracotta.diagnostic.client.connection;

import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.inet.InetSocketAddressUtils;

import java.net.InetSocketAddress;
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

  public Map<InetSocketAddress, DiagnosticService> getOnlineEndpoints() {
    return Collections.unmodifiableMap(onlineEndpoints);
  }

  public Map<InetSocketAddress, DiagnosticServiceProviderException> getOfflineEndpoints() {
    return Collections.unmodifiableMap(offlineEndpoints);
  }

  public Optional<DiagnosticServiceProviderException> getError(InetSocketAddress address) {
    return offlineEndpoints.entrySet().stream()
        .filter(e -> InetSocketAddressUtils.areEqual(e.getKey(), address))
        .map(Map.Entry::getValue)
        .findAny();
  }

  public Optional<DiagnosticService> getDiagnosticService(InetSocketAddress address) {
    return onlineEndpoints.entrySet().stream()
        .filter(e -> InetSocketAddressUtils.areEqual(e.getKey(), address))
        .map(Map.Entry::getValue)
        .findAny();
  }

  public <T> Stream<Tuple2<InetSocketAddress, T>> map(BiFunction<InetSocketAddress, DiagnosticService, T> fn) {
    return onlineEndpoints.entrySet().stream().map(e -> tuple2(e.getKey(), fn.apply(e.getKey(), e.getValue())));
  }

  @Override
  public void close() {
    onlineEndpoints.values().forEach(DiagnosticService::close);
  }
}
