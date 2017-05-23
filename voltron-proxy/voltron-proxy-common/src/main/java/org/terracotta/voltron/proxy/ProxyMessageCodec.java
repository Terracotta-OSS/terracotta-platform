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
package org.terracotta.voltron.proxy;

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.EntityUserException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author cdennis
 */
public class ProxyMessageCodec implements MessageCodec<ProxyEntityMessage, ProxyEntityResponse> {

  private final EnumMap<MessageType, Map<Byte, MethodDescriptor>> methodMappings = new EnumMap<MessageType, Map<Byte, MethodDescriptor>>(MessageType.class);
  private final EnumMap<MessageType, Map<MethodDescriptor, Byte>> reverseMethodMappings = new EnumMap<MessageType, Map<MethodDescriptor, Byte>>(MessageType.class);
  private final EnumMap<MessageType, Map<Class<?>, Byte>> responseMappings = new EnumMap<MessageType, Map<Class<?>, Byte>>(MessageType.class);
  private final EnumMap<MessageType, Map<Byte, Class<?>>> reverseResponseMappings = new EnumMap<MessageType, Map<Byte, Class<?>>>(MessageType.class);

  private Codec codec = new SerializationCodec();

  public ProxyMessageCodec(Class<?> proxyType) {
    this(proxyType, new Class[0], null, null);
  }

  public ProxyMessageCodec(Class<?> proxyType, Class<?>[] eventTypes) {
    this(proxyType, eventTypes, null, null);
  }

  public ProxyMessageCodec(Class<?> proxyType, Class<?>[] eventTypes, Class<?> messengerType, Class<?> synchronizerType) {
    // type == message
    this.methodMappings.put(MessageType.MESSAGE, CommonProxyFactory.createMethodMappings(proxyType));
    this.reverseMethodMappings.put(MessageType.MESSAGE, CommonProxyFactory.invert(methodMappings.get(MessageType.MESSAGE)));
    this.responseMappings.put(MessageType.MESSAGE, CommonProxyFactory.createResponseTypeMappings(proxyType, eventTypes));
    this.reverseResponseMappings.put(MessageType.MESSAGE, CommonProxyFactory.invert(responseMappings.get(MessageType.MESSAGE)));
    // type == sync
    if (synchronizerType != null) {
      this.methodMappings.put(MessageType.SYNC, CommonProxyFactory.createMethodMappings(synchronizerType));
      this.reverseMethodMappings.put(MessageType.SYNC, CommonProxyFactory.invert(methodMappings.get(MessageType.SYNC)));
      this.responseMappings.put(MessageType.SYNC, CommonProxyFactory.createResponseTypeMappings(synchronizerType));
      this.reverseResponseMappings.put(MessageType.SYNC, CommonProxyFactory.invert(responseMappings.get(MessageType.SYNC)));
    }
    // type == messenger
    if (messengerType != null) {
      this.methodMappings.put(MessageType.MESSENGER, CommonProxyFactory.createMethodMappings(messengerType));
      this.reverseMethodMappings.put(MessageType.MESSENGER, CommonProxyFactory.invert(methodMappings.get(MessageType.MESSENGER)));
      this.responseMappings.put(MessageType.MESSENGER, CommonProxyFactory.createResponseTypeMappings(messengerType));
      this.reverseResponseMappings.put(MessageType.MESSENGER, CommonProxyFactory.invert(responseMappings.get(MessageType.MESSENGER)));
    }
  }

  public void setCodec(Codec codec) {
    this.codec = codec;
  }

  public Codec getCodec() {
    return codec;
  }

