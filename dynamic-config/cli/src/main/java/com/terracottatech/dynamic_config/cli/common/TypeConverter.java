/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.common;

import com.beust.jcommander.converters.EnumConverter;
import com.terracottatech.dynamic_config.cli.service.command.TopologyChangeCommand;

/**
 * @author Mathieu Carbou
 */
public class TypeConverter extends EnumConverter<TopologyChangeCommand.Type> {
  public TypeConverter() {
    super("-t", TopologyChangeCommand.Type.class);
  }
}