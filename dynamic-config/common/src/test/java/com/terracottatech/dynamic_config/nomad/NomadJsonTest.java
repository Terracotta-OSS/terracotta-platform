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
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class NomadJsonTest {

  @Test
  public void test_ser_deser() throws IOException {
    NomadChange[] changes = {
        new ClusterActivationNomadChange("myClusterName", new Cluster(new Stripe(new Node()
            .setNodeName("foo")
            .setClientReconnectWindow(60, TimeUnit.SECONDS).setOffheapResource("foo", 1, MemoryUnit.GB)))),
        new ConfigMigrationNomadChange("xml config"),
        new ConfigRepairNomadChange("xml config"),
        SettingNomadChange.set(Applicability.node("stripe1", "node1"), "offheap-resources.foo", "2GB"),
        new MultipleNomadChanges(
            SettingNomadChange.set(Applicability.node("stripe1", "node1"), "offheap-resources.foo", "2GB"),
            SettingNomadChange.set(Applicability.cluster(), "offheap-resources.bar", "512MB")
        )
    };

    ObjectMapper objectMapper = NomadJson.buildObjectMapper();

    for (int i = 0; i < changes.length; i++) {
      NomadChange change = changes[i];
      URL jsonFile = getClass().getResource("/nomad/change" + i + ".json");
      //System.out.println(Json.toPrettyJson(change));
      assertThat(jsonFile.getPath(), objectMapper.valueToTree(change).toString(), is(equalTo(objectMapper.readTree(jsonFile).toString())));
      assertThat(jsonFile.getPath(), objectMapper.readValue(jsonFile, NomadChange.class), is(equalTo(change)));
    }
  }

}
