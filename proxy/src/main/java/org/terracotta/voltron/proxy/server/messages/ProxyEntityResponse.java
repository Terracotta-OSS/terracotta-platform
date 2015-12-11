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
