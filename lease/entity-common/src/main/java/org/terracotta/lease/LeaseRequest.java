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
import org.terracotta.runnel.utils.RunnelDecodingException;

/**
 * A message to send from the client entity to the server entity to request a lease.
 */
public class LeaseRequest implements LeaseMessage {
  private final long connectionSequenceNumber;

  public LeaseRequest(long connectionSequenceNumber) {
    this.connectionSequenceNumber = connectionSequenceNumber;
  }

  public long getConnectionSequenceNumber() {
    return connectionSequenceNumber;
  }

  @Override
  public LeaseMessageType getType() {
    return LeaseMessageType.LEASE_REQUEST;
  }

  public static void addStruct(StructBuilder parentBuilder, int index) {
    StructBuilder builder = StructBuilder.newStructBuilder();
    builder.int64("connectionSequenceNumber", 10);
    Struct struct = builder.build();

    parentBuilder.struct("leaseRequest", index, struct);
  }

  @Override
  public void encode(StructEncoder<Void> parentEncoder) {
    StructEncoder<StructEncoder<Void>> encoder = parentEncoder.struct("leaseRequest");
    encoder.int64("connectionSequenceNumber", connectionSequenceNumber);
    encoder.end();
  }

  public static LeaseMessage decode(StructDecoder<Void> parentDecoder) throws RunnelDecodingException {
    StructDecoder<StructDecoder<Void>> decoder = parentDecoder.mandatoryStruct("leaseRequest");
    long connectionSequenceNumber = decoder.mandatoryInt64("connectionSequenceNumber");
    return new LeaseRequest(connectionSequenceNumber);
  }
}
