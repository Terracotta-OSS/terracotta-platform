/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

public enum LeaseResponseType {
  LEASE_REQUEST_RESULT(LeaseRequestResult::decode),
  LEASE_ACQUIRER_AVAILABLE(LeaseAcquirerAvailable::decode),
  IGNORED_LEASE_RESPONSE(IgnoredLeaseResponse::decode);

  private final LeaseResponseDecoder leaseResponseDecoder;

  LeaseResponseType(LeaseResponseDecoder leaseResponseDecoder) {
    this.leaseResponseDecoder = leaseResponseDecoder;
  }

  public LeaseResponse decode(StructDecoder<Void> decoder) {
    return leaseResponseDecoder.decode(decoder);
  }
}
