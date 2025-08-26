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
package org.terracotta.management.integration.tests;

import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.DiagnosticsFactory;
import org.terracotta.connection.entity.EntityRef;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;
import org.terracotta.connection.Diagnostics;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticIT extends AbstractSingleTest {

  @Test
  public void cluster_state_dump() throws Exception {
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "10000");
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, "diagnostic");
    properties.setProperty(DiagnosticsFactory.REQUEST_TIMEOUT, "10000");
    properties.setProperty(DiagnosticsFactory.REQUEST_TIMEOUT_MESSAGE, "10000");
    URI uri = URI.create("diagnostic://" + voltron.getConnectionURI().getAuthority());

    while (!Thread.currentThread().isInterrupted()) {

      try (Connection connection = ConnectionFactory.connect(uri, properties)) {
        EntityRef<Diagnostics, Object, Void> ref = connection.getEntityRef(Diagnostics.class, 1, "root");
        Diagnostics diagnostics = ref.fetchEntity(null);
        String dump = diagnostics.getClusterState();
        System.out.println(dump);
        try (Stream<String> lines = Files.lines(Paths.get(getClass().getResource("/state-dump-partial.txt").toURI()))) {
          if (lines.allMatch(line -> {
            boolean match = containsString(line).matches(dump);
            if (!match) {
              System.out.println("==> MISSING: " + line);
            }
            return match;
          })) {
            return;
          }
        }
      }

      try {
        Thread.sleep(1_000);
      } catch (InterruptedException e) {
        fail("interrupted");
      }
    }

    try (Connection connection = ConnectionFactory.connect(uri, properties)) {
      EntityRef<Diagnostics, Object, Void> ref = connection.getEntityRef(Diagnostics.class, 1, "root");
      Diagnostics diagnostics = ref.fetchEntity(null);
      String dump = diagnostics.getClusterState();
//      System.out.println(dump);
      try (Stream<String> lines = Files.lines(Paths.get(getClass().getResource("/sate-dump-partial.txt").toURI()))) {
        lines.forEach(line -> {
          //System.out.println(line);
          assertThat("Did not find line '" + line + "' in the dump", dump, containsString(line));
        });
      }
    }
  }

}
