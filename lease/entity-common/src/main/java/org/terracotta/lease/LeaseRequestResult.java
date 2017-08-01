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
package org.terracotta.lease;

import org.terracotta.runnel.Struct;
import org.terracotta.runnel.StructBuilder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.encoding.StructEncoder;

/**
 * A message sent from the the server entity to the client entity to indicate the response to the LeaseRequest.
 */
public class LeaseRequestResult implements LeaseResponse {
  private final boolean connectionGood;
  private final boolean leaseGranted;
  private final long leaseLength;

  public static LeaseRequestResult leaseNotGranted() {
    return new LeaseRequestResult(true, false, -1L);
  }

  public static LeaseRequestResult leaseGranted(long leaseLength) {
    if (leaseLength <= 0) {
      throw new IllegalArgumentException("Granting a non-positive length lease is not allowed: " + leaseLength);
    }
    return new LeaseRequestResult(true, true, leaseLength);
  }

  public static LeaseRequestResult oldConnection() {
    return new LeaseRequestResult(false, false, -1L);
  }

  private LeaseRequestResult(boolean connectionGood, boolean leaseGranted, long leaseLength) {
    this.connectionGood = connectionGood;
    this.leaseGranted = leaseGranted;
    this.leaseLength = leaseLength;
  }

  public boolean isConnectionGood() {
    return connectionGood;
  }

  public boolean isLeaseGranted() {
    return leaseGranted;
  }

  public long getLeaseLength() {
    if (leaseLength <= 0) {
      throw new IllegalStateException("Attempt to get the lease length when the lease was not granted");
    }
    return leaseLength;
  }
  @Override
  public LeaseResponseType getType() {
    return LeaseResponseType.LEASE_REQUEST_RESULT;
  }


  public static void addStruct(StructBuilder parentBuilder, int index) {
    StructBuilder builder = StructBuilder.newStructBuilder();
    builder.bool("connectionGood", 10);
    builder.bool("leaseGranted", 20);
    builder.int64("leaseLength", 30);
    Struct struct = builder.build();

    parentBuilder.struct("leaseRequestResult", index, struct);
  }

  @Override
  public void encode(StructEncoder<Void> parentEncoder) {
    StructEncoder<StructEncoder<Void>> encoder = parentEncoder.struct("leaseRequestResult");
    encoder.bool("connectionGood", connectionGood);
    encoder.bool("leaseGranted", leaseGranted);
    encoder.int64("leaseLength", leaseLength);
    encoder.end();
  }

  public static LeaseResponse decode(StructDecoder<Void> parentDecoder) {
    StructDecoder<StructDecoder<Void>> decoder = parentDecoder.struct("leaseRequestResult");
    boolean connectionGood = decoder.bool("connectionGood");
    boolean leaseGranted = decoder.bool("leaseGranted");
    long leaseLength = decoder.int64("leaseLength");
    return new LeaseRequestResult(connectionGood, leaseGranted, leaseLength);
  }
}
