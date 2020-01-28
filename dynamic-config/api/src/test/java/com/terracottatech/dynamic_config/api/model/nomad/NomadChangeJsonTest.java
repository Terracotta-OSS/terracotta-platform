/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.MemoryUnit;
import com.terracottatech.dynamic_config.api.model.Node;
import com.terracottatech.dynamic_config.api.model.Stripe;
import com.terracottatech.dynamic_config.api.model.TimeUnit;
import com.terracottatech.json.Json;
import com.terracottatech.nomad.client.change.MultipleNomadChanges;
import com.terracottatech.nomad.client.change.NomadChange;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.terracottatech.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class NomadChangeJsonTest {

  private Cluster cluster = new Cluster("myClusterName", new Stripe(Node.newDefaultNode("foo", "localhost", 9410)
      .setClientReconnectWindow(60, TimeUnit.SECONDS).setOffheapResource("foo", 1, MemoryUnit.GB)));

  @Test
  public void test_ser_deser() throws IOException, URISyntaxException {
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
      byte[] bytes = Files.readAllBytes(Paths.get(jsonFile.toURI()));
      String json = new String(bytes, StandardCharsets.UTF_8);

      if (isWindows()) {
        json = json.replace("/", "\\\\");
      }

      assertThat(jsonFile.getPath(), objectMapper.valueToTree(change).toString(), is(equalTo(objectMapper.readTree(json).toString())));
      assertThat(jsonFile.getPath(), objectMapper.readValue(json, NomadChange.class), is(equalTo(change)));
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }
}
