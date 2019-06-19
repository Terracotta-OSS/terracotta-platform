/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.common;

import com.beust.jcommander.IStringConverter;
import com.terracottatech.utilities.InetSocketAddressConvertor;

import java.net.InetSocketAddress;

/**
 * @author Mathieu Carbou
 */
public class InetSocketAddressConverter implements IStringConverter<InetSocketAddress> {
  @Override
  public InetSocketAddress convert(String value) {
      //TODO [DYNAMIC-CONFIG]: Check if we could move the 9410 default port to InetSocketAddressConvertor instead of using 0
    return InetSocketAddressConvertor.getInetSocketAddress(value, 9410);
  }
}
