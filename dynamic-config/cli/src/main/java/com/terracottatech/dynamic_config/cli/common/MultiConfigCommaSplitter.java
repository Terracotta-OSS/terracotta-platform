/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.common;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.IParameterSplitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A splitter which splits the input about a comma, and transforms it into a multi-config representation. For example,
 * data-dirs=foo:bar,foo2:bar2 becomes {data-dirs.foo=bar, data-dirs.foo2=bar2}
 */
public class MultiConfigCommaSplitter implements IParameterSplitter {
  public List<String> split(String value) {
    List<String> properties = new ArrayList<>();
    try {
      String[] keyValue = value.split("=");
      if (keyValue.length == 1) {
        // No '=' specified, which would be case in get and unset commands
        return Collections.singletonList(value);
      }

      String[] values = keyValue[1].split(",");
      if (values.length == 1 && !values[0].contains(":")) {
        return Collections.singletonList(value);
      }

      for (String item : values) {
        String[] nameProperty = item.split(":");
        properties.add(keyValue[0] + "." + nameProperty[0] + "=" + nameProperty[1]);
      }
      return properties;
    } catch (Exception e) {
      throw new ParameterException("Invalid input: " + value, e);
    }
  }
}
