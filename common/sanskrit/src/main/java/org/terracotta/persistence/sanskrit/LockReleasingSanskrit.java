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
package org.terracotta.persistence.sanskrit;

import org.terracotta.persistence.sanskrit.change.SanskritChange;

import java.io.IOException;

import static org.terracotta.persistence.sanskrit.Owner.own;

public class LockReleasingSanskrit implements Sanskrit {
  private final Sanskrit underlying;
  private final DirectoryLock lock;

  public LockReleasingSanskrit(Sanskrit underlying, DirectoryLock lock) {
    this.underlying = underlying;
    this.lock = lock;
  }

  @Override
  @SuppressWarnings("try")
  public void close() throws SanskritException {
    try (
        Owner<DirectoryLock, IOException> lockOwner = own(lock, IOException.class);
        Owner<Sanskrit, SanskritException> sanskritOwner = own(underlying, SanskritException.class)
    ) {
      // Do nothing - the Java try-with-resources will correctly close both objects.
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  @Override
  public String getString(String key) throws SanskritException {
    return underlying.getString(key);
  }

  @Override
  public Long getLong(String key) throws SanskritException {
    return underlying.getLong(key);
  }

  @Override
  public SanskritObject getObject(String key) throws SanskritException {
    return underlying.getObject(key);
  }

  @Override
  public void applyChange(SanskritChange change) throws SanskritException {
    underlying.applyChange(change);
  }

  @Override
  public MutableSanskritObject newMutableSanskritObject() {
    return underlying.newMutableSanskritObject();
  }

  @Override
  public void reset() throws SanskritException {
    underlying.reset();
  }
}
