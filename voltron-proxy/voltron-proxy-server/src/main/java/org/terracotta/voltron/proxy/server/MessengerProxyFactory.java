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

import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
class MessengerProxyFactory {

  static <T> T createProxy(Class<T> messengerType, IEntityMessenger entityMessenger) {
    Objects.requireNonNull(messengerType);
    return messengerType.cast(Proxy.newProxyInstance(
        messengerType.getClassLoader(),
        new Class<?>[]{messengerType},
        (proxy, method, args) -> {
          final MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
          ProxyEntityMessage proxyEntityMessage = new ProxyEntityMessage(methodDescriptor, args, MessageType.MESSENGER);

          long delayMs = methodDescriptor.getDelayMs();
          if (delayMs > 0) {
            return entityMessenger.messageSelfAfterDelay(proxyEntityMessage, delayMs);
          }

          long frequencyMs = methodDescriptor.getFrequencyMs();
          if (frequencyMs > 0) {
            return entityMessenger.messageSelfPeriodically(proxyEntityMessage, frequencyMs);
          }

          entityMessenger.messageSelf(proxyEntityMessage);
          return null;
        }));
  }

}
