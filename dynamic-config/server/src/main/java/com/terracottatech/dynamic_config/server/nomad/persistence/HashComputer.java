/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.nomad.persistence;

/**
 * @author Mathieu Carbou
 */
@FunctionalInterface
public interface HashComputer<T> {
  String computeHash(T o);
}