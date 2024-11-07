/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.service.ConfigValidator;
import org.terracotta.config.service.ExtendedConfigParser;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
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
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static org.terracotta.offheapresource.OffHeapResourceIdentifier.identifier;

public class OffHeapResourceConfigurationParser implements ExtendedConfigParser {
  private static final BigInteger MAX_LONG_PLUS_ONE = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
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
    OffheapResourcesType configuration = parser().apply(elmnt);
    return toOffHeapResourcesProvider(configuration);
  }

  public ConfigValidator getConfigValidator() {
    return new OffHeapConfigValidator(parser());
  }

  public Function<Element, OffheapResourcesType> parser() {
    return (Element element) -> {
      OffheapResourcesType retValue;
      try {
        JAXBContext jaxbContext = JAXBContext.newInstance(OffheapResourcesType.class.getPackage().getName(), OffHeapResourceConfigurationParser.class.getClassLoader());
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Collection<Source> schemaSources = new ArrayList<>();
        schemaSources.add(new StreamSource(TCConfigurationParser.TERRACOTTA_XML_SCHEMA.openStream()));
        schemaSources.add(getXmlSchema());
        unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaSources.toArray(new Source[0])));
        @SuppressWarnings("unchecked")
        JAXBElement<OffheapResourcesType> parsed = (JAXBElement<OffheapResourcesType>)unmarshaller.unmarshal(element);
        retValue = parsed.getValue();
      } catch (JAXBException e) {
        throw new IllegalArgumentException(e);
      } catch (SAXException | IOException e) {
        throw new AssertionError(e);
      }
      return retValue;
    };
  }

  public static OffHeapResourcesProvider toOffHeapResourcesProvider(OffheapResourcesType configuration) {
    OffHeapResourcesProvider provider = new OffHeapResourcesProvider(emptyMap());
    for (ResourceType r : configuration.getResource()) {
      long size = longValueExact(convert(r.getValue(), r.getUnit()));
      provider.addToResources(identifier(r.getName()), size);
    }
    return provider;
  }

  static BigInteger convert(BigInteger value, MemoryUnit unit) {
    switch (unit) {
      case B:
        return value.shiftLeft(0);
      case K_B:
        return value.shiftLeft(10);
      case MB:
        return value.shiftLeft(20);
      case GB:
        return value.shiftLeft(30);
      case TB:
        return value.shiftLeft(40);
      case PB:
        return value.shiftLeft(50);
    }
    throw new IllegalArgumentException("Unknown unit " + unit);
  }

  static long longValueExact(BigInteger value) {
    if (value.compareTo(MAX_LONG_PLUS_ONE) < 0) {
      return value.longValue();
    } else {
      throw new ArithmeticException("BigInteger out of long range");
    }
  }
}
