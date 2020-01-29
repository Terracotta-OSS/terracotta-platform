/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.change;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class MuxSanskritChangeTest {
  @Mock
  private SanskritChangeVisitor visitor;

  @Mock
  private SanskritChange change1;

  @Mock
  private SanskritChange change2;

  @Test
  public void noChanges() {
    MuxSanskritChange change = new MuxSanskritChange(Collections.emptyList());
    change.accept(visitor);
    verifyNoMoreInteractions(visitor);
  }

  @Test
  public void multipleChanges() {
    MuxSanskritChange change = new MuxSanskritChange(Arrays.asList(change1, change2));
    change.accept(visitor);
    verify(change1).accept(visitor);
    verify(change2).accept(visitor);
    verifyNoMoreInteractions(visitor);
  }
}
