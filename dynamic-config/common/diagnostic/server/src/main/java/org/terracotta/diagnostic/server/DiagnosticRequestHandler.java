/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.server;

import com.tc.management.AbstractTerracottaMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.common.Base64DiagnosticCodec;
import org.terracotta.diagnostic.common.DiagnosticCodec;
import org.terracotta.diagnostic.common.DiagnosticRequest;
import org.terracotta.diagnostic.common.EmptyParameterDiagnosticCodec;

import javax.management.NotCompliantMBeanException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_UNKNOWN_COMMAND;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticRequestHandler extends AbstractTerracottaMBean implements DiagnosticRequestHandlerMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticRequestHandler.class);

  private final DiagnosticCodec<String> codec;
  private final Map<String, DiagnosticServiceDescriptor<?>> services = new ConcurrentHashMap<>();

  private DiagnosticRequestHandler(DiagnosticCodec<?> codec) throws NotCompliantMBeanException {
    super(DiagnosticRequestHandlerMBean.class, false);
    // we need this chain of codecs to work around the badly written DiagnosticHandler that has some flaws in String processing.
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
  public void reset() {
  }

  @Override
  public boolean hasServiceInterface(String serviceName) {
    return findService(serviceName).isPresent();
  }

  @SuppressWarnings("unchecked")
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

  <T> DiagnosticServiceDescriptor<T> add(Class<T> serviceInterface, T serviceImplementation) {
    DiagnosticServiceDescriptor<T> service = new DiagnosticServiceDescriptor<>(serviceInterface, serviceImplementation);
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
