/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.nomad.persistence;

public interface NomadSanskritKeys {
  String REQUEST = "request";
  String MODE = "mode";
  String MUTATIVE_MESSAGE_COUNT = "mutativeMessageCount";
  String LAST_MUTATION_HOST = "lastMutationHost";
  String LAST_MUTATION_USER = "lastMutationUser";
  String LAST_MUTATION_TIMESTAMP = "lastMutationTimestamp";
  String LATEST_CHANGE_UUID = "latestChangeUuid";
  String CURRENT_VERSION = "currentVersion";
  String HIGHEST_VERSION = "highestVersion";
  String CHANGE_STATE = "state";
  String CHANGE_VERSION = "version";
  String CHANGE_OPERATION = "operation";
  String CHANGE_RESULT_HASH = "changeResultHash";
  String CHANGE_CREATION_HOST = "creationHost";
  String CHANGE_CREATION_USER = "creationUser";
  String CHANGE_CREATION_TIMESTAMP = "creationTimestamp";
  String PREV_CHANGE_UUID = "prevChangeUuid";
}
