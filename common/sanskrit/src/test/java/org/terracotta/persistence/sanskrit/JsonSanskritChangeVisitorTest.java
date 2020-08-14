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

public class JsonSanskritChangeVisitorTest {

  private final ObjectMapperSupplier objectMapperSupplier = ObjectMapperSupplier.notVersioned(new ObjectMapperFactory().create());

  @Test
  public void empty() throws Exception {
    JsonSanskritChangeVisitor visitor = new JsonSanskritChangeVisitor(objectMapperSupplier);
    assertEquals("{}", visitor.getJson(null));
  }

  @Test
  public void someData() throws Exception {
    SanskritObjectImpl object = new SanskritObjectImpl(ObjectMapperSupplier.notVersioned(new ObjectMapperFactory().create()));
    object.setString("E", "e");

    JsonSanskritChangeVisitor visitor = new JsonSanskritChangeVisitor(ObjectMapperSupplier.notVersioned(new ObjectMapperFactory().create()));
    visitor.setString("A", "a");
    visitor.setLong("B", 1L);
    visitor.setObject("C", object);
    visitor.removeKey("D");

    String expected = "{\"A\":\"a\",\"B\":1,\"C\":{\"E\":\"e\"},\"D\":null}";
    assertEquals(expected, visitor.getJson(null));
  }
}
