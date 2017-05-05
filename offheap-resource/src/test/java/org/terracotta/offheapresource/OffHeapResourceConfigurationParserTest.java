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
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.w3c.dom.Document;

import static org.hamcrest.core.Is.is;
import org.junit.Assert;
import static org.junit.Assert.assertThat;
import static org.terracotta.offheapresource.OffHeapResourceIdentifier.identifier;
import static org.terracotta.offheapresource.OffHeapResourcesProvider.convert;
import static org.terracotta.offheapresource.OffHeapResourcesProvider.longValueExact;

/**
 *
 * @author cdennis
 */
public class OffHeapResourceConfigurationParserTest {

  private OffHeapResourceConfigurationParser parser;
  private DocumentBuilderFactory domBuilderFactory;

  @Before
  public void setUp() throws Exception {
    parser = new OffHeapResourceConfigurationParser();

    Collection<Source> schemaSources = new ArrayList<>();
    schemaSources.add(new StreamSource(getClass().getResourceAsStream("/terracotta.xsd")));
    schemaSources.add(parser.getXmlSchema());

    domBuilderFactory = DocumentBuilderFactory.newInstance();
    domBuilderFactory.setNamespaceAware(true);
    domBuilderFactory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaSources.toArray(new Source[schemaSources.size()])));
  }

  @Test
  public void testValidParse() throws Exception {
    Document dom = domBuilderFactory.newDocumentBuilder().parse(getClass().getResourceAsStream("/configs/valid.xml"));

    OffHeapResourcesProvider config = parser.parse(dom.getDocumentElement(), "what is this thing?");

    assertThat(config.getOffHeapResource(identifier("primary")).available(), is(longValueExact(convert(BigInteger.valueOf(128L), MemoryUnit.GB))));
    assertThat(config.getOffHeapResource(identifier("secondary")).available(), is(longValueExact(convert(BigInteger.valueOf(1024L), MemoryUnit.MB))));
  }

  @Test
  public void testNoResources() throws Exception {
    Document dom = domBuilderFactory.newDocumentBuilder().parse(getClass().getResourceAsStream("/configs/no-resources.xml"));

    try {
      parser.parse(dom.getDocumentElement(), "what is this thing?");
      Assert.fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //expected
    }
  }
}
