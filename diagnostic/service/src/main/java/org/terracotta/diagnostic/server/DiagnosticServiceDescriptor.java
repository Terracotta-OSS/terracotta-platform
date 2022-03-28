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
import org.terracotta.diagnostic.common.DiagnosticResponse;
import org.terracotta.diagnostic.server.api.DiagnosticServicesRegistration;
import org.terracotta.diagnostic.server.api.Expose;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
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
  private final Runnable onClose;
  private final Function<String, Boolean> jmxExpose;

  DiagnosticServiceDescriptor(Class<T> serviceInterface, T serviceImplementation, Runnable onClose, Function<String, Boolean> jmxExpose) {
    if (!serviceInterface.isInterface()) {
      throw new IllegalArgumentException("Not an interface: " + serviceInterface.getName());
    }
    this.serviceInterface = requireNonNull(serviceInterface);
    this.serviceImplementation = requireNonNull(serviceImplementation);
    this.onClose = requireNonNull(onClose);
    this.jmxExpose = requireNonNull(jmxExpose);
  }

  @Override
  public Class<T> getServiceInterface() {
    return serviceInterface;
  }

  @Override
  public boolean exposeMBean(String name) {
    return jmxExpose.apply(name);
  }

  @Override
  public void close() {
    onClose.run();
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
