/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.restart;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Mathieu Carbou
 */
public interface RestartProgress {
  /**
   * Await indefinitely for all nodes to be restarted.
   */
  void await() throws InterruptedException;

  /**
   * Await for all nodes to be restarted for a maximum amount of time.
   * <p>
   * If the timeout expires, returns the list of all nodes that have been detected as restarted.
   * <p>
   * If all nodes are restarted before the timeout expires, the list will contain all the nodes that were asked to restart
   */
  Collection<InetSocketAddress> await(Duration duration) throws InterruptedException;

  /**
   * Register a callback that will be called when a node has been restarted
   */
  void onRestarted(Consumer<InetSocketAddress> c);

  /**
   * Get all nodes for which we have failed to ask for a restart
   */
  Map<InetSocketAddress, Exception> getErrors();
}
