/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.entity.EntityMessage;
import org.terracotta.nomad.messages.MutativeMessage;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NomadEntityMessage implements EntityMessage {

  private final MutativeMessage nomadMessage;

  // For Json
  NomadEntityMessage() {
    nomadMessage = null;
  }

  public NomadEntityMessage(MutativeMessage nomadMessage) {
    this.nomadMessage = requireNonNull(nomadMessage);
  }

  public MutativeMessage getNomadMessage() {
    return nomadMessage;
  }

  @Override
  public String toString() {
    return String.valueOf(nomadMessage);
  }
}
