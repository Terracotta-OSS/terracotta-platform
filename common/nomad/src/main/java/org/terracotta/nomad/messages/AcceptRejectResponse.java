/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.nomad.messages;

public class AcceptRejectResponse {
  private final boolean accepted;
  private final RejectionReason rejectionReason;
  private final String rejectionMessage;
  private final String lastMutationHost;
  private final String lastMutationUser;

  // For Json
  AcceptRejectResponse() {
    accepted = false;
    rejectionReason = null;
    rejectionMessage = null;
    lastMutationHost = null;
    lastMutationUser = null;
  }

  public static AcceptRejectResponse accept() {
    return new AcceptRejectResponse(true, null, null, null, null);
  }

  public static AcceptRejectResponse reject(RejectionReason rejectionReason, String lastMutationHost, String lastMutationUser) {
    return new AcceptRejectResponse(false, rejectionReason, null, lastMutationHost, lastMutationUser);
  }

  public static AcceptRejectResponse reject(RejectionReason rejectionReason, String rejectionMessage, String lastMutationHost, String lastMutationUser) {
    return new AcceptRejectResponse(false, rejectionReason, rejectionMessage, lastMutationHost, lastMutationUser);
  }

  protected AcceptRejectResponse(boolean accepted,
                                 RejectionReason rejectionReason,
                                 String rejectionMessage,
                                 String lastMutationHost,
                                 String lastMutationUser) {
    this.accepted = accepted;
    this.rejectionReason = rejectionReason;
    this.rejectionMessage = rejectionMessage;
    this.lastMutationHost = lastMutationHost;
    this.lastMutationUser = lastMutationUser;
  }

  public boolean isAccepted() {
    return accepted;
  }

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
    return accepted ? "accepted" : (rejectionReason.name().toLowerCase() + (rejectionMessage == null ? "" : (" ( " + rejectionMessage + ")")) + " by " + lastMutationUser + " from " + lastMutationHost);
  }
}
