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

import org.terracotta.config.dataroots.DataDirectories;
import org.terracotta.dynamic_config.server.api.XmlParser;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

/**
 * @author vmad
 */
public class DataRootConfigParser implements XmlParser<DataDirectories> {
  private static final URL XML_SCHEMA = DataRootConfigParser.class.getResource("/data-roots-config.xsd");

  @Override
  public String getNamespace() {
    return "http://www.terracottatech.com/config/data-roots";
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
  public DataDirectories parse(Element element) {
    DataDirectories dataDirectories;
    try {
      JAXBContext jc = JAXBContext.newInstance(org.terracotta.config.dataroots.ObjectFactory.class);
      Unmarshaller u = jc.createUnmarshaller();
      dataDirectories = u.unmarshal(element, DataDirectories.class).getValue();
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
    return dataDirectories;
  }
}