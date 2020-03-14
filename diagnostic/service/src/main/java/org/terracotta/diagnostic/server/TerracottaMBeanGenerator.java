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

import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.TerracottaMBean;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;

import java.lang.reflect.Method;

import static java.util.Objects.requireNonNull;
import static net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy.Default.NO_CONSTRUCTORS;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author Mathieu Carbou
 */
class TerracottaMBeanGenerator {

  <T> TerracottaMBean generateMBean(DiagnosticServiceDescriptor<T> descriptor) {
    requireNonNull(descriptor);
    return generateMBean(descriptor.getServiceInterface(), descriptor.getServiceImplementation());
  }

  <T> TerracottaMBean generateMBean(Class<T> serviceInterface, T serviceImplementation) {
    Class<?> mBeanInterface = generateMBeanInterface(serviceInterface);
    return generateMBeanImplementation(mBeanInterface, serviceInterface, serviceImplementation);
  }

  <T> TerracottaMBean generateMBeanImplementation(Class<?> mBeanInterface, Class<T> serviceInterface, T serviceImplementation) {
    requireNonNull(mBeanInterface);
    requireNonNull(serviceInterface);
    requireNonNull(serviceImplementation);
    try {
      DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<AbstractTerracottaMBean> bb = new ByteBuddy()
          .subclass(AbstractTerracottaMBean.class, NO_CONSTRUCTORS)
          .implement(TerracottaMBean.class, serviceInterface, mBeanInterface)
          .name(serviceInterface.getName() + "MBeanImpl")
          // ctor
          .defineConstructor(Visibility.PUBLIC)
          .intercept(MethodCall.invoke(AbstractTerracottaMBean.class.getDeclaredConstructor(Class.class, boolean.class)).with(mBeanInterface, false))
          // reset()
          .define(TerracottaMBean.class.getMethod("reset"))
          .intercept(StubMethod.INSTANCE)
          // toString()
          .method(named("toString"))
          .intercept(FixedValue.value("Mbean: " + serviceInterface.getName()));
      // delegates to diagnostic service implementation
      for (Method method : serviceInterface.getMethods()) {
        bb = bb.defineMethod(method.getName(), method.getReturnType(), method.getModifiers())
            .withParameters(method.getParameterTypes())
            .intercept(MethodDelegation.to(serviceImplementation));
      }
      return bb
          .make()
          .load(mBeanInterface.getClassLoader())
          .getLoaded()
          .newInstance();
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  <T> Class<? extends TerracottaMBean> generateMBeanInterface(Class<T> serviceInterface) {
    requireNonNull(serviceInterface);
    return (Class<? extends TerracottaMBean>) new ByteBuddy()
        .makeInterface(TerracottaMBean.class, serviceInterface)
        .name(serviceInterface.getName() + "MBean")
        .make()
        .load(getClass().getClassLoader())
        .getLoaded();
  }

}
