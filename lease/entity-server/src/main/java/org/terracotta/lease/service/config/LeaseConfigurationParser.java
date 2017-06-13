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
import org.w3c.dom.NodeList;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.terracotta.lease.service.LeaseServiceProvider.MAX_LEASE_LENGTH;

public class LeaseConfigurationParser implements ServiceConfigParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(LeaseConfigurationParser.class);
  private static final URL XML_SCHEMA = LeaseConfigurationParser.class.getResource("/lease-service.xsd");
  private static final String NAMESPACE_STRING = "http://www.terracotta.org/service/lease";
  private static final URI NAMESPACE_URI = URI.create(NAMESPACE_STRING);
  private static final String LEASE_LENGTH_ELEMENT_NAME =  "lease-length";
  private static final String TIME_UNIT_ATTRIBUTE_NAME =  "unit";
  private static final String MAX = "MAX";
  private static final List<TimeUnit> VALID_TIME_UNITS = Arrays.asList(MILLISECONDS, SECONDS, MINUTES, HOURS);
  private static final String VALID_TIME_UNITS_STRING = VALID_TIME_UNITS.stream().map(Object::toString).map(String::toLowerCase).collect(Collectors.joining(", "));

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

    Element leaseLengthElement = (Element) childElements.item(0);

    String leaseLengthString = leaseLengthElement.getTextContent();
    LOGGER.info("Found lease length XML text: " + leaseLengthString);

    if (leaseLengthString.compareToIgnoreCase(MAX) == 0) {
      return new LeaseConfiguration(MAX_LEASE_LENGTH);
    }

    String timeUnitString = leaseLengthElement.getAttribute(TIME_UNIT_ATTRIBUTE_NAME);
    LOGGER.info("Found lease length time unit: " + timeUnitString);

    TimeUnit timeUnit;
    try {
      timeUnit = TimeUnit.valueOf(timeUnitString.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown time unit: " + timeUnitString + ". Must be one of " + VALID_TIME_UNITS_STRING + ".", e);
    }

    if (!VALID_TIME_UNITS.contains(timeUnit)) {
      throw new IllegalArgumentException("Invalid lease time unit: " + timeUnitString + ". Must be one of " + VALID_TIME_UNITS_STRING + ".");
    }

    long leaseLength = Long.parseLong(leaseLengthString);

    long maxLeaseLengthInMatchingUnits = timeUnit.convert(MAX_LEASE_LENGTH, MILLISECONDS);
    if (leaseLength > maxLeaseLengthInMatchingUnits) {
      throw new NumberFormatException("Lease length in " + timeUnitString + " must be less than or equal to: " + maxLeaseLengthInMatchingUnits);
    }

    return new LeaseConfiguration(TimeUnit.MILLISECONDS.convert(leaseLength, timeUnit));
  }
}
