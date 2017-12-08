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
package com.tc.state;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.terracotta.entity.StateDumpCollector;
/**
 *
 */
public class ObjectStateReporterTest {
  
  public ObjectStateReporterTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  @Test
  public void testCheckFormatting() throws Throwable {
    ObjectStateReporter reporter = new ObjectStateReporter(new StateDumpCollector() {
      @Override
      public StateDumpCollector subStateDumpCollector(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override
      public void addState(String string, String string1) {
        Assert.assertEquals(StateDumpCollector.JSON_STATE_KEY, string);
        System.out.println(string1);
      }
    });
    
    ObjectNode node = reporter.getBaseObject();
    node.put("test", 1);
    node.put("testString", "check");
    ObjectNode sub = node.with("subNode");
    sub.put("subIsHere", true);
    ArrayNode array = sub.withArray("array");
    for (int x=0;x<5;x++) {
      array.add(x);
    }
    reporter.finish();
  }
}
