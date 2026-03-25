/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Test;
import org.terracotta.angela.client.filesystem.RemoteFile;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class SecurityLog1x2IT extends DynamicConfigIT {
  @Test
  public void test_security_logs() {
    waitUntil(
      () -> configTool("set", "-connect-to", "localhost:" + getNodePort(1, 1), "-setting", "security-log-dir=logs/security", "-auto-restart"),
      is(successful()));

    for (int i = 1; i <= 2; i++) {
      final int nodeId = i;

      waitUntil(
        () -> configTool("get", "-connect-to", "localhost:" + getNodePort(1, nodeId), "-setting", "security-log-dir", "-runtime"),
        containsOutput("security-log-dir=logs/security"));

      TerracottaServer node = getNode(1, nodeId);
      List<RemoteFile> logFiles = angela.tsa().browse(node, Paths.get("logs", "security").toString()).list();
      assertThat(logFiles, hasSize(1));
      assertThat(logFiles.get(0).getName(), startsWith("terracotta-security-log-"));
      assertThat(logFiles.get(0).getName(), endsWith(".log"));

      try {
        angela.tsa()
          .browse(node, Paths.get("logs", "security").toString())
          .downloadTo(Paths.get("build"));

        Path logFile = Paths.get("build", logFiles.get(0).getName());
        String content = Files.readString(logFile);
        Files.delete(logFile);

        assertThat(content, containsString("client connection open event"));
        assertThat(content, containsString("client connection close event"));
        assertThat(content, containsString("diagnostic handshake started"));
        assertThat(content, containsString("server node joined"));

      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}
