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
package org.terracotta.runnel.metadata;

import org.junit.Test;
import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.decoding.fields.Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;

import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Ludovic Orban
 */
public class MetadataTest {

  @Test
  public void testEmbedding() throws Exception {
    StructField mapEntryStructField = new StructField("entry", 5, Arrays.asList(new StringField("key", 1), new StringField("val", 2), new Int64Field("longHash", 3)));
    ArrayField mapArrayField = new ArrayField("map", 5, mapEntryStructField);

    assertThat(mapArrayField.subFields().size(), is(1));
    Field f = mapArrayField.subFields().get(0);
    assertThat(f.name(), is("entry"));
    assertThat(f.index(), is(5));

    assertThat(f.subFields().size(), is(3));
    Field sf0 = f.subFields().get(0);
    assertThat(sf0.name(), is("key"));
    assertThat(sf0.index(), is(1));
    Field sf1 = f.subFields().get(1);
    assertThat(sf1.name(), is("val"));
    assertThat(sf1.index(), is(2));
    Field sf2 = f.subFields().get(2);
    assertThat(sf2.name(), is("longHash"));
    assertThat(sf2.index(), is(3));
  }

}
