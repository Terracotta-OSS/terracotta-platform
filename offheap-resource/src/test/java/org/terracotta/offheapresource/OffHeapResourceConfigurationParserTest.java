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
package org.terracotta.offheapresource;

import java.math.BigInteger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.SchemaFactory;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.ResourceType;
import org.w3c.dom.Document;

import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import org.junit.Assert;
import static org.junit.Assert.assertThat;

/**
 *
 * @author cdennis
 */
public class OffHeapResourceConfigurationParserTest {

  @Test
  public void testValidParse() throws Exception {
    OffHeapResourceConfigurationParser parser = new OffHeapResourceConfigurationParser();

    DocumentBuilderFactory domBuilderFactory = DocumentBuilderFactory.newInstance();
    domBuilderFactory.setNamespaceAware(true);
    domBuilderFactory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(parser.getXmlSchema()));
    
    Document dom = domBuilderFactory.newDocumentBuilder().parse(getClass().getResourceAsStream("/configs/valid.xml"));

    OffHeapResourcesConfiguration config = (OffHeapResourcesConfiguration) parser.parse(dom.getDocumentElement(), "what is this thing?");

    assertThat(config.getResources(), IsCollectionContaining.<ResourceType>hasItems(
            resource("primary", 128, MemoryUnit.GB),
            resource("secondary", 1024, MemoryUnit.MB)));
  }

  @Test
  public void testNoResources() throws Exception {
    OffHeapResourceConfigurationParser parser = new OffHeapResourceConfigurationParser();

    DocumentBuilderFactory domBuilderFactory = DocumentBuilderFactory.newInstance();
    domBuilderFactory.setNamespaceAware(true);
    domBuilderFactory.setValidating(true);
    domBuilderFactory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(parser.getXmlSchema()));

    Document dom = domBuilderFactory.newDocumentBuilder().parse(getClass().getResourceAsStream("/configs/no-resources.xml"));

    try {
      parser.parse(dom.getDocumentElement(), "what is this thing?");
      Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }


  private static Matcher<ResourceType> resource(String name, long size, MemoryUnit unit) {
    return allOf(hasProperty("name", is(name)), hasProperty("unit", is(unit)),
            hasProperty("value", is(BigInteger.valueOf(size))));
  }
}
