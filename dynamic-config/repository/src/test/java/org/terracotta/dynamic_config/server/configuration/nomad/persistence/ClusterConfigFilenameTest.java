/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ClusterConfigFilenameTest {
  @Test
  public void testValidFilenames() {
    String[] validFilenames = {
        "A.1.properties",
        "1.10.properties",
        "@.100.properties",
        "a.9.properties",
        "node-1.19.properties",
        "server-1-abc_1234@@#*$.199.properties",
        "A.B.1.properties"
    };

    for (String fileName : validFilenames) {
      assertThat(ClusterConfigFilename.from(fileName).get().getNodeName(), notNullValue());
    }
  }

  @Test
  public void testInvalidFilenames() {
    String[] invalidFilenames = {
        "1.properties",
        " .1.properties",
        "09.properties",
        "cluster-configA1.properties",
        "cluster-configA1Bxml",
    };

    for (String fileName : invalidFilenames) {
      assertFalse(fileName, ClusterConfigFilename.from(fileName).isPresent());
    }
  }

  @Test
  public void testGetNodeName() {
    assertThat(ClusterConfigFilename.from("node-1.19.properties").get().getNodeName(), is("node-1"));
    assertThat(ClusterConfigFilename.from("server-1-abc_1234@@#*$.199.properties").get().getNodeName(), is("server-1-abc_1234@@#*$"));
  }

  @Test
  public void testGetVersion() {
    assertThat(ClusterConfigFilename.from("node-1.19.properties").get().getVersion(), is(19L));
    assertThat(ClusterConfigFilename.from("server-1-abc_1234@@#*$.199.properties").get().getVersion(), is(199L));
  }
}
