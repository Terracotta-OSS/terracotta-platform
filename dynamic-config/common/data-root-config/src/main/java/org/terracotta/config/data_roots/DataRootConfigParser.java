/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package org.terracotta.config.data_roots;

import org.terracotta.config.service.ConfigValidator;
import org.terracotta.config.service.ExtendedConfigParser;
import org.terracotta.config.util.DefaultSubstitutor;
import org.terracotta.data.config.DataDirectories;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.function.Function;

/**
 * @author vmad
 */
public class DataRootConfigParser implements ExtendedConfigParser {

  private static final URI NAMESPACE = URI.create("http://www.terracottatech.com/config/data-roots");
  private static final URL XML_SCHEMA = DataRootConfigParser.class.getResource("/data-roots-config.xsd");

  @Override
  public Source getXmlSchema() throws IOException {
    return new StreamSource(XML_SCHEMA.openStream());
  }

  @Override
  public URI getNamespace() {
    return NAMESPACE;
  }

  @Override
  public DataDirectoriesConfigImpl parse(Element element, String source) {
    DataDirectories dataDirectories = parser().apply(element);
    DefaultSubstitutor.applyDefaults(dataDirectories);
    return new DataDirectoriesConfigImpl(source, dataDirectories);
  }

  public Function<Element, DataDirectories> parser() {
    return (element -> {
      DataDirectories dataDirectories;
      try {
        JAXBContext jc = JAXBContext.newInstance(DataDirectories.class.getPackage().getName(), this.getClass()
            .getClassLoader());
        Unmarshaller u = jc.createUnmarshaller();
        dataDirectories = u.unmarshal(element, DataDirectories.class).getValue();
      } catch (JAXBException e) {
        throw new RuntimeException(e);
      }
      return dataDirectories;
    });
  }

  public ConfigValidator getConfigValidator() {
    return new DataRootValidator(parser());
  }
}