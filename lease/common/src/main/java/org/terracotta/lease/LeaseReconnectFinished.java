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
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.encoding.StructEncoder;

import java.util.UUID;

/**
 * A message that the ActiveLeaseAcquirer sends to itself so that it knows the reconnection process is completed
 * and that messages are being delivered again. It uses the UUID to stand for the ClientDescriptor.
 */
public class LeaseReconnectFinished implements LeaseMessage {
  private static final Struct STRUCT = StructBuilder.newStructBuilder()
      .int64("uuidMSB", 10)
      .int64("uuidLSB", 20)
      .build();
  private static final Int64Field UUID_MSB = STRUCT.field("uuidMSB", Int64Field.class);
  private static final Int64Field UUID_LSB = STRUCT.field("uuidLSB", Int64Field.class);

  private final UUID uuid;

  public LeaseReconnectFinished(UUID uuid) {
    this.uuid = uuid;
  }

  public UUID getUUID() {
    return uuid;
  }

  @Override
  public LeaseMessageType getType() {
    return LeaseMessageType.LEASE_RECONNECT_FINISHED;
  }

  public static void addStruct(StructBuilder parentBuilder, int index) {
    parentBuilder.struct("leaseReconnectFinished", index, STRUCT);
  }

  @Override
  public void encode(StructEncoder<Void> parentEncoder) {
    StructEncoder<StructEncoder<Void>> encoder = parentEncoder.struct("leaseReconnectFinished");
    encoder.int64(UUID_MSB, uuid.getMostSignificantBits());
    encoder.int64(UUID_LSB, uuid.getLeastSignificantBits());
    encoder.end();
  }

  public static LeaseMessage decode(StructDecoder<?> parentDecoder) {
    StructDecoder<?> decoder = parentDecoder.struct("leaseReconnectFinished");
    long uuidMSB = decoder.int64(UUID_MSB, 0L);
    long uuidLSB = decoder.int64(UUID_LSB, 0L);

    UUID uuid = new UUID(uuidMSB, uuidLSB);
    return new LeaseReconnectFinished(uuid);
  }
}
