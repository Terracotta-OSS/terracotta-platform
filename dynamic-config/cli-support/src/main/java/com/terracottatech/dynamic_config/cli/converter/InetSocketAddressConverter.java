/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.converter;

import com.beust.jcommander.IStringConverter;
import com.terracottatech.inet.InetSocketAddressConvertor;

import java.net.InetSocketAddress;

/**
 * @author Mathieu Carbou
 */
public class InetSocketAddressConverter implements IStringConverter<InetSocketAddress> {
  @Override
  public InetSocketAddress convert(String value) {
    return InetSocketAddressConvertor.getInetSocketAddress(value);
  }
}
