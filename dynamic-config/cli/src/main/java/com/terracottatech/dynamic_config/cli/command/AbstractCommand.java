/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;


import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.EnumConverter;
import com.terracottatech.utilities.InetSocketAddressConvertor;

import java.net.InetSocketAddress;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractCommand implements DynamicConfigCommand {

  @Parameter(names = {"-h", "--help"}, description = "Help", help = true)
  public boolean help;

  // JCommander converters and validators

  public static class InetSocketAddressConverter implements IStringConverter<InetSocketAddress> {

    private static final int DEFAULT_PORT = 9410;

    @Override
    public InetSocketAddress convert(String value) {
      return InetSocketAddressConvertor.getInetSocketAddress(value, DEFAULT_PORT);
    }
  }

  public static class TypeConverter extends EnumConverter<TopologyChangeCommand.Type> {
    public TypeConverter() {
      super("-t", TopologyChangeCommand.Type.class);
    }
  }
}
