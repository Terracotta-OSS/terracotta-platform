/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.nomad.client.change.MultipleNomadChanges;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.utilities.Json;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static com.terracottatech.dynamic_config.model.Setting.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.model.Setting.OFFHEAP_RESOURCES;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class NomadJsonTest {

  private Cluster cluster = new Cluster("myClusterName", new Stripe(new Node()
      .setNodeName("foo")
      .setClientReconnectWindow(60, TimeUnit.SECONDS).setOffheapResource("foo", 1, MemoryUnit.GB)));

  @Test
  public void test_ser_deser() throws IOException {
    NomadChange[] changes = {
        new ClusterActivationNomadChange(cluster),
        new ConfigMigrationNomadChange(cluster),
        new ConfigRepairNomadChange(cluster),
        SettingNomadChange.set(Applicability.node(1, "node1"), NODE_BACKUP_DIR, "backup"),
        new MultipleNomadChanges(
            SettingNomadChange.set(Applicability.node(1, "node1"), NODE_BACKUP_DIR, "backup"),
            SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "bar", "512MB")
        )
    };

    ObjectMapper objectMapper = Json.copyObjectMapper(true);

    for (int i = 0; i < changes.length; i++) {
      NomadChange change = changes[i];
      URL jsonFile = getClass().getResource("/nomad/change" + i + ".json");
      //System.out.println(Json.toPrettyJson(change));
      assertThat(jsonFile.getPath(), objectMapper.valueToTree(change).toString(), is(equalTo(objectMapper.readTree(jsonFile).toString())));
      assertThat(jsonFile.getPath(), objectMapper.readValue(jsonFile, NomadChange.class), is(equalTo(change)));
    }
  }

}
