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

import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.utils.RunnelDecodingException;

enum LeaseMessageType {
  LEASE_REQUEST(new LeaseMessageDecoder() {
    @Override
    public LeaseMessage decode(StructDecoder<Void> parentDecoder) throws RunnelDecodingException {
      return LeaseRequest.decode(parentDecoder);
    }
  }),
  LEASE_RECONNECT_FINISHED(new LeaseMessageDecoder() {
    @Override
    public LeaseMessage decode(StructDecoder<Void> parentDecoder) throws RunnelDecodingException {
      return LeaseReconnectFinished.decode(parentDecoder);
    }
  });

  private final LeaseMessageDecoder leaseMessageDecoder;

  LeaseMessageType(LeaseMessageDecoder leaseMessageDecoder) {
    this.leaseMessageDecoder = leaseMessageDecoder;
  }

  public LeaseMessage decode(StructDecoder<Void> decoder) throws RunnelDecodingException {
    return leaseMessageDecoder.decode(decoder);
  }
}
