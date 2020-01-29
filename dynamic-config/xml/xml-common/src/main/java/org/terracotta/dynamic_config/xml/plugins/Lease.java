/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
