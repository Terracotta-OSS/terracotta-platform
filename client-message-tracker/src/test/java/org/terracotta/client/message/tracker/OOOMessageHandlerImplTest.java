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
package org.terracotta.client.message.tracker;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.InvokeContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OOOMessageHandlerImplTest {

  private OOOMessageHandler<EntityMessage, EntityResponse> messageHandler;

  @Before
  public void setUp() throws Exception {
    messageHandler = new OOOMessageHandlerImpl<>(msg -> true, () -> {});
  }

  private static Map<Long, EntityResponse> toFilteredMap(Stream<RecordedMessage<EntityMessage, EntityResponse>> recorded, ToIntFunction<EntityMessage> segmenter, int targetSegment, ClientSourceId src) {
    return recorded
            .filter(t->segmenter.applyAsInt(t.getRequest()) == targetSegment && t.getClientSourceId().toLong() == src.toLong())
            .collect(Collectors.toMap(RecordedMessage::getTransactionId, RecordedMessage::getResponse));
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
    messageHandler = new OOOMessageHandlerImpl<>(msg -> false, () -> {});

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
    messageHandler = new OOOMessageHandlerImpl<>(msg -> true, () -> {});

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

    messageHandler = new OOOMessageHandlerImpl<>(m -> true, () -> {});

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

    Map<Long, EntityResponse> trackedResponsesForClient1Segment0 = toFilteredMap(messageHandler.getRecordedMessages(), segmentationStrategy, 0, clientSourceId1);
    assertThat(trackedResponsesForClient1Segment0.size(), is(2));
    assertThat(trackedResponsesForClient1Segment0.get(txnId1), is(response1));
    assertThat(trackedResponsesForClient1Segment0.get(txnId3), is(response3));

    Map<Long, EntityResponse> trackedResponsesForClient1Segment1 = toFilteredMap(messageHandler.getRecordedMessages(), segmentationStrategy, 1, clientSourceId1);
    assertThat(trackedResponsesForClient1Segment1.size(), is(1));
    assertThat(trackedResponsesForClient1Segment1.get(txnId2), is(response2));

    Map<Long, EntityResponse> trackedResponsesForClient2Segment0 = toFilteredMap(messageHandler.getRecordedMessages(), segmentationStrategy, 0, clientSourceId2);
    assertThat(trackedResponsesForClient2Segment0.size(), is(0));

    Map<Long, EntityResponse> trackedResponsesForClient2Segment1 = toFilteredMap(messageHandler.getRecordedMessages(), segmentationStrategy, 1, clientSourceId2);
    assertThat(trackedResponsesForClient2Segment1.size(), is(1));
    assertThat(trackedResponsesForClient2Segment1.get(txnId4), is(response4));
  }

  private static RecordedMessage<EntityMessage, EntityResponse> mockRecorded(EntityMessage msg, EntityResponse resp, long tid, long sid, ClientSourceId cid) {
    return new SequencedRecordedMessage<EntityMessage, EntityResponse>() {
      @Override
      public long getSequenceId() {
        return sid;
      }

      @Override
      public ClientSourceId getClientSourceId() {
        return cid;
      }

      @Override
      public long getTransactionId() {
        return tid;
      }

      @Override
      public EntityMessage getRequest() {
        return msg;
      }

      @Override
      public EntityResponse getResponse() {
        return resp;
      }
    };
  }

  @Test
  public void testLoadTrackedResponsesForSegment() throws Exception {
    List<EntityMessage> seg1 = new ArrayList<>(2);
    ToIntFunction<EntityMessage> segments = m -> seg1.contains(m) ? 0 : 1;

    messageHandler = new OOOMessageHandlerImpl<>(m -> true, () -> {});

    EntityMessage message1 = mock(EntityMessage.class);
    seg1.add(message1);
    EntityMessage message2 = mock(EntityMessage.class);
    seg1.add(message2);
    EntityMessage message3 = mock(EntityMessage.class);
    EntityMessage message4 = mock(EntityMessage.class);
    EntityResponse response1 = mock(EntityResponse.class);
    EntityResponse response2 = mock(EntityResponse.class);
    DummyClientSourceId clientSourceId = new DummyClientSourceId(1);

    List<RecordedMessage<EntityMessage, EntityResponse>> tracked = new ArrayList<>();
    tracked.add(mockRecorded(message1, response1, 1L, 1L, clientSourceId));
    tracked.add(mockRecorded(message2, response2, 2L, 2L, clientSourceId));
    tracked.add(mockRecorded(message3, response1, 3L, 3L, clientSourceId));
    tracked.add(mockRecorded(message4, response2, 4L, 4L, clientSourceId));

    messageHandler.loadRecordedMessages(tracked.stream());

    Map<Long, EntityResponse> trackedResponses1 = toFilteredMap(messageHandler.getRecordedMessages(), segments, 0, clientSourceId);
    assertThat(trackedResponses1.size(), is(2));
    assertThat(trackedResponses1.keySet(), Matchers.contains(1L, 2L));
    assertThat(trackedResponses1.values(), Matchers.contains(response1, response2));

    Map<Long, EntityResponse> trackedResponses2 = toFilteredMap(messageHandler.getRecordedMessages(), segments, 1, clientSourceId);
    assertThat(trackedResponses2.size(), is(2));
    assertThat(trackedResponses2.keySet(), Matchers.contains(3L, 4L));
    assertThat(trackedResponses2.values(), Matchers.contains(response1, response2));
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

    messageHandler = new OOOMessageHandlerImpl<>(trackerPolicy,  () -> {});

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
