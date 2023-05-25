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
package org.terracotta.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonTest {

  @Test
  public void isPretty() {
    assertFalse(new DefaultJsonFactory().create().isPretty());
    assertTrue(new DefaultJsonFactory().pretty().create().isPretty());
    assertTrue(new DefaultJsonFactory().pretty(true).create().isPretty());

    AtomicReference<ObjectMapper> om = new AtomicReference<>();
    Json json = new DefaultJsonFactory().withModule((DefaultJsonFactory.JacksonModule) om::set).create();
    assertFalse(json.isPretty());
    om.get().configure(SerializationFeature.INDENT_OUTPUT, true);
    assertTrue(json.isPretty());
  }
}