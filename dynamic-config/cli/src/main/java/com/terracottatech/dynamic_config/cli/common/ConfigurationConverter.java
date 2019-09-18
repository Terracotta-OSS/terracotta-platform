/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.common;

import com.beust.jcommander.IStringConverter;
import com.terracottatech.dynamic_config.cli.service.command.Configuration;

/**
 * @author Mathieu Carbou
 */
public class ConfigurationConverter implements IStringConverter<Configuration> {
  @Override
  public Configuration convert(String value) {
    return Configuration.valueOf(value);
  }
}
