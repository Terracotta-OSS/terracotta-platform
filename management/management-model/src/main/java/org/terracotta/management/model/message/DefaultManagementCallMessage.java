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
package org.terracotta.management.model.message;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.context.Contextual;
import org.terracotta.management.sequence.Sequence;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
public class DefaultManagementCallMessage extends DefaultMessage implements ManagementCallMessage, Serializable {

  private static final long serialVersionUID = 2L;

  private final String managementCallIdentifier;

  public DefaultManagementCallMessage(String managementCallIdentifier, Sequence sequence, String messageType, Contextual... data) {
    super(sequence, messageType, data);
    this.managementCallIdentifier = Objects.requireNonNull(managementCallIdentifier);
  }

  @Override
  public String getManagementCallIdentifier() {
    return managementCallIdentifier;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    DefaultManagementCallMessage that = (DefaultManagementCallMessage) o;

    return managementCallIdentifier.equals(that.managementCallIdentifier);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + managementCallIdentifier.hashCode();
    return result;
  }
}
