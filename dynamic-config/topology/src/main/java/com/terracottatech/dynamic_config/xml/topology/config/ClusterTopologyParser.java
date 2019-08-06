/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.topology.config;

import com.terracottatech.dynamic_config.xml.topology.config.xmlobjects.Cluster;
import org.terracotta.config.service.ExtendedConfigParser;
import org.w3c.dom.Element;

import com.terracottatech.dynamic_config.xml.topology.config.parser.SchemaProvider;

import java.io.IOException;
import java.net.URI;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;

public class ClusterTopologyParser implements ExtendedConfigParser {

  private static final URI NAMESPACE_URI = URI.create("http://www.terracotta.org/config/cluster");

  @Override
  public Source getXmlSchema() throws IOException {
    return SchemaProvider.getXmlSchema();
  }

  @Override
  public URI getNamespace() {
    return NAMESPACE_URI;
  }

  @Override
  public Cluster parse(final Element element, final String source) {
    Cluster cluster;
    try {
      JAXBContext jc = JAXBContext.newInstance(Cluster.class.getPackage().getName(), this.getClass().getClassLoader());
      Unmarshaller u = jc.createUnmarshaller();
      cluster = u.unmarshal(element, Cluster.class).getValue();
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }

    if (cluster == null) {
      throw new RuntimeException("cluster tag is absent in configuration");
    }

    return cluster;
  }
}