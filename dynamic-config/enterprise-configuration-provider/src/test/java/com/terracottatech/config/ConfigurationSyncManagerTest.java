/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import org.junit.Test;

import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.nomad.server.NomadChangeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.terracottatech.config.ConfigurationSyncManager.Codec.decode;
import static com.terracottatech.config.ConfigurationSyncManager.Codec.encode;
import static com.terracottatech.dynamic_config.nomad.Applicability.cluster;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigurationSyncManagerTest {
  @Test
  public void testCodec() {
    List<NomadChangeInfo> nomadChanges = new ArrayList<>();
    nomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("a", "100")));
    nomadChanges.add(new NomadChangeInfo(UUID.randomUUID(), createOffheapChange("b", "200")));

    List<NomadChangeInfo> decodedChanges = decode(encode(nomadChanges));
    System.out.println(new String(encode(nomadChanges)));
    assertThat(decodedChanges, is(nomadChanges));
  }

  private static SettingNomadChange createOffheapChange(String resourceName, String size) {
    return SettingNomadChange.set(cluster(), Setting.OFFHEAP_RESOURCES, resourceName, size);
  }
}