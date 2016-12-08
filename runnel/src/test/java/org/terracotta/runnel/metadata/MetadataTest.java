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

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Ludovic Orban
 */
public class MetadataTest {

  @Test
  public void testEmbedding() throws Exception {
    StructField mapEntryStructField = new StructField("entry", 5);
    mapEntryStructField.addField(new StringField("key", 1));
    mapEntryStructField.addField(new StringField("val", 2));
    mapEntryStructField.addField(new Int64Field("longHash", 3));
    mapEntryStructField.init();
    ArrayField mapArrayField = new ArrayField("map", 5, mapEntryStructField);

    StructField f = (StructField) mapArrayField.subField();
    assertThat(f.name(), is("entry"));
    assertThat(f.index(), is(5));

    Metadata m = f.getMetadata();

    Map<Integer, Field> integerFieldMap = m.buildFieldsByIndexMap();
    assertThat(integerFieldMap.size(), is(3));

    assertThat(integerFieldMap.get(1).name(), is("key"));
    assertThat(integerFieldMap.get(2).name(), is("val"));
    assertThat(integerFieldMap.get(3).name(), is("longHash"));

    Field sf0 = m.getFieldByName("key");
    assertThat(sf0.name(), is("key"));
    assertThat(sf0.index(), is(1));
    Field sf1 = m.getFieldByName("val");
    assertThat(sf1.name(), is("val"));
    assertThat(sf1.index(), is(2));
    Field sf2 = m.getFieldByName("longHash");
    assertThat(sf2.name(), is("longHash"));
    assertThat(sf2.index(), is(3));
  }

  @Test(expected = IllegalStateException.class)
  public void testCheckFullyInitializedThrowsWhenNotInitialized() throws Exception {
    StructField sf1 = new StructField("entry1", 1);
    StructField sf2 = new StructField("entry2", 2);

    sf1.addField(sf2);

    sf1.checkFullyInitialized();
  }

  @Test(expected = IllegalStateException.class)
  public void testCheckFullyInitializedThrowsWhenSubStructureNotInitialized() throws Exception {
    StructField sf1 = new StructField("entry1", 1);
    StructField sf2 = new StructField("entry2", 2);

    sf1.addField(sf2);

    sf1.init();

    sf1.checkFullyInitialized();
  }

  @Test
  public void testCheckFullyInitializedDoesNotThrowWhenAllStructsInitialized() throws Exception {
    StructField sf1 = new StructField("entry1", 1);
    StructField sf2 = new StructField("entry2", 2);

    sf1.addField(sf2);

    sf1.init();
    sf2.init();

    sf1.checkFullyInitialized();
  }

  @Test
  public void testCheckFullyInitializedDoesNotThrowWhenAllStructsInitializedDespiteStructLoop() throws Exception {
    StructField sf1 = new StructField("entry1", 1);
    StructField sf2 = new StructField("entry2", 2);

    // create struct loop: sf1 contains sf2 and sf2 contains sf1
    sf1.addField(sf2);
    sf2.addField(sf1);

    sf1.init();
    sf2.init();

    sf1.checkFullyInitialized();
    sf2.checkFullyInitialized();
  }

  @Test(expected = IllegalStateException.class)
  public void testAddFieldThrowsWhenInitialized() throws Exception {
    StructField mapEntryStructField = new StructField("entry", 5);
    mapEntryStructField.addField(new StringField("key", 1));
    mapEntryStructField.init();

    mapEntryStructField.addField(new StringField("val", 2));
  }

  @Test(expected = IllegalStateException.class)
  public void testTwoInitThrows() throws Exception {
    StructField mapEntryStructField = new StructField("entry", 5);
    mapEntryStructField.addField(new StringField("key", 1));
    mapEntryStructField.init();
    mapEntryStructField.init();
  }
}
