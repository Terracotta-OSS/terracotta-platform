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
package org.terracotta.persistence.sanskrit;

import org.terracotta.persistence.sanskrit.change.SanskritChange;

public class PersistentFailSanskrit implements Sanskrit {
  private final Sanskrit underlying;
  private volatile Exception failure;

  public PersistentFailSanskrit(Sanskrit underlying) {
    this.underlying = underlying;
  }

  @Override
  public void close() throws SanskritException {
    // No failCheck()

    try {
      underlying.close();
    } catch (Exception e) {
      failure = e;
      throw e;
    }
  }

  @Override
  public String getString(String key) throws SanskritException {
    failCheck();

    try {
      return underlying.getString(key);
    } catch (Exception e) {
      failure = e;
      throw e;
    }
  }

  @Override
  public Long getLong(String key) throws SanskritException {
    failCheck();

    try {
      return underlying.getLong(key);
    } catch (Exception e) {
      failure = e;
      throw e;
    }
  }

  @Override
  public SanskritObject getObject(String key) throws SanskritException {
    failCheck();

    try {
      return underlying.getObject(key);
    } catch (Exception e) {
      failure = e;
      throw e;
    }
  }

  @Override
  public void applyChange(SanskritChange change) throws SanskritException {
    failCheck();

    try {
      underlying.applyChange(change);
    } catch (Exception e) {
      failure = e;
      throw e;
    }
  }

  @Override
  public MutableSanskritObject newMutableSanskritObject() {
    return underlying.newMutableSanskritObject();
  }

  @Override
  public void reset() throws SanskritException {
    // No failCheck()

    try {
      underlying.reset();
    } catch (Exception e) {
      failure = e;
      throw e;
    }
  }

  private void failCheck() throws SanskritException {
    if (failure != null) {
      throw new SanskritException("No longer operational due to earlier error", failure);
    }
  }
}
