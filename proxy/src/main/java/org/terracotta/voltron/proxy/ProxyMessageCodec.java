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

package org.terracotta.voltron.proxy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import org.terracotta.entity.MessageCodec;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityMessage;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

/**
 *
 * @author cdennis
 */
public class ProxyMessageCodec implements MessageCodec<ProxyEntityMessage, ProxyEntityResponse> {

  private final Codec codec;
  private final Map<Byte, Method> methodMappings;
  private final Map<Class<?>, Byte> responseMappings;
  
  public ProxyMessageCodec(Codec codec, Class<?> proxyType, Class<?> ... eventTypes) {
    this.codec = codec;
    this.methodMappings = CommonProxyFactory.createMethodMappings(proxyType);
    this.responseMappings = CommonProxyFactory.createResponseTypeMappings(proxyType, eventTypes);
  }

  public byte[] serialize(ProxyEntityResponse r) {
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

  public ProxyEntityMessage deserialize(final byte[] bytes) {
    final Method method = decodeMethod(bytes[0]);
    return new ProxyEntityMessage(method, decodeArgs(bytes, method.getParameterTypes()));
  }

  public byte[] serializeForSync(int concurrencyKey, ProxyEntityResponse payload) {
    throw new UnsupportedOperationException("Not supported yet."); 
  }

  public ProxyEntityMessage deserializeForSync(final int i, final byte[] bytes) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  private Method decodeMethod(final byte b) {
    final Method method = methodMappings.get(b);
    if(method == null) {
      throw new AssertionError();
    }
    return method;
  }

  private Object[] decodeArgs(final byte[] arg, final Class<?>[] parameterTypes) {
    return codec.decode(Arrays.copyOfRange(arg, 1, arg.length), parameterTypes);
  }
}
