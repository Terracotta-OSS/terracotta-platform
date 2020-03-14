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

import org.hamcrest.Matchers;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;

import java.nio.file.Paths;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.terracotta.dynamic_config.api.model.nomad.Applicability.cluster;
import static org.terracotta.dynamic_config.api.service.IParameterSubstitutor.identity;

public class DataDirectoryConfigChangeHandlerTest {

  private NodeContext topology = new NodeContext(new Cluster("foo", new Stripe(Node.newDefaultNode("bar", "localhost").clearDataDirs())), 1, "bar");
  private SettingNomadChange set = SettingNomadChange.set(cluster(), Setting.DATA_DIRS, "new-root", "/path/to/data/root");

  @Test
  public void testGetConfigWithChange() throws Exception {
    DataDirectoriesConfig dataDirectoriesConfig = mock(DataDirectoriesConfig.class);
    DataDirectoryConfigChangeHandler dataDirectoryConfigChangeHandler = new DataDirectoryConfigChangeHandler(dataDirectoriesConfig, identity());
    dataDirectoryConfigChangeHandler.validate(topology, set.toConfiguration(topology.getCluster()));

    assertThat(set.apply(topology.getCluster()).getSingleNode().get().getDataDirs().entrySet(), Matchers.hasSize(1));
    assertThat(set.apply(topology.getCluster()).getSingleNode().get().getDataDirs(), hasEntry("new-root", Paths.get("/path/to/data/root")));
  }

  @Test
  public void testApplyChange() {
    DataDirectoriesConfig dataDirectoriesConfig = mock(DataDirectoriesConfig.class);
    DataDirectoryConfigChangeHandler dataDirectoryConfigChangeHandler = new DataDirectoryConfigChangeHandler(dataDirectoriesConfig, identity());

    dataDirectoryConfigChangeHandler.apply(set.toConfiguration(topology.getCluster()));

    verify(dataDirectoriesConfig).addDataDirectory("new-root", "/path/to/data/root");
  }
}