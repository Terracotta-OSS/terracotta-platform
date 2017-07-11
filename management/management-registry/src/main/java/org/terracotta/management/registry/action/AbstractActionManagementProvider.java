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
package org.terracotta.management.registry.action;

import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.descriptors.CallDescriptor;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.AbstractManagementProvider;
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.registry.Named;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractActionManagementProvider<T> extends AbstractManagementProvider<T> {

  private static final Map<String, Class<?>> PRIMITIVE_MAP = new HashMap<String, Class<?>>();
  private static final Comparator<CallDescriptor> CALL_DESCRIPTOR_COMPARATOR = new Comparator<CallDescriptor>() {
    @Override
    public int compare(CallDescriptor o1, CallDescriptor o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  static {
    for (Class<?> c : new Class<?>[]{
        void.class, boolean.class, byte.class,
        char.class, short.class, int.class,
        float.class, double.class, long.class})
      PRIMITIVE_MAP.put(c.getName(), c);
  }

  public AbstractActionManagementProvider(Class<? extends T> managedType) {
    super(managedType);
  }

  @Override
  public final Collection<? extends Descriptor> getDescriptors() {
    Collection<CallDescriptor> descriptors = new HashSet<CallDescriptor>();
    for (ExposedObject<T> o : getExposedObjects()) {
      for (Method method : o.getClass().getMethods()) {
        if (method.isAnnotationPresent(Exposed.class)) {
          List<CallDescriptor.Parameter> parameters = new ArrayList<CallDescriptor.Parameter>();
          for (MethodParameter parameter : getParameters(method)) {
            parameters.add(new CallDescriptor.Parameter(parameter.getName(), parameter.getType().getName()));
          }
          descriptors.add(new CallDescriptor(method.getName(), method.getReturnType().getName(), parameters));
        }
      }
    }
    List<CallDescriptor> list = new ArrayList<CallDescriptor>(descriptors);
    Collections.sort(list, CALL_DESCRIPTOR_COMPARATOR);
    return list;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <V> V callAction(Context context, String methodName, Class<V> returnType, Parameter... parameters) throws ExecutionException {
    ExposedObject<T> managedObject = findExposedObject(context);
    if (managedObject == null) {
      throw new IllegalArgumentException("No such managed object for context : " + context);
    }

    String[] argClassNames = new String[parameters.length];
    Object[] args = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      argClassNames[i] = parameters[i].getClassName();
      args[i] = parameters[i].getValue();
    }

    try {
      Method method = managedObject.getClass().getMethod(methodName, toClasses(managedObject.getClassLoader(), argClassNames));

      // sanity check
      if (!method.isAnnotationPresent(Exposed.class)) {
        throw new IllegalArgumentException("Method not @Exposed : " + methodName + " with arg(s) " + Arrays.toString(argClassNames));
      }
      return returnType.isPrimitive() ? (V) method.invoke(managedObject, args) : returnType.cast(method.invoke(managedObject, args));
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("No such method : " + methodName + " with arg(s) " + Arrays.toString(argClassNames), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new ExecutionException(e.getTargetException());
    }
  }

  private static Class<?>[] toClasses(ClassLoader classLoader, String[] classNames) {
    Class<?>[] classes = new Class[classNames.length];
    for (int i = 0; i < classNames.length; i++) {
      String argClassName = classNames[i];
      classes[i] = PRIMITIVE_MAP.get(argClassName);
      if (classes[i] == null) {
        try {
          classes[i] = Class.forName(argClassName, true, classLoader);
        } catch (ClassNotFoundException e) {
          throw new IllegalArgumentException("No such class name : " + argClassName, e);
        }
      }
    }
    return classes;
  }

  private static Collection<MethodParameter> getParameters(Method m) {
    Class<?>[] types = m.getParameterTypes();
    Collection<MethodParameter> parameters = new ArrayList<MethodParameter>(types.length);
    for (int i = 0; i < types.length; i++) {
      parameters.add(new MethodParameter(m, i));
    }
    return parameters;
  }

  private static class MethodParameter {

    private Method m;
    private int idx;

    public MethodParameter(Method m, int idx) {
      this.m = m;
      this.idx = idx;
    }

    String getName() {
      for (Annotation annotation : m.getParameterAnnotations()[idx]) {
        if (Named.class == annotation.annotationType()) {
          return Named.class.cast(annotation).value();
        }
      }
      return "arg" + idx;
    }

    Class<?> getType() {
      return m.getParameterTypes()[idx];
    }

  }

}
