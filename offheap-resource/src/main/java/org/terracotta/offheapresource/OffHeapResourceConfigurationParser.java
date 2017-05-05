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
package org.terracotta.offheapresource;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.service.ExtendedConfigParser;
import org.w3c.dom.Element;

import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.xml.sax.SAXException;

public class OffHeapResourceConfigurationParser implements ExtendedConfigParser {
  
  private static final URL XML_SCHEMA = OffHeapResourceConfigurationParser.class.getResource("/offheap-resource.xsd");
  private static final URI NAMESPACE = URI.create("http://www.terracotta.org/config/offheap-resource");

  @Override
  public Source getXmlSchema() throws IOException {
    return new StreamSource(XML_SCHEMA.openStream());
  }

  @Override
  public URI getNamespace() {
    return NAMESPACE;
  }

  @Override
  public OffHeapResourcesProvider parse(Element elmnt, String string) {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(OffheapResourcesType.class.getPackage().getName(), OffHeapResourceConfigurationParser.class.getClassLoader());
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      Collection<Source> schemaSources = new ArrayList<>();
      schemaSources.add(new StreamSource(TCConfigurationParser.TERRACOTTA_XML_SCHEMA.openStream()));
      schemaSources.add(getXmlSchema());
      unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaSources.toArray(new Source[schemaSources.size()])));
      @SuppressWarnings("unchecked")
      JAXBElement<OffheapResourcesType> parsed = (JAXBElement<OffheapResourcesType>) unmarshaller.unmarshal(elmnt);
      return new OffHeapResourcesProvider(parsed.getValue());
    } catch (JAXBException e) {
      throw new IllegalArgumentException(e);
    } catch (SAXException | IOException e) {
      throw new AssertionError(e);
    }
  }

}
