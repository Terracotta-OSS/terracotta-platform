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
package org.terracotta.config.data_roots;

import org.terracotta.config.service.ConfigValidator;
import org.terracotta.config.service.ExtendedConfigParser;
import org.terracotta.config.util.DefaultSubstitutor;
import org.terracotta.config.util.ParameterSubstitutor;
import org.terracotta.data.config.DataDirectories;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  public DataDirsConfigImpl parse(Element element, String source) {
    DataDirectories dataDirectories = parser().apply(element);
    DefaultSubstitutor.applyDefaults(dataDirectories);

    PathResolver pathResolver = getPathResolver(source);

    // true == skip any file IO when using this parser.
    // this parser is only used by the migration tool now
    return new DataDirsConfigImpl(ParameterSubstitutor::substitute, pathResolver, dataDirectories, true);
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

  static PathResolver getPathResolver(String source) {
    Path tempRootPath = Paths.get(".").toAbsolutePath();
    if (source != null) {
      try {
        Path sourcePath = Paths.get(source);
        if (sourcePath.isAbsolute()) {
          tempRootPath = sourcePath;
        }
      } catch (InvalidPathException e) {
        // Ignore, we keep the root as . then
      }
    }

    final IParameterSubstitutor substitutor = ParameterSubstitutor::substitute;
    return new PathResolver(tempRootPath, substitutor::substitute);
  }
}