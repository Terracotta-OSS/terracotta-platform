/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import org.terracotta.runnel.StructBuilder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.encoding.StructEncoder;

/**
 * A message that is sent to the client to indicate that the reconnection process has completed and now lease requests
 * can be resumed.
 */
public class LeaseAcquirerAvailable implements LeaseResponse {
  @Override
  public LeaseResponseType getType() {
    return LeaseResponseType.LEASE_ACQUIRER_AVAILABLE;
  }

  public static void addStruct(StructBuilder parentBuilder, int index) {
  }

  @Override
  public void encode(StructEncoder<Void> parentEncoder) {
  }

  public static LeaseResponse decode(StructDecoder<Void> parentDecoder) {
    return new LeaseAcquirerAvailable();
  }
}
