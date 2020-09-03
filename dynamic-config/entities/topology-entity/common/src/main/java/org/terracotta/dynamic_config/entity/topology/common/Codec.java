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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.runnel.Struct;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.encoding.StructEncoder;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.IntStream.rangeClosed;
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
import static org.terracotta.runnel.EnumMappingBuilder.newEnumMappingBuilder;
import static org.terracotta.runnel.StructBuilder.newStructBuilder;

/**
 * @author Mathieu Carbou
 */
public class Codec implements MessageCodec<Message, Response> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Codec.class);

  private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH);

  private final Struct struct = newStructBuilder()
      .enm("type", 10, newEnumMappingBuilder(Type.class)
          .mapping(REQ_LICENSE, 1)
          .mapping(REQ_HAS_INCOMPLETE_CHANGE, 2)
          .mapping(REQ_MUST_BE_RESTARTED, 3)
          .mapping(REQ_RUNTIME_CLUSTER, 4)
          .mapping(REQ_UPCOMING_CLUSTER, 5)
          .mapping(EVENT_NODE_ADDITION, 6)
          .mapping(EVENT_NODE_REMOVAL, 7)
          .mapping(EVENT_SETTING_CHANGED, 8)
          .mapping(EVENT_STRIPE_ADDITION, 9)
          .mapping(EVENT_STRIPE_REMOVAL, 10)
          .build())
      .struct(REQ_LICENSE.name(), 20, newStructBuilder()
          .string("date", 10)
          .structs("limits", 20, newStructBuilder()
              .string("name", 10)
              .int64("value", 20)
              .build())
          .structs("flags", 30, newStructBuilder()
              .string("name", 10)
              .bool("value", 20)
              .build())
          .build())
      .bool(REQ_HAS_INCOMPLETE_CHANGE.name(), 30)
      .bool(REQ_MUST_BE_RESTARTED.name(), 40)
      .string(REQ_RUNTIME_CLUSTER.name(), 50)
      .string(REQ_UPCOMING_CLUSTER.name(), 60)
      .struct(EVENT_NODE_ADDITION.name(), 70, newStructBuilder()
          .string("stripeUID", 10) // V1 (deprecated)
          .string("node", 20)  // V1 (deprecated)
          .string("nodeUID", 30) // since V2
          .string("cluster", 40) // since V2
          .build())
      .struct(EVENT_NODE_REMOVAL.name(), 80, newStructBuilder()
          .string("stripeUID", 10)
          .string("node", 20)
          .string("cluster", 30) // since V2
          .build())
      .struct(EVENT_SETTING_CHANGED.name(), 90, newStructBuilder()
          .string("configuration", 10)
          .string("cluster", 20)
          .build())
      .string(EVENT_STRIPE_ADDITION.name(), 100) // V1 (deprecated)
      .string(EVENT_STRIPE_REMOVAL.name(), 110) // V1 (deprecated)
      .struct("EVENT_STRIPE_ADDITION_V2", 120, newStructBuilder()  // since V2
          .string("stripeUID", 10)
          .string("cluster", 20)
          .build())
      .struct("EVENT_STRIPE_REMOVAL_V2", 130, newStructBuilder()  // since V2
          .string("stripe", 10)
          .string("cluster", 20)
          .build())
      .build();

  @Override
  public byte[] encodeMessage(Message message) throws MessageCodecException {
    LOGGER.trace("encodeMessage({})", message);
    try {
      return struct.encoder()
          .enm("type", message.getType())
          .encode()
          .array();
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public Message decodeMessage(byte[] bytes) throws MessageCodecException {
    try {
      final Message message = new Message(struct.decoder(ByteBuffer.wrap(bytes)).<Type>enm("type").get());
      LOGGER.trace("decodeMessage(): {}", message);
      return message;
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] encodeResponse(Response response) throws MessageCodecException {
    LOGGER.trace("encodeResponse({})", response);
    try {
      Type type = response.getType();
      StructEncoder<Void> encoder = struct.encoder();
      encoder.enm("type", type);
      switch (type) {
        case REQ_LICENSE: {
          License license = response.getPayload();
          if (license != null) {
            encoder.struct(type.name())
                .string("date", license.getExpiryDate().format(DT_FORMATTER))
                .structs("limits", license.getCapabilityLimitMap().entrySet(), (entryEncoder, entry) -> entryEncoder
                    .string("name", entry.getKey())
                    .int64("value", entry.getValue()))
                .structs("flags", license.getFlagsMap().entrySet(), (entryEncoder, entry) -> entryEncoder
                    .string("name", entry.getKey())
                    .bool("value", entry.getValue()));
          }
          break;
        }
        case REQ_HAS_INCOMPLETE_CHANGE:
        case REQ_MUST_BE_RESTARTED: {
          encoder.bool(type.name(), response.getPayload());
          break;
        }
        case REQ_RUNTIME_CLUSTER:
        case REQ_UPCOMING_CLUSTER: {
          encoder.string(type.name(), encodeCluster(response.getPayload()));
          break;
        }
        case EVENT_NODE_ADDITION: {
          List<Object> oo = response.getPayload();
          Cluster cluster = (Cluster) oo.get(0);
          UID addedNodeUID = (UID) oo.get(1);
          encoder.struct(type.name())
              .string("stripeUID", cluster.getStripeByNode(addedNodeUID).get().getUID().toString()) // V1 (deprecated)
              .string("node", encodeNode(cluster.getNode(addedNodeUID).get())) // V1 (deprecated)
              .string("nodeUID", addedNodeUID.toString()) // since V2
              .string("cluster", encodeCluster(cluster)); // since V2
          break;
        }
        case EVENT_NODE_REMOVAL: {
          List<Object> oo = response.getPayload();
          Cluster cluster = (Cluster) oo.get(0);
          UID stripeUID = (UID) oo.get(1);
          Node node = (Node) oo.get(2);
          encoder.struct(type.name())
              .string("stripeUID", stripeUID.toString())
              .string("node", encodeNode(node))
              .string("cluster", encodeCluster(cluster)); // since V2
          break;
        }
        case EVENT_SETTING_CHANGED: {
          List<Object> oo = response.getPayload();
          encoder.struct(type.name())
              .string("configuration", encodeConfiguration((Configuration) oo.get(1)))
              .string("cluster", encodeCluster((Cluster) oo.get(0)));
          break;
        }
        case EVENT_STRIPE_ADDITION: {
          List<Object> oo = response.getPayload();
          Cluster cluster = (Cluster) oo.get(0);
          UID stripeUID = (UID) oo.get(1);
          // V1 (deprecated)
          encoder.string(type.name(), encodeStripe(cluster.getStripe(stripeUID).get()));
          // since V2
          encoder.struct("EVENT_STRIPE_ADDITION_V2")
              .string("stripeUID", stripeUID.toString())
              .string("cluster", encodeCluster(cluster));
          break;
        }
        case EVENT_STRIPE_REMOVAL: {
          List<Object> oo = response.getPayload();
          Cluster cluster = (Cluster) oo.get(0);
          Stripe stripe = (Stripe) oo.get(1);
          // V1 (deprecated)
          encoder.string(type.name(), encodeStripe(stripe));
          // since V2
          encoder.struct("EVENT_STRIPE_REMOVAL_V2")
              .string("stripe", encodeStripe(stripe))
              .string("cluster", encodeCluster(cluster));
          break;
        }
        default:
          throw new UnsupportedOperationException(type.name());
      }
      return encoder.encode().array();
    } catch (RuntimeException e) {
      LOGGER.error("encodeResponse({}): {}", response, e.getMessage(), e);
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public Response decodeResponse(byte[] bytes) throws MessageCodecException {
    try {
      StructDecoder<Void> decoder = struct.decoder(ByteBuffer.wrap(bytes));
      Type type = decoder.<Type>enm("type").get();
      LOGGER.trace("decodeResponse({})", type);
      switch (type) {
        case REQ_LICENSE: {
          StructDecoder<StructDecoder<Void>> payload = decoder.struct(type.name());
          if (payload == null) {
            return new Response(type, null);
          } else {
            LocalDate expiryDate = LocalDate.parse(payload.string("date"), DT_FORMATTER);
            Map<String, Long> limits = new HashMap<>();
            payload.structs("limits").forEachRemaining(entry -> limits.put(entry.string("name"), entry.int64("value")));
            Map<String, Boolean> flags = new HashMap<>();
            payload.structs("flags").forEachRemaining(entry -> flags.put(entry.string("name"), entry.bool("value")));
            return new Response(type, new License(limits, flags, expiryDate));
          }
        }
        case REQ_HAS_INCOMPLETE_CHANGE:
        case REQ_MUST_BE_RESTARTED:
          return new Response(type, decoder.bool(type.name()));
        case REQ_RUNTIME_CLUSTER:
        case REQ_UPCOMING_CLUSTER:
          return new Response(type, decodeCluster(decoder.string(type.name())));
        case EVENT_NODE_ADDITION: {
          // since V2
          StructDecoder<?> event = decoder.struct(type.name());
          UID nodeUID = UID.valueOf(event.string("nodeUID"));
          Cluster cluster = decodeCluster(event.string("cluster"));
          return new Response(type, asList(cluster, nodeUID));
        }
        case EVENT_NODE_REMOVAL: {
          // since V2
          StructDecoder<?> event = decoder.struct(type.name());
          UID stripeUID = UID.valueOf(event.string("stripeUID"));
          Node removedNode = decodeNode(event.string("node"));
          Cluster cluster = decodeCluster(event.string("cluster"));
          return new Response(type, asList(cluster, stripeUID, removedNode));
        }
        case EVENT_SETTING_CHANGED: {
          StructDecoder<?> event = decoder.struct(type.name());
          final String configuration = event.string("configuration");
          final String cluster = event.string("cluster");
          return new Response(type, asList(decodeCluster(cluster), decodeConfiguration(configuration)));
        }
        case EVENT_STRIPE_ADDITION: {
          // since V2
          StructDecoder<?> event = decoder.struct("EVENT_STRIPE_ADDITION_V2");
          UID stripeUID = UID.valueOf(event.string("stripeUID"));
          Cluster cluster = decodeCluster(event.string("cluster"));
          return new Response(type, asList(cluster, stripeUID));
        }
        case EVENT_STRIPE_REMOVAL:
          // since V2
          StructDecoder<?> event = decoder.struct("EVENT_STRIPE_REMOVAL_V2");
          Stripe stripe = decodeStripe(event.string("stripe"));
          Cluster cluster = decodeCluster(event.string("cluster"));
          return new Response(type, asList(cluster, stripe));
        default:
          throw new UnsupportedOperationException(type.name());
      }
    } catch (RuntimeException e) {
      LOGGER.error("decodeResponse(): {}", e.getMessage(), e);
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  // the encode / decode methods below re-uses the inner mapping mechanism we have

  private String encodeCluster(Cluster cluster) {
    requireNonNull(cluster);
    return Props.toString(cluster.toProperties(false, false, true));
  }

  private Cluster decodeCluster(String payload) {
    requireNonNull(payload);
    return new ClusterFactory(ClusterValidator.noop()).create(Props.load(payload), configuration -> {
    });
  }

  private String encodeNode(Node node) {
    requireNonNull(node);
    return Props.toString(node.toProperties(false, false, true));
  }

  private Node decodeNode(String payload) {
    requireNonNull(payload);
    Properties properties = Props.load(payload);
    Node node = new Node();
    Cluster cluster = new Cluster(new Stripe().addNode(node));
    properties.stringPropertyNames().forEach(key -> Configuration.valueOf("stripe.1.node.1." + key, properties.getProperty(key)).apply(cluster));
    return node;
  }

  private String encodeStripe(Stripe stripe) {
    requireNonNull(stripe);
    final Properties props = stripe.toProperties(false, false, true);
    props.setProperty("nodes", String.valueOf(stripe.getNodeCount()));
    return Props.toString(props);
  }

  private Stripe decodeStripe(String payload) {
    requireNonNull(payload);
    final Properties properties = Props.load(payload);
    final int count = Integer.parseInt(properties.remove("nodes").toString());
    final Stripe stripe = new Stripe();
    final Cluster cluster = new Cluster(stripe);
    rangeClosed(1, count).forEach(idx -> stripe.addNode(new Node()));
    properties.stringPropertyNames().forEach(key -> Configuration.valueOf("stripe.1." + key, properties.getProperty(key)).apply(cluster));
    return stripe;
  }

  private String encodeConfiguration(Configuration configuration) {
    requireNonNull(configuration);
    return configuration.toString();
  }

  private Configuration decodeConfiguration(String payload) {
    requireNonNull(payload);
    return Configuration.valueOf(payload);
  }
}
