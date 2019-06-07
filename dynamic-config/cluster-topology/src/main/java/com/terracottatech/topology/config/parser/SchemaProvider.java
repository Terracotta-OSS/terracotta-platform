/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.topology.config.parser;

import org.terracotta.config.service.ServiceConfigParser;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

public class SchemaProvider {
  public static final URI NAMESPACE_URI = URI.create("http://www.terracotta.org/config/cluster");
  public static final URL XML_SCHEMA = ServiceConfigParser.class.getResource("/cluster-topology.xsd");

  public static Source getXmlSchema() throws IOException {
    return new StreamSource(XML_SCHEMA.openStream());
  }
}