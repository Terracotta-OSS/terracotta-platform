/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.restart;

import com.terracottatech.utilities.Tuple2;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
public interface RestartProgress {
  Map<InetSocketAddress, Tuple2<String, Exception>> await() throws InterruptedException;
}
