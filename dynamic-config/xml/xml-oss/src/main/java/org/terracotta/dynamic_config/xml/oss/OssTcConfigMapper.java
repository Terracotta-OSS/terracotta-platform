/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.xml.oss;

import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.TcConfigMapper;

/**
 * @author Mathieu Carbou
 */
public class OssTcConfigMapper implements TcConfigMapper {
  @Override
  public Stripe fromXml(String xml) {
    //TODO [DYNAMIC-CONFIG]: TDB-4833 - convert an OSS tc-config.xml file here
    throw new UnsupportedOperationException("to implement");
  }
}
