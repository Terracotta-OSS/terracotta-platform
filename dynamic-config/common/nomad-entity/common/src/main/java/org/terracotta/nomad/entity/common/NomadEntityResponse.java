/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.entity.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.terracotta.entity.EntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;

/**
 * @author Mathieu Carbou
 */
public class NomadEntityResponse implements EntityResponse {

  private final AcceptRejectResponse response;

  @JsonCreator
  public NomadEntityResponse(@JsonProperty(value = "response", required = true) AcceptRejectResponse response) {
    this.response = response;
  }

  public AcceptRejectResponse getResponse() {
    return response;
  }
}
