/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.nomad.server.NomadChangeInfo;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.UpgradableNomadServer;

import java.util.List;

import static com.terracottatech.utilities.Json.parse;
import static com.terracottatech.utilities.Json.toJson;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

class ConfigurationSyncManager {
  private final UpgradableNomadServer<NodeContext> nomadServer;

  ConfigurationSyncManager(UpgradableNomadServer<NodeContext> nomadServer) {
    this.nomadServer = nomadServer;
  }

  byte[] getSyncData() {
    try {
      return Codec.encode(nomadServer.getAllNomadChanges());
    } catch (NomadException e) {
      throw new RuntimeException(e);
    }
  }

  void sync(byte[] syncData) {
    // List<NomadChangeInfo> activeNomadChanges = Codec.decode(syncData);
  }

  static class Codec {
    static byte[] encode(List<NomadChangeInfo> nomadChanges) {
      return toJson(requireNonNull(nomadChanges)).getBytes(UTF_8);
    }

    static List<NomadChangeInfo> decode(byte[] encoded) {
      return parse(new String(encoded, UTF_8), new TypeReference<List<NomadChangeInfo>>(){});
    }
  }
}
