/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.nomad.entity.common;

import org.terracotta.entity.EntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NomadEntityResponse implements EntityResponse {

  private final AcceptRejectResponse response;

  // For Json
  NomadEntityResponse() {
    response = null;
  }

  public NomadEntityResponse(AcceptRejectResponse response) {
    this.response = requireNonNull(response);
  }

  public AcceptRejectResponse getResponse() {
    return response;
  }

  @Override
  public String toString() {
    return String.valueOf(response);
  }
}
