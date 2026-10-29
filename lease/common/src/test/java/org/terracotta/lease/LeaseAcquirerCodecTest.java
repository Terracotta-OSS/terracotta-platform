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

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LeaseAcquirerCodecTest {

  // golden wire frames, captured from the pre-migration codec; any change to these bytes is
  // a wire-format change and must be deliberate
  private static final String GOLDEN_LEASE_REQUEST = "0f0a0101140a0a08000000000000002a";
  private static final String GOLDEN_RECONNECT_FINISHED = "190a01021e140a086ba7b8109dad11d1140880b400c04fd430c8";
  private static final String GOLDEN_NOT_GRANTED = "150a010114100a01011401001e08ffffffffffffffff";
  private static final String GOLDEN_GRANTED = "150a010114100a01011401011e080000000000002710";
  private static final String GOLDEN_OLD_CONNECTION = "150a010114100a01001401001e08ffffffffffffffff";
  private static final String GOLDEN_ACQUIRER_AVAILABLE = "030a0102";
  private static final String GOLDEN_IGNORED_RESPONSE = "030a0103";
  private static final UUID GOLDEN_UUID = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

  private static String hex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(Character.forDigit(b >> 4 & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
    }
    return sb.toString();
  }

  private static byte[] fromHex(String hex) {
    byte[] bytes = new byte[hex.length() / 2];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
    }
    return bytes;
  }

  @Test
  public void leaseRequestGoldenBytes() throws Exception {
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    assertEquals(GOLDEN_LEASE_REQUEST, hex(codec.encodeMessage(new LeaseRequest(42L))));
    LeaseRequest decoded = (LeaseRequest) codec.decodeMessage(fromHex(GOLDEN_LEASE_REQUEST));
    assertEquals(42L, decoded.getConnectionSequenceNumber());
    assertEquals(GOLDEN_LEASE_REQUEST, hex(codec.encodeMessage(decoded)));
  }

  @Test
  public void reconnectFinishedGoldenBytes() throws Exception {
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    assertEquals(GOLDEN_RECONNECT_FINISHED, hex(codec.encodeMessage(new LeaseReconnectFinished(GOLDEN_UUID))));
    LeaseReconnectFinished decoded = (LeaseReconnectFinished) codec.decodeMessage(fromHex(GOLDEN_RECONNECT_FINISHED));
    assertEquals(GOLDEN_UUID, decoded.getUUID());
    assertEquals(GOLDEN_RECONNECT_FINISHED, hex(codec.encodeMessage(decoded)));
  }

  @Test
  public void notGrantedGoldenBytes() throws Exception {
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    assertEquals(GOLDEN_NOT_GRANTED, hex(codec.encodeResponse(LeaseRequestResult.leaseNotGranted())));
    LeaseRequestResult decoded = (LeaseRequestResult) codec.decodeResponse(fromHex(GOLDEN_NOT_GRANTED));
    assertTrue(decoded.isConnectionGood());
    assertFalse(decoded.isLeaseGranted());
    assertEquals(GOLDEN_NOT_GRANTED, hex(codec.encodeResponse(decoded)));
  }

  @Test
  public void grantedGoldenBytes() throws Exception {
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    assertEquals(GOLDEN_GRANTED, hex(codec.encodeResponse(LeaseRequestResult.leaseGranted(10000L))));
    LeaseRequestResult decoded = (LeaseRequestResult) codec.decodeResponse(fromHex(GOLDEN_GRANTED));
    assertTrue(decoded.isConnectionGood());
    assertTrue(decoded.isLeaseGranted());
    assertEquals(10000L, decoded.getLeaseLength());
    assertEquals(GOLDEN_GRANTED, hex(codec.encodeResponse(decoded)));
  }

  @Test
  public void oldConnectionGoldenBytes() throws Exception {
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    assertEquals(GOLDEN_OLD_CONNECTION, hex(codec.encodeResponse(LeaseRequestResult.oldConnection())));
    LeaseRequestResult decoded = (LeaseRequestResult) codec.decodeResponse(fromHex(GOLDEN_OLD_CONNECTION));
    assertFalse(decoded.isConnectionGood());
    assertFalse(decoded.isLeaseGranted());
    assertEquals(GOLDEN_OLD_CONNECTION, hex(codec.encodeResponse(decoded)));
  }

  @Test
  public void acquirerAvailableGoldenBytes() throws Exception {
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    assertEquals(GOLDEN_ACQUIRER_AVAILABLE, hex(codec.encodeResponse(new LeaseAcquirerAvailable())));
    assertNotNull(codec.decodeResponse(fromHex(GOLDEN_ACQUIRER_AVAILABLE)));
    assertEquals(GOLDEN_ACQUIRER_AVAILABLE, hex(codec.encodeResponse(new LeaseAcquirerAvailable())));
  }

  @Test
  public void ignoredResponseGoldenBytes() throws Exception {
    LeaseAcquirerCodec codec = new LeaseAcquirerCodec();
    assertEquals(GOLDEN_IGNORED_RESPONSE, hex(codec.encodeResponse(new IgnoredLeaseResponse())));
    assertNotNull(codec.decodeResponse(fromHex(GOLDEN_IGNORED_RESPONSE)));
    assertEquals(GOLDEN_IGNORED_RESPONSE, hex(codec.encodeResponse(new IgnoredLeaseResponse())));
  }

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