  @Override
  public byte[] encodeResponse(ProxyEntityResponse r) {
    if (r == null) {
      return new byte[0];
    }
    MessageType messageType = r.getMessageType();
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    DataOutputStream output = new DataOutputStream(byteOut);
    try {
      output.writeByte(messageType.ordinal());
      output.writeByte(messageType == MessageType.ERROR ? 0 : getMessageTypeIdentifier(r));
      output.write(codec.encode(r.getResponseType(), r.getResponse()));
      output.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return byteOut.toByteArray();
  }

  @Override
  public ProxyEntityResponse decodeResponse(byte[] buffer) throws MessageCodecException {
    if (buffer.length == 0) {
      return null;
    }
    MessageType messageType = MessageType.values()[buffer[0]];
    Class<?> responseType = messageType == MessageType.ERROR ? EntityUserException.class : getResponseType(messageType, buffer[1]);
    Object o = codec.decode(responseType, buffer, 2, buffer.length - 2);
    return ProxyEntityResponse.response(messageType, responseType, o);
  }

  @Override
  public byte[] encodeMessage(ProxyEntityMessage message) throws MessageCodecException {
    try {
      MessageType messageType = message.getType();
      MethodDescriptor method = message.getMethod();
      Byte methodIdentifier = getMethodIdentifier(message);

      Object[] args = message.getArguments();
      final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
      for (int i = 0, parameterAnnotationsLength = parameterAnnotations.length; i < parameterAnnotationsLength; i++) {
        final Annotation[] parameterAnnotation = parameterAnnotations[i];
        for (Annotation annotation : parameterAnnotation) {
          if (annotation.annotationType() == ClientId.class) {
            args[i] = null;
          }
        }
      }

      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      DataOutputStream output = new DataOutputStream(byteOut);

      output.writeByte(messageType.ordinal()); // first, message type
      output.writeByte(methodIdentifier); // then method mapping
      output.write(codec.encode(method.getParameterTypes(), args));

      output.close();
      return byteOut.toByteArray();
    } catch (IOException ex) {
      throw new MessageCodecException("Error encoding ProxyEntityMessage", ex);
    }
  }

  @Override
  public ProxyEntityMessage decodeMessage(final byte[] buffer) {
    MessageType messageType = MessageType.values()[buffer[0]];
    MethodDescriptor method = getMethod(messageType, buffer[1]);
    return new ProxyEntityMessage(method, codec.decode(method.getParameterTypes(), buffer, 2, buffer.length - 2), messageType);
  }

  private MethodDescriptor getMethod(MessageType messageType, Byte b) {
    Map<Byte, MethodDescriptor> mapping = methodMappings.get(messageType);
    if (mapping == null) {
      throw new AssertionError("No mapping for " + messageType);
    }

    MethodDescriptor method = mapping.get(b);
    if (method == null) {
      throw new AssertionError("No mapping for method " + b + " for messageType " + messageType);
    }

    return method;
  }

  private Class<?> getResponseType(MessageType messageType, Byte b) {
    Map<Byte, Class<?>> mapping = reverseResponseMappings.get(messageType);
    if (mapping == null) {
      throw new AssertionError("No mapping for " + messageType);
    }

    Class<?> responseType = mapping.get(b);
    if (responseType == null) {
      throw new AssertionError("No mapping for method " + b + " for messageType " + messageType);
    }

    return responseType;
  }

  private Byte getMethodIdentifier(ProxyEntityMessage message) {
    Map<MethodDescriptor, Byte> mapping = reverseMethodMappings.get(message.getType());
    if (mapping == null) {
      throw new AssertionError("No mapping for " + message.getType());
    }

    Byte methodIdentifier = mapping.get(message.getMethod());
    if (methodIdentifier == null) {
      throw new AssertionError("No mapping for " + message.getMethod().toGenericString());
    }

    return methodIdentifier;
  }

  private Byte getMessageTypeIdentifier(ProxyEntityResponse response) {
    Map<Class<?>, Byte> mapping = responseMappings.get(response.getMessageType());
    if (mapping == null) {
      throw new AssertionError("No mapping for " + response.getMessageType());
    }

    Byte messageTypeIdentifier = mapping.get(response.getResponseType());
    if (messageTypeIdentifier == null) {
      throw new AssertionError("No mapping for " + response.getResponseType().getName());
    }

    return messageTypeIdentifier;
  }

}
