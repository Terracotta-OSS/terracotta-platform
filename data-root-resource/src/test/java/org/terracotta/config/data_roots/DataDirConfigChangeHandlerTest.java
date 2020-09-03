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
package org.terracotta.config.data_roots;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.testing.TmpDir;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;
import static org.terracotta.dynamic_config.api.model.nomad.Applicability.cluster;
import static org.terracotta.dynamic_config.api.service.IParameterSubstitutor.identity;

public class DataDirConfigChangeHandlerTest {

  @Rule
  public TmpDir tmpDir = new TmpDir(Paths.get(System.getProperty("user.dir"), "target"), false);

  private final Cluster testCluster = newTestCluster("foo", newTestStripe("stripe-1").addNode(newTestNode("bar", "localhost").unsetDataDirs()));
  private final NodeContext topology = new NodeContext(testCluster, Testing.N_UIDS[1]);
  private final SettingNomadChange set = SettingNomadChange.set(cluster(), Setting.DATA_DIRS, "new-root", "path/to/data/root");

  @Test
  public void testGetConfigWithChange() throws Exception {
    DataDirsConfig dataDirsConfig = mock(DataDirsConfig.class);
    DataDirConfigChangeHandler dataDirConfigChangeHandler = new DataDirConfigChangeHandler(dataDirsConfig, identity(), new PathResolver(tmpDir.getRoot()));
    dataDirConfigChangeHandler.validate(topology, set.toConfiguration(topology.getCluster()));

    assertThat(set.apply(topology.getCluster()).getSingleNode().get().getDataDirs().orDefault().entrySet(), hasSize(1));
    assertThat(set.apply(topology.getCluster()).getSingleNode().get().getDataDirs().orDefault(), hasEntry("new-root", RawPath.valueOf("path/to/data/root")));
  }

  @Test
  public void testApplyChange() {
    DataDirsConfig dataDirsConfig = mock(DataDirsConfig.class);
    DataDirConfigChangeHandler dataDirConfigChangeHandler = new DataDirConfigChangeHandler(dataDirsConfig, identity(), new PathResolver(tmpDir.getRoot()));
    dataDirConfigChangeHandler.apply(set.toConfiguration(topology.getCluster()));

    verify(dataDirsConfig).addDataDirectory("new-root", "path/to/data/root");
  }
}