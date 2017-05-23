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

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;

/**
 * @author cdennis
 */
@CommonComponent
public final class ProxyEntityResponse implements EntityResponse {

  public static ProxyEntityResponse response(MessageType messageType, Class<?> responseType, Object response) {
    return new ProxyEntityResponse(messageType, responseType, response);
  }

  public static ProxyEntityResponse messageResponse(Class<?> responseType, Object response) {
    return response(MessageType.MESSAGE, responseType, response);
  }

  public static ProxyEntityResponse syncResponse(Class<?> responseType, Object response) {
    return response(MessageType.SYNC, responseType, response);
  }

  public static ProxyEntityResponse messengerResponse(Class<?> responseType, Object response) {
    return response(MessageType.MESSENGER, responseType, response);
  }

  public static ProxyEntityResponse error(EntityUserException error) {
    return response(MessageType.ERROR, EntityUserException.class, error);
  }

  private final MessageType messageType;
  private final Class<?> responseType;
  private final Object response;

  private ProxyEntityResponse(MessageType messageType, Class<?> responseType, Object response) {
    this.messageType = messageType;
    this.responseType = responseType;
    this.response = response;
  }

  public MessageType getMessageType() {
    return messageType;
  }

  public Class<?> getResponseType() {
    return responseType;
  }

  public Object getResponse() {
    return response;
  }
}
