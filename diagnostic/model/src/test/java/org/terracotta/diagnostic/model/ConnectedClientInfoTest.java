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

import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ConnectedClientInfoTest {

  @Test
  public void parse() {

    Properties props = new Properties();

    props.setProperty("clients.0.id", "ddf594de4b4c4ec79b4e275a3eb82b5e");
    props.setProperty("clients.0.name", "TMS:MyCluster");
    props.setProperty("clients.0.version", "10.7.0-SNAPSHOT");
    props.setProperty("clients.0.revision", "3b5da0ab6ff7754c7da436651da2dee6ca16de78");
    props.setProperty("clients.0.ipAddress", "host1:62497");
    props.setProperty("clients.count", "1");

    ConnectedClientInformation info = ConnectedClientInformation.fromProperties(props);

    assertThat(info.connectedClientsInfo.size(), equalTo(1));
    ClientInfo c = info.connectedClientsInfo.get(0);
    assertThat(c.getName(), equalTo("TMS:MyCluster"));
    assertThat(c.getVersion(), equalTo("10.7.0-SNAPSHOT"));
    assertThat(c.getRevision(), equalTo("3b5da0ab6ff7754c7da436651da2dee6ca16de78"));
    assertThat(c.getIpAddress(), equalTo("host1:62497"));
  }

  @Test
  public void parse_convert() {

    Properties props = new Properties();

    props.setProperty("clients.0.id", "ddf594de4b4c4ec79b4e275a3eb82b5e");
    props.setProperty("clients.0.name", "TMS:MyCluster");
    props.setProperty("clients.0.version", "10.7.0-SNAPSHOT");
    props.setProperty("clients.0.revision", "3b5da0ab6ff7754c7da436651da2dee6ca16de78");
    props.setProperty("clients.0.ipAddress", "host1:62497");

    props.setProperty("clients.1.id", "c0393a4de644415a86d33880c747255f");
    props.setProperty("clients.1.name", "Store:TC Client");
    props.setProperty("clients.1.version", "10.7.0-SNAPSHOT");
    props.setProperty("clients.1.revision", "3b5da0ab6ff7754c7da436651da2dee6ca16de78");
    props.setProperty("clients.1.ipAddress", "host1:62515");

    props.setProperty("clients.count", "2");

    ConnectedClientInformation clientInfo = ConnectedClientInformation.fromProperties(props);
    assertThat(clientInfo.connectedClientsInfo.size(), equalTo(2));

    String propsAsString = clientInfo.toString();
    clientInfo = ConnectedClientInformation.fromProperties(propsAsString);
    assertThat(clientInfo.connectedClientsInfo.size(), equalTo(2));
  }
}