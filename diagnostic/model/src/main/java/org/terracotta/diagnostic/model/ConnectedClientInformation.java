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
package org.terracotta.diagnostic.model;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

public class ConnectedClientInformation {

  static final String PROP_ID = "id";
  static final String PROP_NAME = "name";
  static final String PROP_VERSION = "version";
  static final String PROP_REVISION = "revision";
  static final String PROP_IPADDRESS = "ipAddress";

  List<ClientInfo> connectedClientsInfo;

  public ConnectedClientInformation(List<ClientInfo> clients) {
    this.connectedClientsInfo = clients;
  }

  public List<ClientInfo> getConnectedClients() {
    return connectedClientsInfo;
  }

  @Override
  public String toString() {
    try {
      StringWriter sw = new StringWriter();
      toProperties().store(sw, null);
      return sw.toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Properties toProperties() {
    final String format = "clients.%d.%s";
    final int numClients = connectedClientsInfo.size();
    Properties props = new Properties();
    props.setProperty("clients.count", Integer.toString(numClients));
    for (int i = 0; i < numClients; i++) {
      ClientInfo c = connectedClientsInfo.get(i);
      props.setProperty(String.format(format, i, PROP_ID), c.getId());
      props.setProperty(String.format(format, i, PROP_NAME), c.getName());
      props.setProperty(String.format(format, i, PROP_VERSION), c.getVersion());
      props.setProperty(String.format(format, i, PROP_REVISION), c.getRevision());
      props.setProperty(String.format(format, i, PROP_IPADDRESS), c.getIpAddress());
    }
    return props;
  }

  public static ConnectedClientInformation fromProperties(String props) {
    try {
      Properties p = new Properties();
      p.load(new StringReader(props));
      return fromProperties(p);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static ConnectedClientInformation fromProperties(Properties props) {
    return new ConnectedClientInformation(range(0, parseInt(props.getProperty("clients.count")))
        .mapToObj(i ->
            new ClientInfo(Stream.of(PROP_ID, PROP_NAME, PROP_VERSION, PROP_REVISION, PROP_IPADDRESS)
                .map(k -> props.getProperty("clients." + i + "." + k))
                .toArray(String[]::new)))
        .collect(toList()));
  }
}
