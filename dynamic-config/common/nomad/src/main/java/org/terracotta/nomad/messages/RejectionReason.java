/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.messages;

public enum RejectionReason {
  UNACCEPTABLE, // The server is not able to apply that change
  DEAD, // The mutative message count did not match
  BAD // The message was still alive, but inconsistent with the server state - this likely indicates a bug
}
