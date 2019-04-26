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
package org.terracotta.management.model.stats;

import java.io.Serializable;

public class DelegatingSample<T extends Serializable> implements Sample<T> {

  private static final long serialVersionUID = 1L;

  private final org.terracotta.statistics.Sample<T> delegate;

  public DelegatingSample(long timestamp, T sample) {
    this.delegate = new org.terracotta.statistics.Sample<>(timestamp, sample);
  }

  @Override
  public T getSample() {
    return delegate.getSample();
  }

  @Override
  public long getTimestamp() {
    return delegate.getTimestamp();
  }

  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
