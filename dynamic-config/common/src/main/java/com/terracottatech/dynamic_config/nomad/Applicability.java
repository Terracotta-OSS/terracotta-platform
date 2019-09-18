/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracottatech.dynamic_config.model.Scope;

import java.util.Objects;
import java.util.OptionalInt;

import static com.terracottatech.dynamic_config.model.Scope.CLUSTER;
import static com.terracottatech.dynamic_config.model.Scope.NODE;
import static com.terracottatech.dynamic_config.model.Scope.STRIPE;
import static java.util.Objects.requireNonNull;

public class Applicability {
  private Scope scope;
  private String nodeName;
  private Integer stripeId;

  public static Applicability cluster() {
    return new Applicability(CLUSTER, null, null);
  }

  public static Applicability stripe(int stripeId) {
    return new Applicability(STRIPE, stripeId, null);
  }

  public static Applicability node(int stripeId, String nodeName) {
    return new Applicability(NODE, stripeId, requireNonNull(nodeName));
  }

  @JsonCreator
  private Applicability(@JsonProperty(value = "scope", required = true) Scope scope,
                        @JsonProperty("stripeId") Integer stripeId,
                        @JsonProperty("nodeName") String nodeName) {
    this.scope = requireNonNull(scope);
    this.stripeId = stripeId;
    this.nodeName = nodeName;
  }

  public Scope getScope() {
    return scope;
  }

  public String getNodeName() {
    return nodeName;
  }

  public OptionalInt getStripeId() {
    return stripeId == null ? OptionalInt.empty() : OptionalInt.of(stripeId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Applicability)) return false;
    Applicability that = (Applicability) o;
    return getScope() == that.getScope() &&
        Objects.equals(getNodeName(), that.getNodeName()) &&
        Objects.equals(getStripeId(), that.getStripeId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getScope(), getNodeName(), getStripeId());
  }

  @Override
  public String toString() {
    return "Applicability{" +
        "type=" + scope +
        ", nodeName='" + nodeName + '\'' +
        ", stripeId='" + stripeId + '\'' +
        '}';
  }
}
