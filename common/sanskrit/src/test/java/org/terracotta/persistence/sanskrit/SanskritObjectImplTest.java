/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.persistence.sanskrit;

import org.junit.Test;
import org.terracotta.json.ObjectMapperFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SanskritObjectImplTest {

  private final ObjectMapperSupplier objectMapperSupplier = ObjectMapperSupplier.notVersioned(new ObjectMapperFactory().create());

  @Test
  public void setAndGet() {
    SanskritObjectImpl child = new SanskritObjectImpl(objectMapperSupplier);
    child.setString("A", "a");

    SanskritObjectImpl object = new SanskritObjectImpl(objectMapperSupplier);
    object.setString("A", "a");
    object.setLong("B", 1L);
    object.setObject("C", child);

    assertEquals("a", object.getString("A"));
    assertEquals(1L, (long) object.getLong("B"));

    assertEquals("a", object.getObject("C").getString("A"));
  }

  @Test(expected = ClassCastException.class)
  public void getLongWithStringMethod() {
    SanskritObjectImpl object = new SanskritObjectImpl(objectMapperSupplier);
    object.setLong("A", 1L);

    object.getString("A");
  }

  @Test(expected = ClassCastException.class)
  public void getStringWithLongMethod() {
    SanskritObjectImpl object = new SanskritObjectImpl(objectMapperSupplier);
    object.setString("A", "a");

    object.getLong("A");
  }

  @Test
  public void getMissingKeys() {
    SanskritObjectImpl object = new SanskritObjectImpl(objectMapperSupplier);
    assertNull(object.getString("A"));
    assertNull(object.getLong("A"));
    assertNull(object.getObject("A"));
  }

  @Test
  public void changeType() {
    SanskritObjectImpl object = new SanskritObjectImpl(objectMapperSupplier);
    object.setString("A", "a");
    object.setLong("A", 1L);
    assertEquals(1L, (long) object.getLong("A"));
  }
}
