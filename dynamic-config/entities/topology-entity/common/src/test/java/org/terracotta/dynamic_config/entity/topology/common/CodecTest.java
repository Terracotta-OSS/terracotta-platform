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
package org.terracotta.dynamic_config.entity.topology.common;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.entity.MessageCodecException;

import java.time.LocalDate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.entity.topology.common.Type.EVENT_NODE_ADDITION;
import static org.terracotta.dynamic_config.entity.topology.common.Type.EVENT_NODE_REMOVAL;
import static org.terracotta.dynamic_config.entity.topology.common.Type.EVENT_SETTING_CHANGED;
import static org.terracotta.dynamic_config.entity.topology.common.Type.EVENT_STRIPE_ADDITION;
import static org.terracotta.dynamic_config.entity.topology.common.Type.EVENT_STRIPE_REMOVAL;
import static org.terracotta.dynamic_config.entity.topology.common.Type.REQ_HAS_INCOMPLETE_CHANGE;
import static org.terracotta.dynamic_config.entity.topology.common.Type.REQ_LICENSE;
import static org.terracotta.dynamic_config.entity.topology.common.Type.REQ_MUST_BE_RESTARTED;
import static org.terracotta.dynamic_config.entity.topology.common.Type.REQ_RUNTIME_CLUSTER;
import static org.terracotta.dynamic_config.entity.topology.common.Type.REQ_UPCOMING_CLUSTER;

/**
 * @author Mathieu Carbou
 */
public class CodecTest {
  @Test
  public void test_encode_decode() throws MessageCodecException {
    Node node = Testing.newTestNode("foo", "localhost", 9410);
    Node node2 = Testing.newTestNode("foo2", "localhost", 9411);
    Stripe stripe = new Stripe(node, node2);
    Cluster cluster = Testing.newTestCluster("bar", stripe);

    test(REQ_LICENSE, null);
    test(REQ_LICENSE, new License(emptyMap(), emptyMap(), LocalDate.of(2020, 1, 1)));
    test(REQ_LICENSE, new License(singletonMap("offheap", 1024L), emptyMap(), LocalDate.of(2020, 1, 1)));
    test(REQ_LICENSE, new License(emptyMap(), singletonMap("SubscriptionBased", false), LocalDate.now()));
    test(REQ_LICENSE, new License(singletonMap("offheap", 1024L), singletonMap("StorageBased", false), LocalDate.now()));

    test(REQ_MUST_BE_RESTARTED, true);
    test(REQ_HAS_INCOMPLETE_CHANGE, true);

    test(REQ_RUNTIME_CLUSTER, cluster);
    test(REQ_UPCOMING_CLUSTER, cluster);

    test(EVENT_NODE_ADDITION, asList(1, node));
    test(EVENT_NODE_REMOVAL, asList(1, node));

    test(EVENT_SETTING_CHANGED, asList(Configuration.valueOf("cluster-name=foo"), cluster));

    test(EVENT_STRIPE_ADDITION, stripe);
    test(EVENT_STRIPE_REMOVAL, stripe);
  }

  private static void test(Type type, Object payload) throws MessageCodecException {
    Codec codec = new Codec();

    Message message = new Message(type);
    byte[] bytes = codec.encodeMessage(message);
    Message decodedMessage = codec.decodeMessage(bytes);
    assertThat(decodedMessage, is(equalTo(message)));

    Response response = new Response(type, payload);
    bytes = codec.encodeResponse(response);
    Response decodedResponse = codec.decodeResponse(bytes);
    assertThat(decodedResponse, is(equalTo(response)));
  }
}