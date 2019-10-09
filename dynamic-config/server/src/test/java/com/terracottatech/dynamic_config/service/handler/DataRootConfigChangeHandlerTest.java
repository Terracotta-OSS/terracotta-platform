/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package com.terracottatech.dynamic_config.service.handler;

import com.terracottatech.config.data_roots.DataDirectoriesConfig;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.nio.file.Paths;

import static com.terracottatech.dynamic_config.nomad.Applicability.cluster;
import static com.terracottatech.dynamic_config.util.IParameterSubstitutor.identity;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DataRootConfigChangeHandlerTest {

  private NodeContext topology = new NodeContext(new Cluster("foo", new Stripe(new Node().setNodeName("bar"))), 1, "bar");
  private SettingNomadChange set = SettingNomadChange.set(cluster(), Setting.DATA_DIRS, "new-root", "/path/to/data/root");

  @Test
  public void testGetConfigWithChange() throws Exception {
    DataDirectoriesConfig dataDirectoriesConfig = mock(DataDirectoriesConfig.class);
    DataRootConfigChangeHandler dataRootConfigChangeHandler = new DataRootConfigChangeHandler(dataDirectoriesConfig, identity());
    Cluster updatedXmlConfig = dataRootConfigChangeHandler.tryApply(topology, set);

    assertThat(updatedXmlConfig.getSingleNode().get().getDataDirs().entrySet(), Matchers.hasSize(1));
    assertThat(updatedXmlConfig.getSingleNode().get().getDataDirs(), hasEntry("new-root", Paths.get("/path/to/data/root")));
  }

  @Test
  public void testApplyChange() {
    DataDirectoriesConfig dataDirectoriesConfig = mock(DataDirectoriesConfig.class);
    DataRootConfigChangeHandler dataRootConfigChangeHandler = new DataRootConfigChangeHandler(dataDirectoriesConfig, identity());

    dataRootConfigChangeHandler.apply(set);

    verify(dataDirectoriesConfig).addDataDirectory("new-root", "/path/to/data/root");
  }
}