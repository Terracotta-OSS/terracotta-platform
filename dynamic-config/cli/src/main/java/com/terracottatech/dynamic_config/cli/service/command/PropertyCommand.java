/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public abstract class PropertyCommand extends Command {
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-c"}, description = "Config properties to be set", required = true)
  List<String> configs;

  @Resource
  public MultiDiagnosticServiceConnectionFactory connectionFactory;

  final List<ParsedInput> parsedInput = new ArrayList<>();

  int validateIndex(String key, String errMsgFragment) {
    int stripeId = -1;
    try {
      stripeId = Integer.parseInt(key);
    } catch (NumberFormatException e) {
      throwException("Expected an integer, got: %s", key);
    }

    if (stripeId < 1) {
      throwException("%s, but found: %s", errMsgFragment, stripeId);
    }
    return stripeId;
  }

  void throwException(String formattedMsg, Object... args) {
    throw new IllegalArgumentException(String.format(formattedMsg, args));
  }

  void setConfigs(List<String> configs) {
    this.configs = configs;
  }

  enum Scope {
    NODE,
    STRIPE,
    CLUSTER
  }

  static class ParsedInput {
    private final String rawInput;
    private final Scope scope;
    private final int stripeId;
    private final int nodeId;
    private final String property;
    private final String propertyName;
    private final String value;

    ParsedInput(String rawInput, Scope scope, int stripeId, int nodeId, String property, String propertyName, String value) {
      this.rawInput = rawInput;
      this.scope = scope;
      this.stripeId = stripeId;
      this.nodeId = nodeId;
      this.property = property;
      this.propertyName = propertyName;
      this.value = value;
    }

    String getRawInput() {
      return rawInput;
    }

    Scope getScope() {
      return scope;
    }

    int getStripeId() {
      return stripeId;
    }

    int getNodeId() {
      return nodeId;
    }

    String getProperty() {
      return property;
    }

    String getPropertyName() {
      return propertyName;
    }

    String getValue() {
      return value;
    }
  }
}
