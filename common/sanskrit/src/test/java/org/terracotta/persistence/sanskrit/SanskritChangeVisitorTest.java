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
import org.terracotta.persistence.sanskrit.change.AddLongSanskritChange;
import org.terracotta.persistence.sanskrit.change.AddObjectSanskritChange;
import org.terracotta.persistence.sanskrit.change.AddStringSanskritChange;
import org.terracotta.persistence.sanskrit.change.MuxSanskritChange;
import org.terracotta.persistence.sanskrit.change.SanskritChange;
import org.terracotta.persistence.sanskrit.change.UnsetKeySanskritChange;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class SanskritChangeVisitorTest {

  private final SanskritMapper mapper = new JsonSanskritMapper();

  @Test
  public void change() throws Exception {
    SanskritChange change = new AddStringSanskritChange("a", "b");
    assertEquals("{\"a\":\"b\"}", mapper.toString(change));
  }

  @Test
  public void obj() throws SanskritException {
    SanskritObjectImpl object = new SanskritObjectImpl(mapper);
    object.setString("E", "e");

    SanskritChange change = new MuxSanskritChange(asList(
        new AddStringSanskritChange("A", "a"),
        new AddLongSanskritChange("B", 1L),
        new AddObjectSanskritChange("C", object),
        new UnsetKeySanskritChange("D")

    ));

    String expected = "{\"A\":\"a\",\"B\":1,\"C\":{\"E\":\"e\"},\"D\":null}";
    assertEquals(expected, mapper.toString(change));
  }
}
