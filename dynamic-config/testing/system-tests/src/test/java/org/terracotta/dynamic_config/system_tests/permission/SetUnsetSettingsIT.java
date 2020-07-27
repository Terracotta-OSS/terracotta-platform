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
package org.terracotta.dynamic_config.system_tests.permission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.terracotta.persistence.sanskrit.SanskritObject;
import org.terracotta.persistence.sanskrit.SanskritImpl;
import org.terracotta.persistence.sanskrit.SanskritObjectImpl;
import org.terracotta.persistence.sanskrit.MutableSanskritObject;
import org.terracotta.persistence.sanskrit.JsonUtils;
import org.terracotta.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 1)
public class SetUnsetSettingsIT extends DynamicConfigIT {
  @Test
  public void testUnsetOffHeapAtClusterLevelAfterActivate() throws IOException, SanskritException {
    activateCluster();
    assertThat(
        () -> invokeConfigTool("unset", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        exceptionMatcher("Reason: Setting 'offheap-resources' cannot be unset when node is activated"));
    assertChanges(3);
  }

  @Test
  public void testUnsetOffHeapAtNodeLevelAfterActivate() throws IOException, SanskritException {
    activateCluster();
    assertThat(
        () -> invokeConfigTool("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.offheap-resources.main"),
        exceptionMatcher("Reason: Setting 'offheap-resources' does not allow any operation at node level"));
    assertChanges(3);
  }

  @Test
  public void testUnSetOffHeapAtClusterLevelBeforeActivate() throws IOException, SanskritException {
    invokeConfigTool("unset", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main");
    activateCluster();
    assertThat(
        invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB"),
        is(successful()));
    assertChanges(5);
  }

  @Test
  public void testUnsetDataDirAtClusterLevelAfterActivate() throws IOException, SanskritException {
    activateCluster();
    assertThat(
        () -> invokeConfigTool("unset", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.main"),
        exceptionMatcher("Reason: Setting 'data-dirs' cannot be unset when node is activated"));
    assertChanges(3);
  }

  @Test
  public void testUnsetDataDirAtNodeLevelAfterActivate() throws IOException, SanskritException {
    activateCluster();
    assertThat(
        () -> invokeConfigTool("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs.main"),
        exceptionMatcher("Reason: Setting 'data-dirs' cannot be unset when node is activated"));
    assertChanges(3);
  }

  @Test
  public void testUnsetDataDirAtClusterLevelBeforeActivate() throws IOException, SanskritException {
    invokeConfigTool("unset", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.main");
    activateCluster();
    assertChanges(3);
  }

  @Test
  public void testUnsetDataDirAtNodeLevelBeforeActivate() throws IOException, SanskritException {
    invokeConfigTool("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs.main");
    activateCluster();
    assertChanges(3);
  }

  @Test
  public void testSetFailoverPriorityAfterActivate() throws SanskritException, IOException {
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency");
    activateCluster();
    assertChanges(3);
  }

  @Test
  public void testSetClusterNameAferActivation() throws IOException, SanskritException {
    activateCluster();
    invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "cluster-name=mycluster");
    assertChanges(5);
  }

  @Test
  public void setMetaDataDirAtNodeLevelAfterActivation() throws IOException, SanskritException {
    activateCluster();
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.metadata-dir=newmetadata"),
        exceptionMatcher("Reason: Setting 'metadata-dir' cannot be set when node is activated"));
    assertChanges(3);
  }

  @Test
  public void unsetMetaDataDirAtNodeLevelAfterActivation() throws IOException, SanskritException {
    activateCluster();
    assertThat(
        () -> invokeConfigTool("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.metadata-dir"),
        exceptionMatcher("Reason: Setting 'metadata-dir' cannot be unset"));
    assertChanges(3);
  }

  @Test
  public void setGroupPortNodeLevelAfterActivation() throws IOException, SanskritException {
    activateCluster();
    assertThat(
        () -> invokeConfigTool("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.group-port=1234"),
        exceptionMatcher("Reason: Setting 'group-port' cannot be set when node is activated"));
    assertChanges(3);
  }

  private List<SanskritObject> getChanges(Path pathToAppendLog) throws SanskritException {
    ObjectMapper objectMapper = objectMapperFactory.create();
    List<SanskritObject> res = new ArrayList<>();
    new SanskritImpl(new FileBasedFilesystemDirectory(pathToAppendLog), objectMapper) {
      @Override
      public void onNewRecord(String timeStamp, String json) throws SanskritException {
        MutableSanskritObject mutableSanskritObject = new SanskritObjectImpl(objectMapper);
        JsonUtils.parse(objectMapper, json, mutableSanskritObject);
        res.add(mutableSanskritObject);
      }
    };
    return res;
  }

  private void assertChanges(int size) throws IOException, SanskritException {
    TerracottaServer active = getNode(1, 1);
    stopNode(1, 1);
    assertThat(angela.tsa().getStopped().size(), is(1));
    Path activePath = getBaseDir().resolve("activeRepo");
    Files.createDirectories(activePath);
    angela.tsa().browse(active, Paths.get(active.getConfigRepo()).resolve("changes").toString()).downloadTo(activePath.toFile());
    List<SanskritObject> activeChanges = getChanges(activePath);
    assertThat(activeChanges.size(), is(size));
  }
}
