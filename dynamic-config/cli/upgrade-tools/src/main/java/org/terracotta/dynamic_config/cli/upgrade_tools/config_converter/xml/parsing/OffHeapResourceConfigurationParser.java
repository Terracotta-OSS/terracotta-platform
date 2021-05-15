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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.xml.parsing;

import org.terracotta.config.offheapresources.OffheapResourcesType;
import org.terracotta.dynamic_config.server.api.XmlParser;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

public class OffHeapResourceConfigurationParser implements XmlParser<OffheapResourcesType> {

  private static final URL XML_SCHEMA = OffHeapResourceConfigurationParser.class.getResource("/offheap-resource.xsd");

  @Override
  public String getNamespace() {
    return "http://www.terracotta.org/config/offheap-resource";
  }

  @Override
  public Source getXmlSchema() {
    try {
      return new StreamSource(XML_SCHEMA.openStream());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public OffheapResourcesType parse(Element element) {
    OffheapResourcesType retValue;
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(org.terracotta.config.offheapresources.ObjectFactory.class);
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      Collection<Source> schemaSources = new ArrayList<>();
      schemaSources.add(new StreamSource(getClass().getResource("/terracotta.xsd").openStream()));
      schemaSources.add(getXmlSchema());
      unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaSources.toArray(new Source[0])));
      @SuppressWarnings("unchecked")
      JAXBElement<OffheapResourcesType> parsed = (JAXBElement<OffheapResourcesType>) unmarshaller.unmarshal(element);
      retValue = parsed.getValue();
    } catch (JAXBException e) {
      throw new IllegalArgumentException(e);
    } catch (SAXException | IOException e) {
      throw new AssertionError(e);
    }
    return retValue;
  }

}
