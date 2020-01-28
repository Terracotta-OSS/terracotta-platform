/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.json.Json;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class CopyUtilsTest {
  @Mock
  private SanskritVisitor visitor1;
  @Mock
  private SanskritVisitor visitor2;
  private ObjectMapper objectMapper = Json.copyObjectMapper();

  @Test
  public void copyEmpty() {
    SanskritObjectImpl object = new SanskritObjectImpl(objectMapper);
    SanskritObject sanskritObject = CopyUtils.makeCopy(objectMapper, object);
    sanskritObject.accept(visitor1);

    verifyNoMoreInteractions(visitor1);
  }

  @Test
  public void copyData() {
    SanskritObjectImpl subObject = new SanskritObjectImpl(objectMapper);
    subObject.setString("1", "b");

    SanskritObjectImpl object = new SanskritObjectImpl(objectMapper);
    object.setString("1", "a");
    object.setLong("2", 1L);
    object.setObject("3", subObject);

    SanskritObject sanskritObject = CopyUtils.makeCopy(objectMapper, object);
    sanskritObject.accept(visitor1);
    sanskritObject.getObject("3").accept(visitor2);

    verify(visitor1).setString("1", "a");
    verify(visitor1).setLong("2", 1L);
    verify(visitor2).setString("1", "b");
  }
}
