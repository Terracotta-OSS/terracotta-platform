/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.server;

import com.tc.classloader.CommonComponent;
import com.tc.management.TerracottaMBean;
import com.tc.management.TerracottaManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.common.DiagnosticCodec;
import org.terracotta.diagnostic.common.JsonDiagnosticCodec;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
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
@CommonComponent
public class DiagnosticServices {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticServices.class);
  private static final DiagnosticCodec<String> CODEC = new JsonDiagnosticCodec(false);
  private static final DiagnosticRequestHandler HANDLER = DiagnosticRequestHandler.withCodec(CODEC);
  private static final TerracottaMBeanGenerator GENERATOR = new TerracottaMBeanGenerator();
  private static final Map<Class<?>, CompletableFuture<?>> LISTENERS = new ConcurrentHashMap<>();

  static {
    registerMBean("DiagnosticRequestHandler", HANDLER);
  }

  public static <T> DiagnosticServicesRegistration<T> register(Class<T> serviceInterface, T serviceImplementation) {
    DiagnosticServiceDescriptor<T> added = HANDLER.add(serviceInterface, serviceImplementation);
    LOGGER.info("Registered Diagnostic Service: {}", serviceInterface.getName());
    added.discoverMBeanName().ifPresent(name -> registerMBean(name, added));
    fireOnService(serviceInterface, serviceImplementation);
    return added;
  }

  public static Collection<Class<?>> listServices() {
    return HANDLER.getServices().stream().map(DiagnosticServiceDescriptor::getServiceInterface).collect(toList());
  }

  public static <T> Optional<T> findService(Class<T> serviceInterface) {
    requireNonNull(serviceInterface);
    return HANDLER.findService(serviceInterface).map(DiagnosticServiceDescriptor::getServiceImplementation);
  }

  public static <T> CompletionStage<T> onService(Class<T> serviceInterface) {
    return getCompletableFuture(serviceInterface);
  }

  public static <T> void onService(Class<T> serviceInterface, Consumer<T> action) {
    onService(serviceInterface).thenAccept(action);
  }

  public static void clear() {
    while (!HANDLER.getServices().isEmpty()) {
      for (DiagnosticServiceDescriptor<?> descriptor : new ArrayList<>(HANDLER.getServices())) {
        unregister(descriptor.getServiceInterface());
      }
    }
    LISTENERS.clear();
  }

  public static <T> void unregister(Class<T> serviceInterface) {
    requireNonNull(serviceInterface);
    DiagnosticServiceDescriptor<?> descriptor = HANDLER.remove(serviceInterface);
    if (descriptor != null) {
      descriptor.getRegisteredMBeans().forEach(DiagnosticServices::unregisterMBean);
    }
    LISTENERS.remove(serviceInterface);
  }

  static <T> boolean registerMBean(String name, Class<T> serviceInterface) {
    DiagnosticServiceDescriptor<T> serviceDescriptor = HANDLER.findService(serviceInterface).orElse(null);
    if (serviceDescriptor == null) {
      return false;
    } else {
      registerMBean(name, serviceDescriptor);
      return true;
    }
  }

  private static <T> void registerMBean(String name, DiagnosticServiceDescriptor<T> descriptor) {
    registerMBean(name, GENERATOR.generateMBean(descriptor));
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

  private static <T> void fireOnService(Class<T> serviceInterface, T serviceImplementation) {
    CompletableFuture<T> future = getCompletableFuture(serviceInterface);
    future.complete(serviceImplementation);
  }

  @SuppressWarnings("unchecked")
  private static <T> CompletableFuture<T> getCompletableFuture(Class<T> serviceInterface) {
    return (CompletableFuture<T>) LISTENERS.computeIfAbsent(serviceInterface, aClass -> new CompletableFuture<>());
  }
}
