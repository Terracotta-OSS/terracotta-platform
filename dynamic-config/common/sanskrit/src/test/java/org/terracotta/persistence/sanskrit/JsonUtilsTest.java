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
import org.terracotta.json.Json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.terracotta.persistence.sanskrit.MarkableLineParser.LS;

public class JsonUtilsTest {
  @Test
  public void parseEmpty() throws Exception {
    String input = "{}";

    SanskritObjectImpl result = new SanskritObjectImpl(Json.copyObjectMapper());
    JsonUtils.parse(Json.copyObjectMapper(), input, result);

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

    SanskritObjectImpl result = new SanskritObjectImpl(Json.copyObjectMapper());
    JsonUtils.parse(Json.copyObjectMapper(), input, result);

    assertEquals("a", result.getString("A"));
    assertEquals(1L, (long) result.getLong("B"));
    assertNull(result.getObject("D"));

    SanskritObject child = result.getObject("C");
    assertEquals("e", child.getString("E"));
  }
}
