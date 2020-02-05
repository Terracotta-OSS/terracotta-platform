/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
