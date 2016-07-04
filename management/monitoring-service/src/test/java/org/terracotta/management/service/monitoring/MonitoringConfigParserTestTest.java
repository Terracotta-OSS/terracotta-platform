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
package org.terracotta.management.service.monitoring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class MonitoringConfigParserTestTest {

  MonitoringConfigParser parser = new MonitoringConfigParser();
  DocumentBuilderFactory domBuilderFactory = DocumentBuilderFactory.newInstance();

  @Test
  public void test_config_1() throws ParserConfigurationException, IOException, SAXException {
    domBuilderFactory.setNamespaceAware(true);
    domBuilderFactory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(parser.getXmlSchema()));

    Document dom = domBuilderFactory.newDocumentBuilder().parse(getClass().getResourceAsStream("/sample-config-1.xml"));

    MonitoringServiceConfiguration configuration = parser.parse(dom.getDocumentElement(), "source");

    assertEquals(50, configuration.getMaximumUnreadMutationsPerConsumer());
  }

  @Test
  public void test_config_2() throws ParserConfigurationException, IOException, SAXException {
    domBuilderFactory.setNamespaceAware(true);
    domBuilderFactory.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(parser.getXmlSchema()));

    Document dom = domBuilderFactory.newDocumentBuilder().parse(getClass().getResourceAsStream("/sample-config-2.xml"));

    MonitoringServiceConfiguration configuration = parser.parse(dom.getDocumentElement(), "source");

    assertEquals(1000000, configuration.getMaximumUnreadMutationsPerConsumer());
  }

}
