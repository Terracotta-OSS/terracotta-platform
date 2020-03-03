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
package org.terracotta.dynamic_config.xml.plugins;

import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class Lease {
  private static final String LEASE_NAMESPACE = "http://www.terracotta.org/service/lease";

  private final Measure<TimeUnit> measure;

  public Lease(Measure<TimeUnit> measure) {
    this.measure = measure;
  }

  public Element toElement() {
    try {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.newDocument();

      Element connectionLeasingElement = document.createElementNS(LEASE_NAMESPACE, "connection-leasing");
      Element leaseLengthElement = document.createElementNS(LEASE_NAMESPACE, "lease-length");
      leaseLengthElement.setAttribute("unit", measure.getUnit().name().toLowerCase());

      leaseLengthElement.setTextContent(String.valueOf(measure.getQuantity()));
      connectionLeasingElement.appendChild(leaseLengthElement);

      return connectionLeasingElement;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
