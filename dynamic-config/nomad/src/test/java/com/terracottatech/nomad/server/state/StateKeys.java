/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server.state;

public interface StateKeys {
  String INITIALIZED = "initialized";
  String MODE = "mode";
  String MUTATIVE_MESSAGE_COUNT = "mutativeMessageCount";
  String LAST_MUTATION_HOST = "lastMutationHost";
  String LAST_MUTATION_USER = "lastMutationUser";
  String LATEST_CHANGE_UUID = "latestChangeUuid";
  String CURRENT_VERSION = "currentVersion";
  String HIGHEST_VERSION = "highestVersion";

  String STATE = "state";
  String VERSION = "version";
  String CHANGE = "change";
  String CREATION_HOST = "creationHost";
  String CREATION_USER = "creationUser";
  String PREV_CHANGE_UUID = "prevChangeUuid";
}
