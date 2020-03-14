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

import com.tc.management.TerracottaMBean;
import com.tc.management.TerracottaManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.common.DiagnosticCodec;
import org.terracotta.diagnostic.common.JsonDiagnosticCodec;
import org.terracotta.diagnostic.server.api.DiagnosticServices;
import org.terracotta.diagnostic.server.api.DiagnosticServicesRegistration;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.io.Closeable;
import java.lang.management.ManagementFactory;
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

/**
 * Common manager holding all diagnostic services registered on a server
 *
 * @author Mathieu Carbou
 */
public class DefaultDiagnosticServices implements DiagnosticServices, Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDiagnosticServices.class);

  private final Map<Class<?>, CompletableFuture<?>> listeners = new ConcurrentHashMap<>();
  private final TerracottaMBeanGenerator generator = new TerracottaMBeanGenerator();

  private final DiagnosticRequestHandler handler;

  public DefaultDiagnosticServices() {
    this(new JsonDiagnosticCodec(false));
  }

  public DefaultDiagnosticServices(DiagnosticCodec<String> codec) {
    this.handler = DiagnosticRequestHandler.withCodec(codec);
  }

  public void init() {
    registerMBean("DiagnosticRequestHandler", handler);
  }

  @Override
  public void close() {
    unregisterMBean("DiagnosticRequestHandler");
    clear();
  }

  @Override
  public <T> DiagnosticServicesRegistration<T> register(Class<T> serviceInterface, T serviceImplementation) {
    DiagnosticServiceDescriptor<T> added = handler.add(
        serviceInterface,
        serviceImplementation,
        () -> unregister(serviceInterface),
        name -> registerMBean(name, serviceInterface));
    LOGGER.info("Registered Diagnostic Service: {}", serviceInterface.getName());
    added.discoverMBeanName().ifPresent(name -> registerMBean(name, added));
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
      descriptor.getRegisteredMBeans().forEach(DefaultDiagnosticServices::unregisterMBean);
    }
    listeners.remove(serviceInterface);
  }

  <T> boolean registerMBean(String name, Class<T> serviceInterface) {
    DiagnosticServiceDescriptor<T> serviceDescriptor = handler.findService(serviceInterface).orElse(null);
    if (serviceDescriptor == null) {
      return false;
    } else {
      registerMBean(name, serviceDescriptor);
      return true;
    }
  }

  private <T> void registerMBean(String name, DiagnosticServiceDescriptor<T> descriptor) {
    registerMBean(name, generator.generateMBean(descriptor));
    descriptor.addMBean(name);
  }

  private static void registerMBean(String name, TerracottaMBean mBean) {
    try {
      ObjectName beanName = TerracottaManagement.createObjectName(null, name, TerracottaManagement.MBeanDomain.PUBLIC);
      ManagementFactory.getPlatformMBeanServer().registerMBean(mBean, beanName);
      LOGGER.info("Registered MBean with name: {}", name);
    } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
      throw new AssertionError(e);
    }
  }

  private static void unregisterMBean(String name) {
    try {
      ObjectName beanName = TerracottaManagement.createObjectName(null, name, TerracottaManagement.MBeanDomain.PUBLIC);
      ManagementFactory.getPlatformMBeanServer().unregisterMBean(beanName);
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
