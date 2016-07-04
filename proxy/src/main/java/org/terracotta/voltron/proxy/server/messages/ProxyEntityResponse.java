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
package org.terracotta.voltron.proxy.server.messages;

import org.terracotta.entity.EntityResponse;

/**
 *
 * @author cdennis
 */
public final class ProxyEntityResponse implements EntityResponse {

  public static ProxyEntityResponse response(Class<?> type, Object reponse) {
    return new ProxyEntityResponse(type, reponse);
  }

  private final Class<?> type;
  private final Object response;

  private ProxyEntityResponse(Class<?> type, Object response) {
    this.type = type;
    this.response = response;
  }

  public Class<?> getResponseType() {
    return type;
  }

  public Object getResponse() {
    return response;
  }
}
