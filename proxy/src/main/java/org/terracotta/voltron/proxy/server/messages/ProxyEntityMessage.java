/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Connection API.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.voltron.proxy.server.messages;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.voltron.proxy.ClientId;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alex Snaps
 */
public class ProxyEntityMessage implements EntityMessage {

  private final Method method;
  private final Object[] args;

  private final AtomicBoolean consumed = new AtomicBoolean(false);

  public ProxyEntityMessage(final Method method, final Object[] args) {
    this.method = method;
    this.args = args;
  }

  public Object invoke(final Object target, final ClientDescriptor clientDescriptor) throws InvocationTargetException, IllegalAccessException {

    if(!consumed.compareAndSet(false, true)) {
      throw new IllegalStateException("Message was consumed already!");
    }

    if (clientDescriptor != null) {
      final Annotation[][] allAnnotations = method.getParameterAnnotations();
      for (int i = 0; i < allAnnotations.length; i++) {
        for (Annotation parameterAnnotation : allAnnotations[i]) {
          if (parameterAnnotation.annotationType() == ClientId.class) {
            args[i] = clientDescriptor;
            break;
          }
        }
      }
    }

    return method.invoke(target, args);
  }

  public Class<?> returnType() {
    return method.getReturnType();
  }


}
