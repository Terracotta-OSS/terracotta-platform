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

import java.nio.ByteBuffer;

public class LeaseReconnectData {
  private static Struct reconnectStruct = createStruct();

  private long connectionSequenceNumber;

  public LeaseReconnectData(long connectionSequenceNumber) {
    this.connectionSequenceNumber = connectionSequenceNumber;
  }

  public long getConnectionSequenceNumber() {
    return connectionSequenceNumber;
  }

  private static Struct createStruct() {
    StructBuilder builder = StructBuilder.newStructBuilder();
    builder.int64("connectionSequenceNumber", 10);
    return builder.build();
  }

  public byte[] encode() {
    StructEncoder<Void> encoder = reconnectStruct.encoder();
    encoder.int64("connectionSequenceNumber", connectionSequenceNumber);
    return encoder.encode().array();
  }

  public static LeaseReconnectData decode(byte[] bytes) {
    StructDecoder<Void> decoder = reconnectStruct.decoder(ByteBuffer.wrap(bytes));
    long connectionSequenceNumber = decoder.int64("connectionSequenceNumber");
    return new LeaseReconnectData(connectionSequenceNumber);
  }
}
