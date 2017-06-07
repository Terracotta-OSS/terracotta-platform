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

import java.nio.ByteBuffer;

/**
 * The codec responsible for converting back and forth between bytes and lease messages.
 */
public class LeaseAcquirerCodec implements MessageCodec<LeaseRequest, LeaseResponse> {
  @Override
  public byte[] encodeMessage(LeaseRequest leaseRequest) throws MessageCodecException {
    return new byte[0];
  }

  @Override
  public LeaseRequest decodeMessage(byte[] bytes) throws MessageCodecException {
    return new LeaseRequest();
  }

  @Override
  public byte[] encodeResponse(LeaseResponse leaseResponse) throws MessageCodecException {
    if (leaseResponse.isLeaseGranted()) {
      long leaseLength = leaseResponse.getLeaseLength();
      return ByteBuffer.allocate(8).putLong(leaseLength).array();
    } else {
      return ByteBuffer.allocate(8).putLong(-1L).array();
    }
  }

  @Override
  public LeaseResponse decodeResponse(byte[] bytes) throws MessageCodecException {
    long leaseLength = ByteBuffer.wrap(bytes).getLong();
    if (leaseLength > 0) {
      return LeaseResponse.leaseGranted(leaseLength);
    } else {
      return LeaseResponse.leaseNotGranted();
    }
  }
}
