/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.nomad.client.change.MultipleNomadChanges;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.change.SimpleNomadChange;
import com.terracottatech.utilities.Json;

/**
 * @author Mathieu Carbou
 */
public class NomadJson {

  public static ObjectMapper buildObjectMapper() {
    ObjectMapper objectMapper = Json.copyObjectMapper(true);
    objectMapper.registerSubtypes(
        NomadChange.class,
        TopologyNomadChange.class,
        FilteredNomadChange.class,
        ConfigMigrationNomadChange.class,
        ConfigRepairNomadChange.class,
        ClusterActivationNomadChange.class,
        SettingNomadChange.class,
        MultipleNomadChanges.class,
        SimpleNomadChange.class);
    return objectMapper.copy();
  }

}
