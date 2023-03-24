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
package org.terracotta.diagnostic.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.common.DiagnosticCodec;
import org.terracotta.diagnostic.common.JsonDiagnosticCodec;
import org.terracotta.diagnostic.server.api.DiagnosticServices;
import org.terracotta.diagnostic.server.api.DiagnosticServicesRegistration;
import org.terracotta.json.Json;
import org.terracotta.server.ServerJMX;
import org.terracotta.server.ServerMBean;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_DIAGNOSTIC_REQUEST_HANDLER;

/**
 * Common manager holding all diagnostic services registered on a server
 *
 * @author Mathieu Carbou
 */
public class DefaultDiagnosticServices implements DiagnosticServices, Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDiagnosticServices.class);

  private final Map<Class<?>, CompletableFuture<?>> listeners = new ConcurrentHashMap<>();

  private final DiagnosticRequestHandler handler;
  private final ServerJMX serverJMX;

  public DefaultDiagnosticServices(ServerJMX serverJMX, Json.Factory jsonFactory) {
    this(serverJMX, new JsonDiagnosticCodec(jsonFactory));
  }

  public DefaultDiagnosticServices(ServerJMX serverJMX, DiagnosticCodec<String> codec) {
    this.serverJMX = serverJMX;
    this.handler = DiagnosticRequestHandler.withCodec(codec);
  }

  public void init() {
    registerMBean(MBEAN_DIAGNOSTIC_REQUEST_HANDLER, handler);
  }

  @Override
  public void close() {
    unregisterMBean(MBEAN_DIAGNOSTIC_REQUEST_HANDLER);
    clear();
  }

  @Override
  public <T> DiagnosticServicesRegistration<T> register(Class<T> serviceInterface, T serviceImplementation) {
    DiagnosticServiceDescriptor<T> added = handler.add(
        serviceInterface,
        serviceImplementation,
        () -> unregister(serviceInterface));
    LOGGER.info("Registered Diagnostic Service: {}", serviceInterface.getName());
    fireOnService(serviceInterface, serviceImplementation);
    return added;
  }

  @Override
  public Collection<Class<?>> listServices() {
    return handler.getServices().stream().map(DiagnosticServiceDescriptor::getServiceInterface).collect(toList());
  }

  @Override
  public <T> Optional<T> findService(Class<T> serviceInterface) {
    requireNonNull(serviceInterface);
    return handler.findService(serviceInterface).map(DiagnosticServiceDescriptor::getServiceImplementation);
  }

  @Override
  public <T> CompletionStage<T> onService(Class<T> serviceInterface) {
    return getCompletableFuture(serviceInterface);
  }

  @Override
  public <T> void onService(Class<T> serviceInterface, Consumer<T> action) {
    onService(serviceInterface).thenAccept(action);
  }

  @Override
  public void clear() {
    while (!handler.getServices().isEmpty()) {
      for (DiagnosticServiceDescriptor<?> descriptor : new ArrayList<>(handler.getServices())) {
        unregister(descriptor.getServiceInterface());
      }
    }
    listeners.clear();
  }

  @Override
  public <T> void unregister(Class<T> serviceInterface) {
    requireNonNull(serviceInterface);
    DiagnosticServiceDescriptor<?> descriptor = handler.remove(serviceInterface);
    if (descriptor != null) {
      descriptor.getRegisteredMBeans().forEach(this::unregisterMBean);
    }
    listeners.remove(serviceInterface);
  }

  private void registerMBean(String name, StandardMBean mBean) {
    serverJMX.registerMBean(name, mBean);
    LOGGER.info("Registered MBean with name: {}", name);
  }

  private void unregisterMBean(String name) {
    try {
      ObjectName beanName = ServerMBean.createMBeanName(name);
      serverJMX.getMBeanServer().unregisterMBean(beanName);
      LOGGER.info("Unregistered MBean with name: {}", name);
    } catch (MalformedObjectNameException | MBeanRegistrationException e) {
      throw new AssertionError(e);
    } catch (InstanceNotFoundException ignored) {
    }
  }

  private <T> void fireOnService(Class<T> serviceInterface, T serviceImplementation) {
    CompletableFuture<T> future = getCompletableFuture(serviceInterface);
    future.complete(serviceImplementation);
  }

  @SuppressWarnings("unchecked")
  private <T> CompletableFuture<T> getCompletableFuture(Class<T> serviceInterface) {
    return (CompletableFuture<T>) listeners.computeIfAbsent(serviceInterface, aClass -> new CompletableFuture<>());
  }
}
