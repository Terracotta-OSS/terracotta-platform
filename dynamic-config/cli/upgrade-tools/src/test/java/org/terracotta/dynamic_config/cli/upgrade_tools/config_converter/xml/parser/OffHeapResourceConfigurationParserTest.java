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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.parser;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.config.offheapresources.OffheapResourcesType;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.parsing.OffHeapResourceConfigurationParser;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
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
    schemaSources.add(new StreamSource(getClass().getResourceAsStream("/offheap-resource.xsd")));

    domBuilderFactory = DocumentBuilderFactory.newInstance();
    domBuilderFactory.setNamespaceAware(true);
    domBuilderFactory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaSources.toArray(new Source[0])));
  }

  @Test
  public void testValidParse() throws Exception {
    Document dom = domBuilderFactory.newDocumentBuilder().parse(getClass().getResourceAsStream("/configs/valid.xml"));

    OffheapResourcesType resourcesType = parser.parse(dom.getDocumentElement());

    assertThat(resourcesType.getResource(), hasSize(2));
    assertThat(resourcesType.getResource().get(0).getName(), is(equalTo("primary")));
    assertThat(resourcesType.getResource().get(0).getValue(), is(equalTo(BigInteger.valueOf(128))));
    assertThat(resourcesType.getResource().get(1).getName(), is(equalTo("secondary")));
    assertThat(resourcesType.getResource().get(1).getValue(), is(equalTo(BigInteger.valueOf(1024L))));
  }
}
