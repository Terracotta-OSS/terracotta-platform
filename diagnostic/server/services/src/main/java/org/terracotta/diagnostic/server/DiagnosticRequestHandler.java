/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.diagnostic.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.common.Base64DiagnosticCodec;
import org.terracotta.diagnostic.common.DiagnosticCodec;
import org.terracotta.diagnostic.common.DiagnosticRequest;
import org.terracotta.diagnostic.common.EmptyParameterDiagnosticCodec;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_UNKNOWN_COMMAND;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticRequestHandler extends StandardMBean implements DiagnosticRequestHandlerMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticRequestHandler.class);

  private final DiagnosticCodec<String> codec;
  private final Map<String, DiagnosticServiceDescriptor<?>> services = new ConcurrentHashMap<>();

  private DiagnosticRequestHandler(DiagnosticCodec<?> codec) throws NotCompliantMBeanException {
    // we need this chain of codecs to work around the badly written DiagnosticHandler that has some flaws in String processing.
    super(DiagnosticRequestHandlerMBean.class, false);
    this.codec = new EmptyParameterDiagnosticCodec()
        .around(new Base64DiagnosticCodec())
        .around(codec);
  }

  public DiagnosticCodec<String> getCodec() {
    return codec;
  }

  public Collection<DiagnosticServiceDescriptor<?>> getServices() {
    return services.values();
  }

  @Override
  public boolean hasServiceInterface(String serviceName) {
    return findService(serviceName).isPresent();
  }

  @Override
  public String request(String payload) {
    requireNonNull(payload);
    DiagnosticRequest request = codec.deserialize(payload, DiagnosticRequest.class);
    return findService(request.getServiceInterface().getName())
        .flatMap(diagnosticServiceDescriptor -> diagnosticServiceDescriptor.invoke(request.getMethodName(), request.getArguments()))
        .map(codec::serialize)
        .orElseGet(() -> {
          LOGGER.warn("Unable to execute diagnostic request: " + request);
          return MESSAGE_UNKNOWN_COMMAND;
        });
  }

  <T> DiagnosticServiceDescriptor<T> add(Class<T> serviceInterface, T serviceImplementation, Runnable onClose) {
    DiagnosticServiceDescriptor<T> service = new DiagnosticServiceDescriptor<>(serviceInterface, serviceImplementation, onClose);
    DiagnosticServiceDescriptor<?> previous = services.putIfAbsent(serviceInterface.getName(), service);
    if (previous == null) {
      return service;
    }
    throw new IllegalArgumentException("Service " + serviceInterface.getName() + " is already registered");
  }

  DiagnosticServiceDescriptor<?> remove(Class<?> serviceInterface) {
    return services.remove(requireNonNull(serviceInterface.getName()));
  }

  @SuppressWarnings("unchecked")
  <T> Optional<DiagnosticServiceDescriptor<T>> findService(Class<T> serviceInterface) {
    return findService(serviceInterface.getName())
        .filter(diagnosticServiceDescriptor -> diagnosticServiceDescriptor.matches(serviceInterface))
        .map(diagnosticServiceDescriptor -> (DiagnosticServiceDescriptor<T>) diagnosticServiceDescriptor);
  }

  private Optional<DiagnosticServiceDescriptor<?>> findService(String name) {
    return Optional.ofNullable(services.get(name));
  }

  static DiagnosticRequestHandler withCodec(DiagnosticCodec<?> codec) {
    try {
      return new DiagnosticRequestHandler(codec);
    } catch (NotCompliantMBeanException e) {
      throw new IllegalStateException(e);
    }
  }

}
