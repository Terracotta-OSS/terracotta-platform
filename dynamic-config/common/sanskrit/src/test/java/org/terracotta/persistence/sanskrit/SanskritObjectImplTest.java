/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit;

import org.junit.Test;
import org.terracotta.json.Json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SanskritObjectImplTest {
  @Test
  public void setAndGet() {
    SanskritObjectImpl child = new SanskritObjectImpl(Json.copyObjectMapper());
    child.setString("A", "a");

    SanskritObjectImpl object = new SanskritObjectImpl(Json.copyObjectMapper());
    object.setString("A", "a");
    object.setLong("B", 1L);
    object.setObject("C", child);

    assertEquals("a", object.getString("A"));
    assertEquals(1L, (long) object.getLong("B"));

    assertEquals("a", object.getObject("C").getString("A"));
  }

  @Test(expected = ClassCastException.class)
  public void getLongWithStringMethod() {
    SanskritObjectImpl object = new SanskritObjectImpl(Json.copyObjectMapper());
    object.setLong("A", 1L);

    object.getString("A");
  }

  @Test(expected = ClassCastException.class)
  public void getStringWithLongMethod() {
    SanskritObjectImpl object = new SanskritObjectImpl(Json.copyObjectMapper());
    object.setString("A", "a");

    object.getLong("A");
  }

  @Test
  public void getMissingKeys() {
    SanskritObjectImpl object = new SanskritObjectImpl(Json.copyObjectMapper());
    assertNull(object.getString("A"));
    assertNull(object.getLong("A"));
    assertNull(object.getObject("A"));
  }

  @Test
  public void changeType() {
    SanskritObjectImpl object = new SanskritObjectImpl(Json.copyObjectMapper());
    object.setString("A", "a");
    object.setLong("A", 1L);
    assertEquals(1L, (long) object.getLong("A"));
  }
}
