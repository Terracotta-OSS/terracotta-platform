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

import org.terracotta.persistence.sanskrit.change.AddLongSanskritChange;
import org.terracotta.persistence.sanskrit.change.AddObjectSanskritChange;
import org.terracotta.persistence.sanskrit.change.AddStringSanskritChange;
import org.terracotta.persistence.sanskrit.change.SanskritChange;
import org.terracotta.persistence.sanskrit.change.UnsetKeySanskritChange;

import java.io.IOException;

import static org.terracotta.persistence.sanskrit.Owner.own;

public interface Sanskrit extends AutoCloseable {

  static Sanskrit init(FilesystemDirectory filesystemDirectory, ObjectMapperSupplier objectMapperSupplier) throws SanskritException {
    try (
        Owner<DirectoryLock, IOException> lockOwner = own(filesystemDirectory.lock(), IOException.class);
        Owner<SanskritImpl, SanskritException> sanskritOwner = own(new SanskritImpl(filesystemDirectory, objectMapperSupplier), SanskritException.class)
    ) {
      return new LockReleasingSanskrit(
          new PersistentFailSanskrit(
              sanskritOwner.release()
          ),
          lockOwner.release()
      );
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  void close() throws SanskritException;

  String getString(String key) throws SanskritException;

  Long getLong(String key) throws SanskritException;

  SanskritObject getObject(String key) throws SanskritException;

  default void setString(String key, String value) throws SanskritException {
    applyChange(new AddStringSanskritChange(key, value));
  }

  default void setLong(String key, long value) throws SanskritException {
    applyChange(new AddLongSanskritChange(key, value));
  }

  default void setObject(String key, SanskritObject value) throws SanskritException {
    applyChange(new AddObjectSanskritChange(key, value));
  }

  default void removeKey(String key) throws SanskritException {
    applyChange(new UnsetKeySanskritChange(key));
  }

  void applyChange(SanskritChange change) throws SanskritException;

  MutableSanskritObject newMutableSanskritObject();

  void reset() throws SanskritException;
}
