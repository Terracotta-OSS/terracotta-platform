/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.terracottatech.dynamic_config.DynamicConfigConstants;

import java.util.Objects;

import static com.terracottatech.dynamic_config.model.FailoverPriority.Type.AVAILABILITY;
import static com.terracottatech.dynamic_config.model.FailoverPriority.Type.CONSISTENCY;
import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class FailoverPriority {

  private final Type type;
  private final int voters;

  public enum Type {AVAILABILITY, CONSISTENCY}

  private FailoverPriority(Type type, Integer voters) {
    this.type = type;
    this.voters = voters;
  }

  public Type getType() {
    return type;
  }

  public int getVoters() {
    return voters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FailoverPriority)) return false;
    FailoverPriority that = (FailoverPriority) o;
    return getVoters() == that.getVoters() &&
        getType() == that.getType();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getVoters());
  }

  @JsonValue
  @Override
  public String toString() {
    return type == AVAILABILITY ? AVAILABILITY.name().toLowerCase() :
        voters == 0 ? CONSISTENCY.name().toLowerCase() :
            (CONSISTENCY.name().toLowerCase() + ":" + voters);
  }

  public static FailoverPriority availability() {
    return new FailoverPriority(AVAILABILITY, 0);
  }

  public static FailoverPriority consistency() {
    return new FailoverPriority(CONSISTENCY, 0);
  }

  public static FailoverPriority consistency(int voters) {
    if (voters <= 0) {
      throw new IllegalArgumentException(Setting.FAILOVER_PRIORITY + " should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
    }
    return new FailoverPriority(Type.CONSISTENCY, voters);
  }

  @JsonCreator
  public static FailoverPriority valueOf(String str) {
    requireNonNull(str);
    final String[] split = str.split(DynamicConfigConstants.PARAM_INTERNAL_SEP);
    if (split.length > 2) {
      throw new IllegalArgumentException(Setting.FAILOVER_PRIORITY + " should be one of: " + Setting.FAILOVER_PRIORITY.getAllowedValues());
    }
    if ("availability".equals(str)) {
      return availability();
    }
    if ("consistency".equals(str)) {
      return consistency();
    }
    if (str.startsWith("consistency:")) {
      try {
        return consistency(Integer.parseInt(str.substring(12)));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(Setting.FAILOVER_PRIORITY + " should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
      }
    }
    throw new IllegalArgumentException(Setting.FAILOVER_PRIORITY + " should be either 'availability', 'consistency', or 'consistency:N' (where 'N' is the voter count expressed as a positive integer)");
  }
}