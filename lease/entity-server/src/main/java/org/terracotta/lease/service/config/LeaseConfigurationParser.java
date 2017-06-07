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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.service.ServiceConfigParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import static org.terracotta.lease.service.LeaseServiceProvider.MAX_LEASE_LENGTH;

public class LeaseConfigurationParser implements ServiceConfigParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(LeaseConfigurationParser.class);
  private static final URL XML_SCHEMA = LeaseConfigurationParser.class.getResource("/lease-service.xsd");
  private static final String NAMESPACE_STRING = "http://www.terracotta.org/service/lease";
  private static final URI NAMESPACE_URI = URI.create(NAMESPACE_STRING);
  private static final String LEASE_LENGTH_ELEMENT_NAME =  "lease-length";
  private static final String MAX = "MAX";

  @Override
  public Source getXmlSchema() throws IOException {
    return new StreamSource(XML_SCHEMA.openStream());
  }

  /**
   * If an element matches this namespace then it will be passed to the parse method
   */
  @Override
  public URI getNamespace() {
    return NAMESPACE_URI;
  }

  @Override
  public LeaseConfiguration parse(Element element, String source) {
    NodeList childElements = element.getElementsByTagNameNS(NAMESPACE_STRING, LEASE_LENGTH_ELEMENT_NAME);

    if (childElements.getLength() != 1) {
      LOGGER.error("Found " + childElements.getLength() + " lease-length elements. The XSD should have prevented this.");
      throw new AssertionError("The schema for connection-leasing element requires one and only one lease-length element");
    }

    Node leaseLengthElement = childElements.item(0);
    String leaseLengthString = leaseLengthElement.getTextContent();
    LOGGER.info("Found lease length XML text: " + leaseLengthString);

    if (leaseLengthString.compareToIgnoreCase(MAX) == 0) {
      return new LeaseConfiguration(MAX_LEASE_LENGTH);
    }

    long leaseLength = Long.parseLong(leaseLengthString);

    if (leaseLength > MAX_LEASE_LENGTH) {
      throw new NumberFormatException("Lease length must be less than or equal to: " + MAX_LEASE_LENGTH);
    }

    return new LeaseConfiguration(leaseLength);
  }
}
