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
package org.terracotta.management.model.cluster;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ClientIdentifierTest {

  @Test
  public void test_identifier() throws UnknownHostException {
    ClientIdentifier identifier = ClientIdentifier.create("127.0.0.1", "my-app", "uid");
    System.out.println(identifier);

    assertEquals(Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]), identifier.getPid());
    assertEquals("127.0.0.1", identifier.getHostAddress());

    assertEquals(ClientIdentifier.create("127.0.0.1", "my-app", "uid"), identifier);

    identifier = ClientIdentifier.valueOf("123@127.0.0.1:jetty:app:3");
    assertEquals("123@127.0.0.1:jetty:app:3", identifier.getClientId());
    assertEquals("123@127.0.0.1", identifier.getVmId());
    assertEquals("jetty:app", identifier.getName());
    assertEquals("3", identifier.getConnectionUid());

    identifier = ClientIdentifier.valueOf("123@[::1]:IS:app:4");
    assertEquals("123@[::1]:IS:app:4", identifier.getClientId());
    assertEquals("123@[::1]", identifier.getVmId());
    assertEquals("IS:app", identifier.getName());
    assertEquals("4", identifier.getConnectionUid());
  }

}
