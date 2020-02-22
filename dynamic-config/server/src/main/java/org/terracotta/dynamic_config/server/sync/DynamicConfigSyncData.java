/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.terracotta.nomad.server.NomadChangeInfo;

import java.util.List;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigSyncData {

  private final List<NomadChangeInfo> nomadChanges;
  private final String license;

  @JsonCreator
  public DynamicConfigSyncData(@JsonProperty(value = "nomadChanges", required = true) List<NomadChangeInfo> nomadChanges,
                               @JsonProperty(value = "license") String license) {
    this.nomadChanges = nomadChanges;
    this.license = license;
  }

  public List<NomadChangeInfo> getNomadChanges() {
    return nomadChanges;
  }

  public String getLicense() {
    return license;
  }
}
