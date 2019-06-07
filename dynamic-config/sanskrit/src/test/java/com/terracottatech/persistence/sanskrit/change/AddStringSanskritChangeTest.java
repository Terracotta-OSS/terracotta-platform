/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.change;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class AddStringSanskritChangeTest {
  @Mock
  private SanskritChangeVisitor visitor;

  @Test
  public void callsSetString() {
    AddStringSanskritChange change = new AddStringSanskritChange("key", "abc");
    change.accept(visitor);

    verify(visitor).setString("key", "abc");
    verifyNoMoreInteractions(visitor);
  }
}
