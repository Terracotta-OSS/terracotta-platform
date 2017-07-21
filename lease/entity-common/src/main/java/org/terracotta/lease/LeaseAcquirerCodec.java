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

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.runnel.EnumMapping;
import org.terracotta.runnel.EnumMappingBuilder;
import org.terracotta.runnel.Struct;
import org.terracotta.runnel.StructBuilder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.encoding.StructEncoder;

import java.nio.ByteBuffer;

/**
 * The codec responsible for converting back and forth between bytes and lease messages.
 */
public class LeaseAcquirerCodec implements MessageCodec<LeaseMessage, LeaseResponse> {
  private static Struct messageStruct = createMessageStruct();
  private static Struct responseStruct = createResponseStruct();

  @Override
  public byte[] encodeMessage(LeaseMessage leaseMessage) throws MessageCodecException {
    StructEncoder<Void> encoder = messageStruct.encoder();
    encoder.enm("messageType", leaseMessage.getType());
    leaseMessage.encode(encoder);
    return encoder.encode().array();
  }

  @Override
  public LeaseMessage decodeMessage(byte[] bytes) throws MessageCodecException {
    StructDecoder<Void> decoder = messageStruct.decoder(ByteBuffer.wrap(bytes));
    LeaseMessageType type = decoder.<LeaseMessageType>enm("messageType").get();
    return type.decode(decoder);
  }

  @Override
  public byte[] encodeResponse(LeaseResponse leaseResponse) throws MessageCodecException {
    StructEncoder<Void> encoder = responseStruct.encoder();
    encoder.enm("responseType", leaseResponse.getType());
    leaseResponse.encode(encoder);
    return encoder.encode().array();
  }

  @Override
  public LeaseResponse decodeResponse(byte[] bytes) throws MessageCodecException {
    StructDecoder<Void> decoder = responseStruct.decoder(ByteBuffer.wrap(bytes));
    LeaseResponseType type = decoder.<LeaseResponseType>enm("responseType").get();
    return type.decode(decoder);
  }

  private static Struct createMessageStruct() {
    StructBuilder builder = StructBuilder.newStructBuilder();
    builder.enm("messageType", 10, createMessageTypeMapping());
    LeaseRequest.addStruct(builder, 20);
    LeaseReconnectFinished.addStruct(builder, 30);
    return builder.build();
  }

  private static Struct createResponseStruct() {
    StructBuilder builder = StructBuilder.newStructBuilder();
    builder.enm("responseType", 10, createResponseTypeMapping());
    LeaseRequestResult.addStruct(builder, 20);
    LeaseAcquirerAvailable.addStruct(builder, 30);
    IgnoredLeaseResponse.addStruct(builder, 40);
    return builder.build();
  }

  private static Struct createReconnectStruct() {
    StructBuilder builder = StructBuilder.newStructBuilder();
    builder.int64("connectionSequenceNumber", 10);
    return builder.build();
  }

  private static EnumMapping createMessageTypeMapping() {
    EnumMappingBuilder<LeaseMessageType> mapping = EnumMappingBuilder.newEnumMappingBuilder(LeaseMessageType.class);
    mapping.mapping(LeaseMessageType.LEASE_REQUEST, 1);
    mapping.mapping(LeaseMessageType.LEASE_RECONNECT_FINISHED, 2);
    return mapping.build();
  }

  private static EnumMapping createResponseTypeMapping() {
    EnumMappingBuilder<LeaseResponseType> mapping = EnumMappingBuilder.newEnumMappingBuilder(LeaseResponseType.class);
    mapping.mapping(LeaseResponseType.LEASE_REQUEST_RESULT, 1);
    mapping.mapping(LeaseResponseType.LEASE_ACQUIRER_AVAILABLE, 2);
    mapping.mapping(LeaseResponseType.IGNORED_LEASE_RESPONSE, 3);
    return mapping.build();
  }
}
