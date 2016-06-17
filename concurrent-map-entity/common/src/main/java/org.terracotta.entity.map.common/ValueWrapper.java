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
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.entity.map.common;

import java.io.Serializable;
import java.util.Arrays;

/**
 * ValueWrapper
 */
public class ValueWrapper implements Serializable {

  private static final long serialVersionUID = -4794738044295644587L;

  private final int hashCode;
  private final byte[] value;

  public ValueWrapper(int hashCode, byte[] value) {
    this.hashCode = hashCode;
    this.value = value;
  }

  public byte[] getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ValueWrapper that = (ValueWrapper) o;

    if (hashCode != that.hashCode) return false;
    return Arrays.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }
}
