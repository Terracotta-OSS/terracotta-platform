/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.change;

import com.terracottatech.json.Json;
import com.terracottatech.persistence.sanskrit.SanskritObject;
import com.terracottatech.persistence.sanskrit.SanskritObjectImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class SanskritChangeBuilderTest {
  @Mock
  private SanskritChangeVisitor visitor;

  @Test
  public void buildChange() {
    SanskritObject object = new SanskritObjectImpl(Json.copyObjectMapper());

    SanskritChange change = SanskritChangeBuilder.newChange()
        .setString("1", "a")
        .setLong("2", 1L)
        .setObject("3", object)
        .removeKey("4")
        .build();

    change.accept(visitor);

    InOrder inOrder = inOrder(visitor);

    inOrder.verify(visitor).setString("1", "a");
    inOrder.verify(visitor).setLong("2", 1L);
    inOrder.verify(visitor).setObject("3", object);
    inOrder.verify(visitor).removeKey("4");

    verifyNoMoreInteractions(visitor);
  }

  @Test
  public void duplicateKey() {
    SanskritChange change = SanskritChangeBuilder.newChange()
        .setString("1", "a")
        .setString("1", "b")
        .build();

    change.accept(visitor);

    InOrder inOrder = inOrder(visitor);

    inOrder.verify(visitor).setString("1", "a");
    inOrder.verify(visitor).setString("1", "b");

    verifyNoMoreInteractions(visitor);
  }
}
