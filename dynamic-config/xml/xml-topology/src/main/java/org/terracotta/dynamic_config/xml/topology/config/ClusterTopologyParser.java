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
package org.terracotta.dynamic_config.xml.topology.config;

import org.terracotta.dynamic_config.xml.topology.config.xmlobjects.TcCluster;
import org.terracotta.config.service.ExtendedConfigParser;
import org.w3c.dom.Element;

import org.terracotta.dynamic_config.xml.topology.config.parser.SchemaProvider;

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
  public TcCluster parse(final Element element, final String source) {
    TcCluster cluster;
    try {
      JAXBContext jc = JAXBContext.newInstance(TcCluster.class.getPackage().getName(), this.getClass().getClassLoader());
      Unmarshaller u = jc.createUnmarshaller();
      cluster = u.unmarshal(element, TcCluster.class).getValue();
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }

    if (cluster == null) {
      throw new RuntimeException("cluster tag is absent in configuration");
    }

    return cluster;
  }
}
