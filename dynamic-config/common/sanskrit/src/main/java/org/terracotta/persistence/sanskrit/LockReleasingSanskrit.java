/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
}
