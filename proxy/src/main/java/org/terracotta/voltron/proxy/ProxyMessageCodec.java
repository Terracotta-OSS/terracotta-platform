/**
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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityMessage;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

/**
 *
 * @author cdennis
 */
public class ProxyMessageCodec implements MessageCodec<ProxyEntityMessage, ProxyEntityResponse> {

  private final Codec codec;
  private final Map<Byte, MethodDescriptor> methodMappings;
  private final Map<MethodDescriptor, Byte> reverseMethodMappings;
  
  private final Map<Class<?>, Byte> responseMappings;
  private final Map<Byte, Class<?>> reverseResponseMappings;
  
  public ProxyMessageCodec(Codec codec, Class<?> proxyType, Class<?> ... eventTypes) {
    this.codec = codec;
    this.methodMappings = CommonProxyFactory.createMethodMappings(proxyType);
    this.reverseMethodMappings = CommonProxyFactory.invert(methodMappings);
    this.responseMappings = CommonProxyFactory.createResponseTypeMappings(proxyType, eventTypes);
    this.reverseResponseMappings = CommonProxyFactory.invert(responseMappings);
  }

  @Override
  public byte[] encodeResponse(ProxyEntityResponse r) {
    final Byte messageTypeIdentifier = responseMappings.get(r.getResponseType());
    if(messageTypeIdentifier == null) {
      throw new AssertionError("WAT, no mapping for " + r.getResponseType().getName());
    }
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    DataOutputStream output = new DataOutputStream(byteOut);
    try {
      output.writeByte(messageTypeIdentifier);
      output.write(codec.encode(r.getResponseType(), r.getResponse()));
      output.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return byteOut.toByteArray();
  }

  @Override
  public ProxyEntityResponse decodeResponse(byte[] binary) throws MessageCodecException {
    byte messageTypeIdentifier = binary[0];

    Class<?> responseType = reverseResponseMappings.get(messageTypeIdentifier);

    if(responseType == null) {
      throw new AssertionError("WAT, no mapping for " + messageTypeIdentifier);
    }
    return ProxyEntityResponse.response(responseType, codec.decode(Arrays.copyOfRange(binary, 1, binary.length), responseType));
  }

  @Override
  public byte[] encodeMessage(ProxyEntityMessage message) throws MessageCodecException {
    try {
      MethodDescriptor method = message.getMethod();
      Object[] args = message.getArguments();

      final Byte methodIdentifier = reverseMethodMappings.get(method);
      
      if(methodIdentifier == null) {
        throw new AssertionError("WAT, no mapping for " + method.toGenericString());
      }

      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      DataOutputStream output = new DataOutputStream(byteOut);

      final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
      for (int i = 0, parameterAnnotationsLength = parameterAnnotations.length; i < parameterAnnotationsLength; i++) {
        final Annotation[] parameterAnnotation = parameterAnnotations[i];
        for (Annotation annotation : parameterAnnotation) {
          if (annotation.annotationType() == ClientId.class) {
            args[i] = null;
          }
        }
      }

      final Class<?>[] parameterTypes = method.getParameterTypes();
      output.writeByte(methodIdentifier);
      output.write(codec.encode(parameterTypes, args));

      output.close();
      return byteOut.toByteArray();
    } catch (IOException ex) {
      throw new MessageCodecException("Error encoding ProxyEntityMessage", ex);
    }
  }

  @Override
  public ProxyEntityMessage decodeMessage(final byte[] bytes) {
    final MethodDescriptor method = decodeMethod(bytes[0]);
    return new ProxyEntityMessage(method, decodeArgs(bytes, method.getParameterTypes()));
  }

  public byte[] serializeForSync(int concurrencyKey, ProxyEntityResponse payload) {
    throw new UnsupportedOperationException("Not supported yet."); 
  }

  public ProxyEntityMessage deserializeForSync(final int i, final byte[] bytes) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  private MethodDescriptor decodeMethod(final byte b) {
    final MethodDescriptor method = methodMappings.get(b);
    if(method == null) {
      throw new AssertionError();
    }
    return method;
  }

  private Object[] decodeArgs(final byte[] arg, final Class<?>[] parameterTypes) {
    return codec.decode(Arrays.copyOfRange(arg, 1, arg.length), parameterTypes);
  }
}
