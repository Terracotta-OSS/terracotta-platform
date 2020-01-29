/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.change;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// Annotate with @JsonTypeName the sub-classes of an interface annotated with @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
// so that the Json class can discover and register the implementations of the class in the object mapper
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
public interface NomadChange {

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  String getSummary();
}
