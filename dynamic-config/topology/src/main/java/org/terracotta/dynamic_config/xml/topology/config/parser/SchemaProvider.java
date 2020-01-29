/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml.topology.config.parser;

import org.terracotta.config.service.ServiceConfigParser;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URL;

public class SchemaProvider {

  private static final URL XML_SCHEMA = ServiceConfigParser.class.getResource("/cluster-topology.xsd");

  public static Source getXmlSchema() throws IOException {
    return new StreamSource(XML_SCHEMA.openStream());
  }
}