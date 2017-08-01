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

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LeaseAcquirerCodecTest {
  @Test
  public void roundtripLeaseRequest() throws Exception {
    LeaseRequest message = new LeaseRequest(5);
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    byte[] bytes = codec.encodeMessage(message);
    LeaseRequest roundtrippedMessage = (LeaseRequest) codec.decodeMessage(bytes);
    assertEquals(5, roundtrippedMessage.getConnectionSequenceNumber());
  }

  @Test
  public void roundtripLeaseReconnectFinished() throws Exception {
    UUID uuid = UUID.randomUUID();
    LeaseReconnectFinished message = new LeaseReconnectFinished(uuid);
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    byte[] bytes = codec.encodeMessage(message);
    LeaseReconnectFinished roundtrippedMessage = (LeaseReconnectFinished) codec.decodeMessage(bytes);
    assertEquals(uuid, roundtrippedMessage.getUUID());
  }

  @Test
  public void roundtripOldConnectionResponse() throws Exception {
    LeaseRequestResult response = LeaseRequestResult.oldConnection();
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    byte[] bytes = codec.encodeResponse(response);
    LeaseRequestResult roundtrippedResponse = (LeaseRequestResult) codec.decodeResponse(bytes);
    assertFalse(roundtrippedResponse.isConnectionGood());
    assertFalse(roundtrippedResponse.isLeaseGranted());
  }

  @Test
  public void roundtripNotGrantedResponse() throws Exception {
    LeaseRequestResult response = LeaseRequestResult.leaseNotGranted();
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    byte[] bytes = codec.encodeResponse(response);
    LeaseRequestResult roundtrippedResponse = (LeaseRequestResult) codec.decodeResponse(bytes);
    assertTrue(roundtrippedResponse.isConnectionGood());
    assertFalse(roundtrippedResponse.isLeaseGranted());
  }

  @Test
  public void roundtripGrantedResponse() throws Exception {
    LeaseRequestResult response = LeaseRequestResult.leaseGranted(500L);
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    byte[] bytes = codec.encodeResponse(response);
    LeaseRequestResult roundtrippedResponse = (LeaseRequestResult) codec.decodeResponse(bytes);
    assertTrue(roundtrippedResponse.isConnectionGood());
    assertTrue(roundtrippedResponse.isLeaseGranted());
    assertEquals(500L, roundtrippedResponse.getLeaseLength());
  }

  @Test
  public void roundtripLeaseAcquirerAvailable() throws Exception {
    LeaseAcquirerAvailable response = new LeaseAcquirerAvailable();
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    byte[] bytes = codec.encodeResponse(response);
    LeaseAcquirerAvailable roundtrippedResponse = (LeaseAcquirerAvailable) codec.decodeResponse(bytes);
    assertNotNull(roundtrippedResponse);
  }

  @Test
  public void roundtripIgnoredLeaseResponse() throws Exception {
    IgnoredLeaseResponse response = new IgnoredLeaseResponse();
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    byte[] bytes = codec.encodeResponse(response);
    IgnoredLeaseResponse roundtrippedResponse = (IgnoredLeaseResponse) codec.decodeResponse(bytes);
    assertNotNull(roundtrippedResponse);
  }
}
