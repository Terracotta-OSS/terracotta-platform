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
package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.IEntityMessenger;
import org.terracotta.voltron.proxy.MessageType;
import org.terracotta.voltron.proxy.MethodDescriptor;
import org.terracotta.voltron.proxy.ProxyEntityMessage;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Mathieu Carbou
 */
class MessengerProxyFactory {

  private static final Method close;

  static {
    try {
      close = Messenger.class.getDeclaredMethod("unSchedule");
    } catch (NoSuchMethodException e) {
      throw new AssertionError("Someone changed some method signature here!!!");
    }
  }

  static <T extends Messenger> T createProxy(Class<T> messengerType, IEntityMessenger entityMessenger) {
    Objects.requireNonNull(messengerType);
    List<IEntityMessenger.ScheduledToken> tokens = new CopyOnWriteArrayList<>();
    return messengerType.cast(Proxy.newProxyInstance(
        messengerType.getClassLoader(),
        new Class<?>[]{messengerType},
        (proxy, method, args) -> {

          // close() call
          if (close.equals(method)) {
            tokens.forEach(entityMessenger::cancelTimedMessage);
            return null;
          }

          MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
          ProxyEntityMessage proxyEntityMessage = new ProxyEntityMessage(methodDescriptor, args, MessageType.MESSENGER);

          // schedule method call after a delay
          long delayMs = methodDescriptor.getDelayMs();
          if (delayMs > 0) {
            return entityMessenger.messageSelfAfterDelay(proxyEntityMessage, delayMs);
          }

          // schedule method call at a specific frequency
          long frequencyMs = methodDescriptor.getFrequencyMs();
          if (frequencyMs > 0) {
            IEntityMessenger.ScheduledToken token = entityMessenger.messageSelfPeriodically(proxyEntityMessage, frequencyMs);
            tokens.add(token);
            return token;
          }

          // just send once
          entityMessenger.messageSelf(proxyEntityMessage);
          return null;
        }));
  }

}
