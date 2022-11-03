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
package org.terracotta.nomad.client;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.inet.HostPort;
import org.terracotta.nomad.server.NomadServer;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@RunWith(MockitoJUnitRunner.class)
public abstract class NomadClientProcessTest {
  @Mock
  protected NomadServer<String> server1;

  @Mock
  protected NomadServer<String> server2;

  protected List<NomadEndpoint<String>> servers;

  protected HostPort address1 = HostPort.create("localhost", 9410);
  protected HostPort address2 = HostPort.create("localhost", 9411);

  @Before
  public void before() {
    servers = Stream.of(
        new NomadEndpoint<>(address1, server1),
        new NomadEndpoint<>(address2, server2)
    ).collect(toList());
  }
}
