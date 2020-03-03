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
package org.terracotta.dynamic_config.server.nomad.persistence;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ClusterConfigFilenameTest {
  @Test
  public void testValidFilenames() {
    String[] validFilenames = {
        "A.1.xml",
        "1.10.xml",
        "@.100.xml",
        "a.9.xml",
        "node-1.19.xml",
        "server-1-abc_1234@@#*$.199.xml",
        "A.B.1.xml"
    };

    for (String fileName : validFilenames) {
      assertThat(ClusterConfigFilename.from(fileName).get().getNodeName(), notNullValue());
    }
  }

  @Test
  public void testInvalidFilenames() {
    String[] invalidFilenames = {
        "1.xml",
        " .1.xml",
        "09.xml",
        "cluster-configA1.xml",
        "cluster-configA1Bxml",
    };

    for (String fileName : invalidFilenames) {
      assertFalse(fileName, ClusterConfigFilename.from(fileName).isPresent());
    }
  }

  @Test
  public void testGetNodeName() {
    assertThat(ClusterConfigFilename.from("node-1.19.xml").get().getNodeName(), is("node-1"));
    assertThat(ClusterConfigFilename.from("server-1-abc_1234@@#*$.199.xml").get().getNodeName(), is("server-1-abc_1234@@#*$"));
  }

  @Test
  public void testGetVersion() {
    assertThat(ClusterConfigFilename.from("node-1.19.xml").get().getVersion(), is(19L));
    assertThat(ClusterConfigFilename.from("server-1-abc_1234@@#*$.199.xml").get().getVersion(), is(199L));
  }
}
