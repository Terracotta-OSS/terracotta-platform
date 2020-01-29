/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit.change;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class AddLongSanskritChangeTest {
  @Mock
  private SanskritChangeVisitor visitor;

  @Test
  public void callsSetLong() {
    AddLongSanskritChange change = new AddLongSanskritChange("key", 1L);
    change.accept(visitor);

    verify(visitor).setLong("key", 1L);
    verifyNoMoreInteractions(visitor);
  }
}
