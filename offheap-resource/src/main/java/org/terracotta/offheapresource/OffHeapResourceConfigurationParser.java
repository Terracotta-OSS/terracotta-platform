/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is OffHeap Resource.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.offheapresource;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.terracotta.config.service.ServiceConfigParser;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.w3c.dom.Element;

import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.xml.sax.SAXException;

public class OffHeapResourceConfigurationParser implements ServiceConfigParser {
  
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
  public ServiceProviderConfiguration parse(Element elmnt, String string) {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(OffheapResourcesType.class.getPackage().getName(), OffHeapResourceConfigurationParser.class.getClassLoader());
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(getXmlSchema()));
      JAXBElement<OffheapResourcesType> parsed = (JAXBElement<OffheapResourcesType>) unmarshaller.unmarshal(elmnt);
      return new OffHeapResourcesConfiguration(parsed.getValue());
    } catch (JAXBException e) {
      throw new IllegalArgumentException(e);
    } catch (SAXException e) {
      throw new AssertionError(e);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

}
