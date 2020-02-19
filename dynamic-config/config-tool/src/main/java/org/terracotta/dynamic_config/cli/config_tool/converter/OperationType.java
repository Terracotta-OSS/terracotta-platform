/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.converter;

import com.beust.jcommander.converters.EnumConverter;

public enum OperationType {
  NODE,
  STRIPE;

  @Override
  public String toString() {
    return name().toLowerCase();
  }

  public static class TypeConverter extends EnumConverter<OperationType> {
    public TypeConverter() {
      super("-t", OperationType.class);
    }
  }
}