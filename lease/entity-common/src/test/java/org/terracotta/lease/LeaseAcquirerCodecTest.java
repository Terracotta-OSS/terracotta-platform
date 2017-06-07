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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LeaseAcquirerCodecTest {
  @Test
  public void roundtripRequest() throws Exception {
    LeaseRequest request = new LeaseRequest();
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    byte[] bytes = codec.encodeMessage(request);
    assertEquals(0, bytes.length);
    LeaseRequest roundtrippedRequest = codec.decodeMessage(bytes);
    assertNotNull(roundtrippedRequest);
  }

  @Test
  public void roundtripNotGrantedResponse() throws Exception {
    LeaseResponse response = LeaseResponse.leaseNotGranted();
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    byte[] bytes = codec.encodeResponse(response);
    LeaseResponse roundtrippedResponse = codec.decodeResponse(bytes);
    assertFalse(roundtrippedResponse.isLeaseGranted());
  }

  @Test
  public void roundtripGrantedResponse() throws Exception {
    LeaseResponse response = LeaseResponse.leaseGranted(500L);
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    byte[] bytes = codec.encodeResponse(response);
    LeaseResponse roundtrippedResponse = codec.decodeResponse(bytes);
    assertTrue(roundtrippedResponse.isLeaseGranted());
    assertEquals(500L, roundtrippedResponse.getLeaseLength());
  }
}
