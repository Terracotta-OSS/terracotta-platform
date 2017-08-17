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

import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvokeContext;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OOOMessageHandlerImplTest {

  private OOOMessageHandler<EntityMessage, EntityResponse> messageHandler;

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testInvokeCachesResponse() throws Exception {
    messageHandler = new OOOMessageHandlerImpl<>(msg -> true);

    InvokeContext context = new DummyContext(new DummyClientSourceId(1), 25, 18);
    EntityMessage message = mock(EntityMessage.class);
    EntityResponse response1 = mock(EntityResponse.class);
    EntityResponse response2 = mock(EntityResponse.class);

    EntityResponse entityResponse1 = messageHandler.invoke(context, message, (ctxt, msg) -> response1);
    assertThat(entityResponse1, is(response1));

    EntityResponse entityResponse2 = messageHandler.invoke(context, message, (ctxt, msg) -> response2);
    assertThat(entityResponse2, is(entityResponse1));
  }

  @Test
  public void testInvokeDoesNotCacheUntrackableResponse() throws Exception {
    messageHandler = new OOOMessageHandlerImpl<>(msg -> false);

    InvokeContext context = new DummyContext(new DummyClientSourceId(1), 25, 18);
    EntityMessage message = mock(EntityMessage.class);

    EntityResponse response1 = mock(EntityResponse.class);
    EntityResponse entityResponse1 = messageHandler.invoke(context, message, (ctxt, msg) -> response1);
    assertThat(entityResponse1, is(response1));

    EntityResponse response2 = mock(EntityResponse.class);
    EntityResponse entityResponse2 = messageHandler.invoke(context, message, (ctxt, msg) -> response2);
    assertThat(entityResponse2, is(response2));
  }

  @Test
  public void testInvokeDoesNotCacheMessagesNotFromRealClients() throws Exception {
    messageHandler = new OOOMessageHandlerImpl<>(null);

    InvokeContext context = mock(InvokeContext.class);
    when(context.isValidClientInformation()).thenReturn(false);
    EntityMessage message = mock(EntityMessage.class);

    EntityResponse response1 = mock(EntityResponse.class);
    EntityResponse entityResponse1 = messageHandler.invoke(context, message, (ctxt, msg) -> response1);
    assertThat(entityResponse1, is(response1));

    EntityResponse response2 = mock(EntityResponse.class);
    EntityResponse entityResponse2 = messageHandler.invoke(context, message, (ctxt, msg) -> response2);
    assertThat(entityResponse2, is(response2));
  }

  @Test
  public void testResentMessageWithSameCurrentAndOldestTxnId() throws Exception {
    messageHandler = new OOOMessageHandlerImpl<>(msg -> true);

    InvokeContext context = new DummyContext(new DummyClientSourceId(1), 25, 25);
    EntityMessage message = mock(EntityMessage.class);

    EntityResponse entityResponse1 = messageHandler.invoke(context, message, (ctxt, msg) -> mock(EntityResponse.class));

    EntityResponse entityResponse2 = messageHandler.invoke(context, message, (ctxt, msg) -> mock(EntityResponse.class));
    assertThat(entityResponse2, sameInstance(entityResponse1));
  }

  private static class DummyContext implements InvokeContext {

    private final ClientSourceId clientSourceId;
    private final long currentTransactionId;
    private final long oldestTransactionId;

    public DummyContext(ClientSourceId clientSourceId, long currentTransactionId, long oldestTransactionId) {
      this.clientSourceId = clientSourceId;
      this.currentTransactionId = currentTransactionId;
      this.oldestTransactionId = oldestTransactionId;
    }

    @Override
    public ClientSourceId getClientSource() {
      return clientSourceId;
    }

    @Override
    public long getCurrentTransactionId() {
      return currentTransactionId;
    }

    @Override
    public long getOldestTransactionId() {
      return oldestTransactionId;
    }

    @Override
    public boolean isValidClientInformation() {
      return true;
    }

    @Override
    public ClientSourceId makeClientSourceId(long l) {
      return new DummyClientSourceId(l);
    }
  }
}
