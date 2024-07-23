/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.terracotta.persistence.sanskrit.MarkableLineParser.LS;

public class SanskritObjectImplTest {

  private final SanskritMapper mapper = new JsonSanskritMapper();

  @Test
  public void setAndGet() throws SanskritException {
    SanskritObjectImpl child = new SanskritObjectImpl(mapper);
    child.setString("A", "a");

    SanskritObjectImpl object = new SanskritObjectImpl(mapper);
    object.setString("A", "a");
    object.setLong("B", 1L);
    object.setObject("C", child);

    assertEquals("a", object.getString("A"));
    assertEquals(1L, (long) object.getLong("B"));

    assertEquals("a", object.getObject("C").getString("A"));
  }

  @Test(expected = ClassCastException.class)
  public void getLongWithStringMethod() {
    SanskritObjectImpl object = new SanskritObjectImpl(mapper);
    object.setLong("A", 1L);

    object.getString("A");
  }

  @Test(expected = ClassCastException.class)
  public void getStringWithLongMethod() {
    SanskritObjectImpl object = new SanskritObjectImpl(mapper);
    object.setString("A", "a");

    object.getLong("A");
  }

  @Test
  public void getMissingKeys() throws SanskritException {
    SanskritObjectImpl object = new SanskritObjectImpl(mapper);
    assertNull(object.getString("A"));
    assertNull(object.getLong("A"));
    assertNull(object.getObject("A"));
  }

  @Test
  public void changeType() {
    SanskritObjectImpl object = new SanskritObjectImpl(mapper);
    object.setString("A", "a");
    object.setLong("A", 1L);
    assertEquals(1L, (long) object.getLong("A"));
  }

  @Test
  public void parseEmpty() throws Exception {
    String input = "{}";

    SanskritObjectImpl result = new SanskritObjectImpl(new JsonSanskritMapper());
    new JsonSanskritMapper().fromString(input, null, result);

    assertNull(result.getString("A"));
  }

  @Test
  public void parseData() throws Exception {
    String input = "{" + LS +
        "  \"A\" : \"a\"," + LS +
        "  \"B\" : 1," + LS +
        "  \"C\" : {" + LS +
        "    \"E\" : \"e\"" + LS +
        "  }," + LS +
        "  \"D\" : null" + LS +
        "}";

    SanskritObjectImpl result = new SanskritObjectImpl(new JsonSanskritMapper());
    new JsonSanskritMapper().fromString(input, null, result);

    assertEquals("a", result.getString("A"));
    assertEquals(1L, (long) result.getLong("B"));
    assertNull(result.getObject("D"));

    SanskritObject child = result.getObject("C");
    assertEquals("e", child.getString("E"));
  }
}
