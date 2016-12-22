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

import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.voltron.proxy.MessageType;
import org.terracotta.voltron.proxy.MethodDescriptor;
import org.terracotta.voltron.proxy.ProxyEntityMessage;

import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
class SyncProxyFactory {

  private static final ThreadLocal<PassiveSynchronizationChannel<ProxyEntityMessage>> currentChannel = new ThreadLocal<>();

  static <T> T createProxy(Class<T> synchronizerType) {
    Objects.requireNonNull(synchronizerType);
    return synchronizerType.cast(Proxy.newProxyInstance(
        synchronizerType.getClassLoader(),
        new Class<?>[]{synchronizerType},
        (proxy, method, args) -> {
          final MethodDescriptor methodDescriptor = MethodDescriptor.of(method);
          ProxyEntityMessage proxyEntityMessage = new ProxyEntityMessage(methodDescriptor, args, MessageType.SYNC);
          PassiveSynchronizationChannel<ProxyEntityMessage> channel = currentChannel.get();
          if (channel == null) {
            throw new IllegalStateException("No PassiveSynchronizationChannel set to current thread");
          }
          channel.synchronizeToPassive(proxyEntityMessage);
          return null;
        }));
  }

  static void setCurrentChannel(PassiveSynchronizationChannel<ProxyEntityMessage> currentChannel) {
    SyncProxyFactory.currentChannel.set(currentChannel);
  }

  static void removeCurrentChannel() {
    SyncProxyFactory.currentChannel.remove();
  }

}
