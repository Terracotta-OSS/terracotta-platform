/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import com.terracottatech.utilities.Json;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonSanskritChangeVisitorTest {
  @Test
  public void empty() throws Exception {
    JsonSanskritChangeVisitor visitor = new JsonSanskritChangeVisitor(Json.copyObjectMapper());
    assertEquals("{}", visitor.getJson());
  }

  @Test
  public void someData() throws Exception {
    SanskritObjectImpl object = new SanskritObjectImpl();
    object.setString("E", "e");

    JsonSanskritChangeVisitor visitor = new JsonSanskritChangeVisitor(Json.copyObjectMapper());
    visitor.setString("A", "a");
    visitor.setLong("B", 1L);
    visitor.setObject("C", object);
    visitor.removeKey("D");

    String expected = "{\"A\":\"a\",\"B\":1,\"C\":{\"E\":\"e\"},\"D\":null}";
    assertEquals(expected, visitor.getJson());
  }
}
