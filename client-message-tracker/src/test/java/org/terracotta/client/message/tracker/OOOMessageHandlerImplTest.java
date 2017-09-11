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
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OOOMessageHandlerImplTest {

  private OOOMessageHandler<EntityMessage, EntityResponse> messageHandler;

  @Before
  public void setUp() throws Exception {
    messageHandler = new OOOMessageHandlerImpl<>(msg -> true, 1, m -> 0);
  }

  @Test
  public void testInvokeCachesResponse() throws Exception {
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
    messageHandler = new OOOMessageHandlerImpl<>(msg -> false, 1, m -> 0);

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
    messageHandler = new OOOMessageHandlerImpl<>(msg -> true, 1, null);

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
    InvokeContext context = new DummyContext(new DummyClientSourceId(1), 25, 25);
    EntityMessage message = mock(EntityMessage.class);

    EntityResponse entityResponse1 = messageHandler.invoke(context, message, (ctxt, msg) -> mock(EntityResponse.class));

    EntityResponse entityResponse2 = messageHandler.invoke(context, message, (ctxt, msg) -> mock(EntityResponse.class));
    assertThat(entityResponse2, sameInstance(entityResponse1));
  }

  @Test
  public void testSegmentation() throws Exception {
    EntityMessage message1 = mock(EntityMessage.class);
    EntityMessage message2 = mock(EntityMessage.class);
    EntityMessage message3 = mock(EntityMessage.class);
    EntityMessage message4 = mock(EntityMessage.class);

    ToIntFunction<EntityMessage> segmentationStrategy = m -> {
      if (m == message1 || m == message3) {
        return 0;
      } else if (m == message2 || m == message4) {
        return 1;
      } else {
        return -1;
      }
    };

    messageHandler = new OOOMessageHandlerImpl<>(m -> true, 2, segmentationStrategy);

    DummyClientSourceId clientSourceId1 = new DummyClientSourceId(1);
    DummyClientSourceId clientSourceId2 = new DummyClientSourceId(2);
    long txnId1 = 25;
    long txnId2 = 26;
    long txnId3 = 27;
    long txnId4 = 32;
    InvokeContext context1 = new DummyContext(clientSourceId1, txnId1, 18);
    InvokeContext context2 = new DummyContext(clientSourceId1, txnId2, 18);
    InvokeContext context3 = new DummyContext(clientSourceId1, txnId3, 18);
    InvokeContext context4 = new DummyContext(clientSourceId2, txnId4, 28);

    EntityResponse response1 = mock(EntityResponse.class);
    EntityResponse response2 = mock(EntityResponse.class);
    EntityResponse response3 = mock(EntityResponse.class);
    EntityResponse response4 = mock(EntityResponse.class);

    EntityResponse entityResponse1 = messageHandler.invoke(context1, message1, (ctxt, msg) -> response1);  //message mapped to segment 0
    assertThat(entityResponse1, is(response1));

    EntityResponse entityResponse2 = messageHandler.invoke(context2, message2, (ctxt, msg) -> response2);  //message mapped to segment 1
    assertThat(entityResponse2, is(response2));

    EntityResponse entityResponse3 = messageHandler.invoke(context3, message3, (ctxt, msg) -> response3);  //message mapped to segment 0
    assertThat(entityResponse3, is(response3));

    EntityResponse entityResponse4 = messageHandler.invoke(context4, message4, (ctxt, msg) -> response4);  //message mapped to segment 1
    assertThat(entityResponse4, is(response4));

    Map<Long, EntityResponse> trackedResponsesForClient1Segment0 = messageHandler.getTrackedResponsesForSegment(0, clientSourceId1);
    assertThat(trackedResponsesForClient1Segment0.size(), is(2));
    assertThat(trackedResponsesForClient1Segment0.get(txnId1), is(response1));
    assertThat(trackedResponsesForClient1Segment0.get(txnId3), is(response3));

    Map<Long, EntityResponse> trackedResponsesForClient1Segment1 = messageHandler.getTrackedResponsesForSegment(1, clientSourceId1);
    assertThat(trackedResponsesForClient1Segment1.size(), is(1));
    assertThat(trackedResponsesForClient1Segment1.get(txnId2), is(response2));

    Map<Long, EntityResponse> trackedResponsesForClient2Segment0 = messageHandler.getTrackedResponsesForSegment(0, clientSourceId2);
    assertThat(trackedResponsesForClient2Segment0.size(), is(0));

    Map<Long, EntityResponse> trackedResponsesForClient2Segment1 = messageHandler.getTrackedResponsesForSegment(1, clientSourceId2);
    assertThat(trackedResponsesForClient2Segment1.size(), is(1));
    assertThat(trackedResponsesForClient2Segment1.get(txnId4), is(response4));
  }

  @Test
  public void testLoadTrackedResponsesForSegment() throws Exception {
    messageHandler = new OOOMessageHandlerImpl<>(m -> true, 2, m -> 0);

    EntityResponse response1 = mock(EntityResponse.class);
    EntityResponse response2 = mock(EntityResponse.class);

    Map<Long, EntityResponse> trackedResponsesForSegment1 = new HashMap<>();
    trackedResponsesForSegment1.put(1L, response1);
    trackedResponsesForSegment1.put(2L, response2);

    Map<Long, EntityResponse> trackedResponsesForSegment2 = new HashMap<>();
    trackedResponsesForSegment2.put(3L, response1);
    trackedResponsesForSegment2.put(4L, response2);

    DummyClientSourceId clientSourceId = new DummyClientSourceId(1);
    messageHandler.loadTrackedResponsesForSegment(0, clientSourceId, trackedResponsesForSegment1);
    messageHandler.loadTrackedResponsesForSegment(1, clientSourceId, trackedResponsesForSegment2);

    Map<Long, EntityResponse> trackedResponses1 = messageHandler.getTrackedResponsesForSegment(0, clientSourceId);
    assertThat(trackedResponses1.size(), is(2));
    assertThat(trackedResponsesForSegment1, is(trackedResponsesForSegment1));

    Map<Long, EntityResponse> trackedResponses2 = messageHandler.getTrackedResponsesForSegment(1, clientSourceId);
    assertThat(trackedResponses2.size(), is(2));
    assertThat(trackedResponses2, is(trackedResponsesForSegment2));
  }

  @Test
  public void testLoadOnSyncWithOldServer() throws Exception {
    messageHandler = new OOOMessageHandlerImpl<>(m -> true, 2, m -> 0);

    long txnId1 = 25;
    long txnId2 = 26;
    EntityResponse response1 = mock(EntityResponse.class);
    EntityResponse response2 = mock(EntityResponse.class);

    Map<Long, EntityResponse> trackedResponsesForSharedTracker = new HashMap<>();
    trackedResponsesForSharedTracker.put(txnId1, response1);
    trackedResponsesForSharedTracker.put(txnId2, response2);

    DummyClientSourceId clientSourceId = new DummyClientSourceId(1);
    messageHandler.loadOnSync(clientSourceId, trackedResponsesForSharedTracker);

    assertThat(messageHandler.getTrackedResponsesForSegment(0, clientSourceId).size(), is(0));
    assertThat(messageHandler.getTrackedResponsesForSegment(1, clientSourceId).size(), is(0));

    InvokeContext context1 = new DummyContext(clientSourceId, txnId1, 18);
    InvokeContext context2 = new DummyContext(clientSourceId, txnId2, txnId2);
    EntityMessage message1 = mock(EntityMessage.class);
    EntityMessage message2 = mock(EntityMessage.class);

    EntityResponse entityResponse = messageHandler.invoke(context1, message1, null);
    assertThat(entityResponse, is(response1));
    entityResponse = messageHandler.invoke(context2, message2, null);
    assertThat(entityResponse, is(response2));

    EntityResponse response3 = mock(EntityResponse.class);
    entityResponse = messageHandler.invoke(context1, message1, (ctxt, msg) -> response3); //Previous invoke should have reconciled the cached response1 for context1
    assertThat(entityResponse, is(response3));
  }

  @Test
  public void testGetTrackedClients() throws Exception {
    EntityMessage message1 = mock(EntityMessage.class);
    EntityMessage message2 = mock(EntityMessage.class);
    EntityMessage message3 = mock(EntityMessage.class);
    EntityMessage message4 = mock(EntityMessage.class);

    ToIntFunction<EntityMessage> segmentationStrategy = m -> {
      if (m == message1 || m == message3) {
        return 0;
      } else if (m == message2 || m == message4) {
        return 1;
      } else {
        return -1;
      }
    };

    Predicate<EntityMessage> trackerPolicy = msg -> {
      if (msg == message3) {
        return false;
      } else {
        return true;
      }
    };

    messageHandler = new OOOMessageHandlerImpl<>(trackerPolicy, 2, segmentationStrategy);

    DummyClientSourceId clientSourceId1 = new DummyClientSourceId(1);
    DummyClientSourceId clientSourceId2 = new DummyClientSourceId(2);
    DummyClientSourceId clientSourceId3 = new DummyClientSourceId(3);

    InvokeContext context1 = new DummyContext(clientSourceId1, 25, 18);
    InvokeContext context2 = new DummyContext(clientSourceId2, 26, 18);
    InvokeContext context3 = new DummyContext(clientSourceId3, 27, 18);
    InvokeContext context4 = new DummyContext(clientSourceId1, 32, 28);

    messageHandler.invoke(context1, message1, (ctxt, msg) -> mock(EntityResponse.class));
    messageHandler.invoke(context2, message2, (ctxt, msg) -> mock(EntityResponse.class));
    messageHandler.invoke(context3, message3, (ctxt, msg) -> mock(EntityResponse.class));
    messageHandler.invoke(context4, message4, (ctxt, msg) -> mock(EntityResponse.class));

    Set<ClientSourceId> clients = messageHandler.getTrackedClients().collect(toSet());
    assertThat(clients.size(), is(2));
    assertThat(clients.contains(clientSourceId1), is(true));
    assertThat(clients.contains(clientSourceId2), is(true));
    assertThat(clients.contains(clientSourceId3), is(false));
  }

  /**
   * Test just making sure we got all the typing right. If it compiles, it means we do
   *
   * @throws EntityUserException
   */
  @Test
  public void testTyping() throws EntityUserException {
    OOOMessageHandler<DummyEntityMessage, DummyEntityResponse> messageHandler = new OOOMessageHandlerImpl<>(msg -> true, 1, m -> 0);

    DummyClientSourceId clientSourceId = new DummyClientSourceId(1);
    InvokeContext context = new DummyContext(clientSourceId, 25, 25);
    DummyEntityMessage message = new DummyEntityMessage();
    DummyEntityResponse response = messageHandler.invoke(context, message, this::invokeActiveInternal);

    Map<Long, DummyEntityResponse> messages = new HashMap<>();
    messageHandler.loadTrackedResponsesForSegment(0, clientSourceId, messages);

    Map<Long, DummyEntityResponse> responses = messageHandler.getTrackedResponsesForSegment(0, clientSourceId);
  }

  private DummyEntityResponse invokeActiveInternal(InvokeContext context, DummyEntityMessage message) {
    return new DummyEntityResponse();
  }

  private static class DummyEntityMessage implements EntityMessage {}
  private static class DummyEntityResponse implements EntityResponse {}
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

    @Override
    public int getConcurrencyKey() {
      return 0;
    }
  }
}
