/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import com.terracottatech.dynamic_config.json.Json;
import org.junit.Test;

import static com.terracottatech.persistence.sanskrit.MarkableLineParser.LS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
