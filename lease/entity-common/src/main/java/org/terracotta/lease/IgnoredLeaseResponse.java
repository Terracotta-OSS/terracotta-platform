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

import org.terracotta.runnel.StructBuilder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.encoding.StructEncoder;

/**
 * A LeaseResponse that the ActiveLeaseAcquirer can return in response to a self message
 */
public class IgnoredLeaseResponse implements LeaseResponse {
  @Override
  public LeaseResponseType getType() {
    return LeaseResponseType.IGNORED_LEASE_RESPONSE;
  }

  public static void addStruct(StructBuilder parentBuilder, int index) {
  }

  @Override
  public void encode(StructEncoder<Void> parentEncoder) {
  }

  public static LeaseResponse decode(StructDecoder<Void> parentDecoder) {
    return new IgnoredLeaseResponse();
  }
}
