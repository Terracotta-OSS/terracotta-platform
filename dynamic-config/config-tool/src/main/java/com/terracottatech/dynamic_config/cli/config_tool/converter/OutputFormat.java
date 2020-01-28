/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.config_tool.converter;

import com.beust.jcommander.converters.EnumConverter;

public enum OutputFormat {
  JSON,
  PROPERTIES;

  public static class FormatConverter extends EnumConverter<OutputFormat> {
    public FormatConverter() {
      super("-f", OutputFormat.class);
    }
  }
}
