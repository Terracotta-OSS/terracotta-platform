/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package org.terracotta.dynamic_config.cli.converter;

import com.beust.jcommander.IStringConverter;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;

public class TimeUnitConverter implements IStringConverter<Measure<TimeUnit>> {
  @Override
  public Measure<TimeUnit> convert(String value) {
    return Measure.parse(value, TimeUnit.class);
  }
}
