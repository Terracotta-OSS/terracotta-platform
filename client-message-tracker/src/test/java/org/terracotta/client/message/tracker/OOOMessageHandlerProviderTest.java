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
package org.terracotta.client.message.tracker;

import org.junit.Test;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.ServiceConfiguration;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class OOOMessageHandlerProviderTest {

  @Test
  public void getService() throws Exception {
    OOOMessageHandlerProvider provider =  new OOOMessageHandlerProvider();
    OOOMessageHandlerConfiguration<EntityMessage, EntityResponse> config =
        new OOOMessageHandlerConfiguration<>("foo", null, 1, m -> 0);
    OOOMessageHandler<EntityMessage, EntityResponse> messageHandler = provider.getService(1L, config);
    assertThat(provider.getService(2L, config), sameInstance(messageHandler));

    config = new OOOMessageHandlerConfiguration<>("bar", null, 1, m -> 0);
    assertThat(provider.getService(2L, config), not(sameInstance(messageHandler)));
  }

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalArgumentException.class)
  public void getServiceInvalidConfig() throws Exception {
    OOOMessageHandlerProvider provider =  new OOOMessageHandlerProvider();
    provider.getService(1L, mock(ServiceConfiguration.class));
  }

}
