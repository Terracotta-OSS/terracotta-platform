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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.MessageCodec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LeaseAcquirerClientServiceTest {
  @Mock
  private EntityClientEndpoint<LeaseMessage, LeaseResponse> endpoint;

  @Test
  public void handlesCorrectClass() {
    LeaseAcquirerClientService clientService = new LeaseAcquirerClientService();
    assertTrue(clientService.handlesEntityType(LeaseAcquirer.class));
  }

  @Test
  public void roundtripConfiguration() {
    LeaseAcquirerClientService clientService = new LeaseAcquirerClientService();
    byte[] bytes = clientService.serializeConfiguration(null);
    assertEquals(0, bytes.length);
    Void result = clientService.deserializeConfiguration(bytes);
    assertNull(result);
  }

  @Test
  public void createEntity() {
    LeaseAcquirerClientService clientService = new LeaseAcquirerClientService();
    LeaseAcquirer leaseAcquirer = clientService.create(endpoint, null);
    leaseAcquirer.close();
    verify(endpoint).close();
  }

  @Test
  public void createCodec() {
    LeaseAcquirerClientService clientService = new LeaseAcquirerClientService();
    MessageCodec<LeaseMessage, LeaseResponse> messageCodec = clientService.getMessageCodec();
    assertTrue(messageCodec instanceof LeaseAcquirerCodec);
  }
}
