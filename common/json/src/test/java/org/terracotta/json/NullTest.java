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
package org.terracotta.json;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class NullTest {
  @Test
  public void test() {
    Json json = new DefaultJsonFactory().create();
    assertThat(json.toString(Json.NULL), is(equalTo("null")));
    Map<String, Object> map = new HashMap<>();
    map.put("foo", Json.NULL);
    map.put("bar", null);
    map.put("baz", "baz");
    assertThat(json.toString(map), is(equalTo("{\"baz\":\"baz\",\"foo\":null}")));
  }
}