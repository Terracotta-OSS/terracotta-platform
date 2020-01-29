/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.server;

import com.terracottatech.diagnostic.common.DiagnosticResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
class DiagnosticServiceDescriptor<T> implements DiagnosticServicesRegistration<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticServiceDescriptor.class);

  private final Class<T> serviceInterface;
  private final T serviceImplementation;
  private final Set<String> mBeans = ConcurrentHashMap.newKeySet();

  DiagnosticServiceDescriptor(Class<T> serviceInterface, T serviceImplementation) {
    requireNonNull(serviceInterface);
    requireNonNull(serviceImplementation);
    if (!serviceInterface.isInterface()) {
      throw new IllegalArgumentException("Not an interface: " + serviceInterface.getName());
    }
    this.serviceInterface = serviceInterface;
    this.serviceImplementation = serviceImplementation;
  }

  @Override
  public Class<T> getServiceInterface() {
    return serviceInterface;
  }

  T getServiceImplementation() {
    return serviceImplementation;
  }

  Optional<DiagnosticResponse<?>> invoke(String methodName, Object... arguments) {
    return findMethod(methodName).map(method -> {
      try {
        Object result = method.invoke(serviceImplementation, arguments);
        return new DiagnosticResponse<>(result);
      } catch (InvocationTargetException e) {
        Throwable cause = e.getTargetException();
        LOGGER.error("Failed invoking method {} on diagnostic service {}: {}", methodName, serviceInterface.getName(), cause.getMessage(), cause);
        return new DiagnosticResponse<>(null, cause);
      } catch (Exception e) {
        LOGGER.error("Failed invoking method {} on diagnostic service {}: {}", methodName, serviceInterface.getName(), e.getMessage(), e);
        return new DiagnosticResponse<>(null, e);
      }
    });
  }

  boolean matches(Class<?> serviceInterface) {
    return matches(serviceInterface.getName());
  }

  private boolean matches(String serviceInterface) {
    return Objects.equals(serviceInterface, this.serviceInterface.getName());
  }

  private Optional<Method> findMethod(String methodName) {
    List<Method> list = Stream.of(serviceInterface.getMethods())
        .filter(method -> method.getName().equals(methodName))
        .collect(Collectors.toList());
    if (list.size() > 1) {
      throw new AssertionError("Method overloading not yet supported: " + serviceInterface.getName());
    }
    return list.stream().findFirst();
  }

  Optional<String> discoverMBeanName() {
    return Optional.ofNullable(serviceImplementation.getClass().getAnnotation(Expose.class)).map(Expose::value);
  }

  void addMBean(String name) {
    mBeans.add(name);
  }

  public Set<String> getRegisteredMBeans() {
    return mBeans;
  }
}