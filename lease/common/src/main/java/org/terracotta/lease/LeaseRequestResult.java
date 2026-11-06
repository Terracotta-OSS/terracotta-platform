/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
import org.terracotta.runnel.decoding.fields.BoolField;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.encoding.StructEncoder;

/**
 * A message sent from the server entity to the client entity to indicate the response to the LeaseRequest.
 */
public class LeaseRequestResult implements LeaseResponse {
  private static final Struct STRUCT = StructBuilder.newStructBuilder()
      .bool("connectionGood", 10)
      .bool("leaseGranted", 20)
      .int64("leaseLength", 30)
      .build();
  private static final BoolField CONNECTION_GOOD = STRUCT.field("connectionGood", BoolField.class);
  private static final BoolField LEASE_GRANTED = STRUCT.field("leaseGranted", BoolField.class);
  private static final Int64Field LEASE_LENGTH = STRUCT.field("leaseLength", Int64Field.class);

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
    parentBuilder.struct("leaseRequestResult", index, STRUCT);
  }

  @Override
  public void encode(StructEncoder<Void> parentEncoder) {
    StructEncoder<StructEncoder<Void>> encoder = parentEncoder.struct("leaseRequestResult");
    encoder.bool(CONNECTION_GOOD, connectionGood);
    encoder.bool(LEASE_GRANTED, leaseGranted);
    encoder.int64(LEASE_LENGTH, leaseLength);
    encoder.end();
  }

  public static LeaseResponse decode(StructDecoder<?> parentDecoder) {
    StructDecoder<?> decoder = parentDecoder.struct("leaseRequestResult");
    boolean connectionGood = decoder.bool(CONNECTION_GOOD, false);
    boolean leaseGranted = decoder.bool(LEASE_GRANTED, false);
    long leaseLength = decoder.int64(LEASE_LENGTH, -1L);
    return new LeaseRequestResult(connectionGood, leaseGranted, leaseLength);
  }
}
