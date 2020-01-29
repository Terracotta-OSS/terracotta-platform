/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.terracotta.persistence.sanskrit.change.AddLongSanskritChange;
import org.terracotta.persistence.sanskrit.change.AddObjectSanskritChange;
import org.terracotta.persistence.sanskrit.change.AddStringSanskritChange;
import org.terracotta.persistence.sanskrit.change.SanskritChange;
import org.terracotta.persistence.sanskrit.change.UnsetKeySanskritChange;

import java.io.IOException;

import static org.terracotta.persistence.sanskrit.Owner.own;

public interface Sanskrit extends AutoCloseable {

  static Sanskrit init(FilesystemDirectory filesystemDirectory, ObjectMapper objectMapper) throws SanskritException {
    try (
        Owner<DirectoryLock, IOException> lockOwner = own(filesystemDirectory.lock(), IOException.class);
        Owner<SanskritImpl, SanskritException> sanskritOwner = own(new SanskritImpl(filesystemDirectory, objectMapper), SanskritException.class)
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
}
