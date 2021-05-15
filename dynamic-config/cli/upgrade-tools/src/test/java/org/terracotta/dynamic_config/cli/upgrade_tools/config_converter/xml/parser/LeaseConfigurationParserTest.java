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

import org.junit.Test;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.parsing.LeaseConfigurationParser;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.parsing.LeaseElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class LeaseConfigurationParserTest {
  private static final String LEASE_NAMESPACE = "http://www.terracotta.org/service/lease";

  @Test
  public void namespace() {
    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    assertEquals(LEASE_NAMESPACE, parser.getNamespace());
  }

  @Test
  public void parseValid() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("5000", "milliseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    LeaseElement configuration = parser.parse(connectionLeasingElement);

    assertEquals("5000", configuration.getLeaseValue());
  }

  @Test(expected = NumberFormatException.class)
  public void parseNotANumber() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("BLAH", "milliseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement);
  }

  @Test
  public void parseBigNumber() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("1000000000000", "milliseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    LeaseElement configuration = parser.parse(connectionLeasingElement);

    assertEquals("1000000000000", configuration.getLeaseValue());
  }

  @Test(expected = NumberFormatException.class)
  public void parseTooBigNumber() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("10000000000000", "milliseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement);
  }

  @Test(expected = NumberFormatException.class)
  public void parseTooBigNumberHours() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("2562048", "hours");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseUnknownUnits() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("1", "month");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseTooSmallUnits() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("1000000000", "nanoseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseTooBigUnits() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("1", "days");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement);
  }

  @Test
  public void parseMax() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("MAX", "milliseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    LeaseElement configuration = parser.parse(connectionLeasingElement);

    assertEquals(String.valueOf(TimeUnit.MILLISECONDS.convert(Long.MAX_VALUE, TimeUnit.NANOSECONDS)), configuration.getLeaseValue());
  }

  private Element getXMLConfigurationElement(String leaseLengthText, String timeUnit) throws Exception {
    DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = documentBuilder.newDocument();

    Element connectionLeasingElement = document.createElementNS(LEASE_NAMESPACE, "connection-leasing");
    Element leaseLengthElement = document.createElementNS(LEASE_NAMESPACE, "lease-length");
    leaseLengthElement.setAttribute("unit", timeUnit);

    leaseLengthElement.setTextContent(leaseLengthText);
    connectionLeasingElement.appendChild(leaseLengthElement);

    return connectionLeasingElement;
  }
}
