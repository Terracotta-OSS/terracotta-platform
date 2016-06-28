/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.terracotta.management.service.monitoring;

import org.terracotta.config.service.ServiceConfigParser;
import org.w3c.dom.Element;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * @author Mathieu Carbou
 */
public class MonitoringConfigParser implements ServiceConfigParser {

  private static final URI NAMESPACE = URI.create("http://www.terracotta.org/config/management");
  private static final URL XML_SCHEMA = MonitoringConfigParser.class.getResource("/org/terracotta/management/service/monitoring/management.xsd");

  @Override
  public Source getXmlSchema() throws IOException {
    return new StreamSource(XML_SCHEMA.openStream());
  }

  @Override
  public URI getNamespace() {
    return NAMESPACE;
  }

  @Override
  public MonitoringServiceConfiguration parse(Element fragment, String source) {
    MonitoringServiceConfiguration configuration = new MonitoringServiceConfiguration();

    if (fragment.hasAttribute("debug")) {
      configuration.setDebug(Boolean.valueOf(fragment.getAttribute("debug")));
    }

    if (fragment.hasAttribute("maximumUnreadMutationsPerConsumer")) {
      configuration.setMaximumUnreadMutationsPerConsumer(Integer.valueOf(fragment.getAttribute("maximumUnreadMutationsPerConsumer")));
    }

    return configuration;
  }

}
