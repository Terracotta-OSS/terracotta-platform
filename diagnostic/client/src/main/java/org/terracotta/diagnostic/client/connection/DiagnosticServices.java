/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.terracotta.common.struct.Tuple2.tuple2;

public class DiagnosticServices<K> implements AutoCloseable {
  private final Map<K, DiagnosticService> onlineEndpoints;
  private final Map<K, DiagnosticServiceProviderException> failedEndpoints;

  public DiagnosticServices(Map<K, DiagnosticService> onlineEndpoints, Map<K, DiagnosticServiceProviderException> failedEndpoints) {
    this.onlineEndpoints = requireNonNull(onlineEndpoints);
    this.failedEndpoints = requireNonNull(failedEndpoints);
  }

  public Map<K, DiagnosticService> getOnlineEndpoints() {
    return Collections.unmodifiableMap(onlineEndpoints);
  }

  public Optional<DiagnosticService> findAnyOnlineDiagnosticService() {
    return Optional.ofNullable(onlineEndpoints.isEmpty() ? null : onlineEndpoints.values().iterator().next());
  }

  public Map<K, DiagnosticServiceProviderException> getFailedEndpoints() {
    return Collections.unmodifiableMap(failedEndpoints);
  }

  public Optional<DiagnosticServiceProviderException> getFailure(K id) {
    return failedEndpoints.entrySet().stream()
        .filter(e -> e.getKey().equals(id))
        .map(Map.Entry::getValue)
        .findAny();
  }

  public Optional<DiagnosticService> getDiagnosticService(K id) {
    return onlineEndpoints.entrySet().stream()
        .filter(e -> e.getKey().equals(id))
        .map(Map.Entry::getValue)
        .findAny();
  }

  public <T> Stream<Tuple2<K, T>> map(BiFunction<K, DiagnosticService, T> fn) {
    return onlineEndpoints.entrySet().stream().map(e -> tuple2(e.getKey(), fn.apply(e.getKey(), e.getValue())));
  }

  @Override
  public void close() {
    onlineEndpoints.values().forEach(DiagnosticService::close);
  }
}
