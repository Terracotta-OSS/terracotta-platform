/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AcceptRejectResponse {
  private final boolean accepted;
  private final RejectionReason rejectionReason;
  private final String rejectionMessage;
  private final String lastMutationHost;
  private final String lastMutationUser;

  public static AcceptRejectResponse accept() {
    return new AcceptRejectResponse(true, null, null, null, null);
  }

  public static AcceptRejectResponse reject(RejectionReason rejectionReason, String lastMutationHost, String lastMutationUser) {
    return new AcceptRejectResponse(false, rejectionReason, null, lastMutationHost, lastMutationUser);
  }

  public static AcceptRejectResponse reject(RejectionReason rejectionReason, String rejectionMessage, String lastMutationHost, String lastMutationUser) {
    return new AcceptRejectResponse(false, rejectionReason, rejectionMessage, lastMutationHost, lastMutationUser);
  }

  @JsonCreator
  private AcceptRejectResponse(@JsonProperty(value = "accepted", required = true) boolean accepted,
                               @JsonProperty(value = "rejectionReason") RejectionReason rejectionReason,
                               @JsonProperty(value = "rejectionMessage") String rejectionMessage,
                               @JsonProperty(value = "lastMutationHost") String lastMutationHost,
                               @JsonProperty(value = "lastMutationUser") String lastMutationUser) {
    this.accepted = accepted;
    this.rejectionReason = rejectionReason;
    this.rejectionMessage = rejectionMessage;
    this.lastMutationHost = lastMutationHost;
    this.lastMutationUser = lastMutationUser;
  }

  public boolean isAccepted() {
    return accepted;
  }

  @JsonIgnore
  public boolean isRejected() {
    return !accepted;
  }

  public RejectionReason getRejectionReason() {
    return rejectionReason;
  }

  public String getRejectionMessage() {
    return rejectionMessage;
  }

  public String getLastMutationHost() {
    return lastMutationHost;
  }

  public String getLastMutationUser() {
    return lastMutationUser;
  }

  @Override
  public String toString() {
    return accepted ? "accepted" : (rejectionReason.name().toLowerCase() + (rejectionMessage == null ? "" : (" ( " + rejectionMessage + ")")));
  }
}
