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

package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.Codec;
import org.terracotta.voltron.proxy.CommonProxyFactory;
import org.terracotta.voltron.proxy.client.messages.MessageListener;
import org.terracotta.voltron.proxy.server.messages.MessageFiring;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Alex Snaps
 */
public class ProxyInvoker<T> {

  private final T target;
  private final Codec codec;
  private final Map<Byte, Method> mappings;
  private final Map<Class, Byte> eventMappings;
  private final Set<Class> messageTypes;
  private volatile ClientCommunicator clientCommunicator;
  private final Set<ClientDescriptor> clients = new ConcurrentSkipListSet<ClientDescriptor>();

  public ProxyInvoker(Class<T> proxyType, T target, Codec codec, Class... messageTypes) {
    this.target = target;
    this.codec = codec;
    this.mappings = createMethodMappings(proxyType);
    this.messageTypes = new HashSet<Class>();
    for (Class eventType : messageTypes) {
      this.messageTypes.add(eventType);
      if(target instanceof MessageFiring) {
        ((MessageFiring)target).registerListener(eventType, new MessageListener() {
          @Override
          public void onMessage(final Object message) {
            fireMessage(clientCommunicator, message, clients); // fire to all Clients!
          }
        });
      }
    }
    this.eventMappings = createEventTypeMappings(messageTypes);
  }

  public byte[] invoke(final ClientDescriptor clientDescriptor, final byte[] arg) {
    final Method method = mappings.get(arg[0]);
    if(method == null) {
      throw new AssertionError();
    }
    final Object[] args = codec.decode(Arrays.copyOfRange(arg, 1, arg.length), method.getParameterTypes());
    try {
      final Annotation[][] allAnnotations = method.getParameterAnnotations();
      for (int i = 0; i < allAnnotations.length; i++) {
        for (Annotation parameterAnnotation : allAnnotations[i]) {
          if (parameterAnnotation.annotationType() == ClientId.class) {
            args[i] = clientDescriptor;
            break;
          }
        }
      }
      return codec.encode(method.getReturnType(), method.invoke(target, args));
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public void fireMessage(ClientCommunicator clientCommunicator, Object message, Set<ClientDescriptor> clients) {
    final Class<?> type = message.getClass();
    if(!messageTypes.contains(type)) {
      throw new IllegalArgumentException("Event type '" + type + "' isn't supported");
    }
    Set<Future<Void>> futures = new HashSet<Future<Void>>();
    for (ClientDescriptor client : clients) {
      futures.add(clientCommunicator.send(client, encode(type, message)));
    }
    boolean interrupted = false;
    while(!futures.isEmpty()) {
      for (Iterator<Future<Void>> iterator = futures.iterator(); iterator.hasNext(); ) {
        final Future<Void> future = iterator.next();
        try {
          future.get();
          iterator.remove();
        } catch (InterruptedException e) {
          interrupted = true;
        } catch (ExecutionException e) {
          iterator.remove();
          e.printStackTrace();
        }
      }
    }
    if(interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  public void fireAndForgetMessage(ClientCommunicator clientCommunicator, Object message, ClientDescriptor... clients) {
    final Class<?> type = message.getClass();
    if(!messageTypes.contains(type)) {
      throw new IllegalArgumentException("Event type '" + type + "' isn't supported");
    }
    for (ClientDescriptor client : clients) {
      clientCommunicator.sendNoResponse(client, encode(type, message));
    }
  }

  private byte[] encode(final Class type, final Object message) {

    final Byte messageTypeIdentifier = eventMappings.get(type);

    if(messageTypeIdentifier == null) {
      throw new AssertionError("WAT, no mapping for " + type.getName());
    }

    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    DataOutputStream output = new DataOutputStream(byteOut);

    try {
      output.writeByte(messageTypeIdentifier);
      output.write(codec.encode(type, message));

      output.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return byteOut.toByteArray();
  }


  static Map<Byte, Method> createMethodMappings(final Class type) {
    SortedSet<Method> methods = CommonProxyFactory.getSortedMethods(type);

    final HashMap<Byte, Method> map = new HashMap<Byte, Method>();
    byte index = 0;
    for (final Method method : methods) {
      map.put(index++, method);
    }
    return map;
  }

  static Map<Class, Byte> createEventTypeMappings(final Class... types) {
    final HashMap<Class, Byte> map = new HashMap<Class, Byte>();
    byte index = 0;
    for (Class messageType : types) {
      map.put(messageType, index++);
    }
    return map;
  }

  public void setClientCommunicator(final ClientCommunicator clientCommunicator) {
    this.clientCommunicator = clientCommunicator;
  }

  public void addClient(ClientDescriptor descriptor) {
    clients.add(descriptor);
  }

  public void removeClient(ClientDescriptor descriptor) {
    clients.remove(descriptor);
  }

}
