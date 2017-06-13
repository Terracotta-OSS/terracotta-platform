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
package org.terracotta.lease.service.config;

import org.junit.Test;
import org.terracotta.lease.service.LeaseServiceProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LeaseConfigurationParserTest {
  private static final String LEASE_NAMESPACE = "http://www.terracotta.org/service/lease";

  @Test
  public void namespace() {
    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    URI namespace = parser.getNamespace();
    assertEquals(LEASE_NAMESPACE, namespace.toString());
  }

  @Test
  public void xsd() throws Exception {
    LeaseConfigurationParser parser = new LeaseConfigurationParser();

    StreamSource xsdSource = (StreamSource) parser.getXmlSchema();

    StringBuilder stringBuilder = new StringBuilder();
    try (InputStream inputStream = xsdSource.getInputStream()) {
      int value;
      while ((value = inputStream.read()) != -1) {
        stringBuilder.append((char) value);
      }
    }

    String xsd = stringBuilder.toString();
    assertTrue(xsd.contains("<xs:import namespace=\"http://www.terracotta.org/config\"/>"));
    assertTrue(xsd.contains("<xs:element name=\"connection-leasing\" type=\"lease:connection-leasing-type\" substitutionGroup=\"tc:service-content\">"));
    assertTrue(xsd.contains("<xs:element name=\"lease-length\" type=\"lease:lease-length-type\" minOccurs=\"1\" maxOccurs=\"1\">"));
  }

  @Test
  public void parseValid() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("5000", "milliseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    LeaseConfiguration configuration = parser.parse(connectionLeasingElement, "source");

    assertEquals(5000L, configuration.getLeaseLength());
  }

  @Test(expected = NumberFormatException.class)
  public void parseNotANumber() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("BLAH", "milliseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement, "source");
  }

  @Test
  public void parseBigNumber() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("1000000000000", "milliseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    LeaseConfiguration configuration = parser.parse(connectionLeasingElement, "source");

    assertEquals(1000000000000L, configuration.getLeaseLength());
  }

  @Test(expected = NumberFormatException.class)
  public void parseTooBigNumber() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("10000000000000", "milliseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement, "source");
  }

  @Test(expected = NumberFormatException.class)
  public void parseTooBigNumberHours() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("2562048", "hours");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement, "source");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseUnknownUnits() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("1", "month");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement, "source");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseTooSmallUnits() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("1000000000", "nanoseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement, "source");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseTooBigUnits() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("1", "days");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    parser.parse(connectionLeasingElement, "source");
  }

  @Test
  public void parseMax() throws Exception {
    Element connectionLeasingElement = getXMLConfigurationElement("MAX", "milliseconds");

    LeaseConfigurationParser parser = new LeaseConfigurationParser();
    LeaseConfiguration configuration = parser.parse(connectionLeasingElement, "source");

    assertEquals(LeaseServiceProvider.MAX_LEASE_LENGTH, configuration.getLeaseLength());
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
