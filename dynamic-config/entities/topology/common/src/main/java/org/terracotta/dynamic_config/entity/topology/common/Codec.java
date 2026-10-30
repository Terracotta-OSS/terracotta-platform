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
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.runnel.Struct;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.decoding.fields.BoolField;
import org.terracotta.runnel.decoding.fields.EnumField;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;
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

  // nested schemas, hoisted so field handles can be resolved from the exact instances
  // registered with the root schema
  private static final Struct LIMITS_STRUCT = newStructBuilder()
      .string("name", 10)
      .int64("value", 20)
      .build();
  private static final Struct FLAGS_STRUCT = newStructBuilder()
      .string("name", 10)
      .bool("value", 20)
      .build();
  private static final Struct REQ_LICENSE_STRUCT = newStructBuilder()
      .string("date", 10)
      .structs("limits", 20, LIMITS_STRUCT)
      .structs("flags", 30, FLAGS_STRUCT)
      .build();
  private static final Struct NODE_ADDITION_STRUCT = newStructBuilder()
      .string("stripeUID", 10) // V1 (deprecated)
      .string("node", 20)  // V1 (deprecated)
      .string("nodeUID", 30) // since V2
      .string("cluster", 40) // since V2
      .build();
  private static final Struct NODE_REMOVAL_STRUCT = newStructBuilder()
      .string("stripeUID", 10)
      .string("node", 20)
      .string("cluster", 30) // since V2
      .build();
  private static final Struct SETTING_CHANGED_STRUCT = newStructBuilder()
      .string("configuration", 10)
      .string("cluster", 20)
      .build();
  private static final Struct STRIPE_ADDITION_V2_STRUCT = newStructBuilder()
      .string("stripeUID", 10)
      .string("cluster", 20)
      .build();
  private static final Struct STRIPE_REMOVAL_V2_STRUCT = newStructBuilder()
      .string("stripe", 10)
      .string("cluster", 20)
      .build();

  private static final Struct STRUCT = newStructBuilder()
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
      .struct(REQ_LICENSE.name(), 20, REQ_LICENSE_STRUCT)
      .bool(REQ_HAS_INCOMPLETE_CHANGE.name(), 30)
      .bool(REQ_MUST_BE_RESTARTED.name(), 40)
      .string(REQ_RUNTIME_CLUSTER.name(), 50)
      .string(REQ_UPCOMING_CLUSTER.name(), 60)
      .struct(EVENT_NODE_ADDITION.name(), 70, NODE_ADDITION_STRUCT)
      .struct(EVENT_NODE_REMOVAL.name(), 80, NODE_REMOVAL_STRUCT)
      .struct(EVENT_SETTING_CHANGED.name(), 90, SETTING_CHANGED_STRUCT)
      .string(EVENT_STRIPE_ADDITION.name(), 100) // V1 (deprecated)
      .string(EVENT_STRIPE_REMOVAL.name(), 110) // V1 (deprecated)
      .struct("EVENT_STRIPE_ADDITION_V2", 120, STRIPE_ADDITION_V2_STRUCT)  // since V2
      .struct("EVENT_STRIPE_REMOVAL_V2", 130, STRIPE_REMOVAL_V2_STRUCT)  // since V2
      .build();

  // fields resolved once: no per-message name lookup, and a mistyped name fails at class init
  @SuppressWarnings("unchecked")
  private static final EnumField<Type> TYPE_FIELD = STRUCT.field("type", EnumField.class);
  private static final StructField REQ_LICENSE_FIELD = STRUCT.field(REQ_LICENSE.name(), StructField.class);
  private static final BoolField HAS_INCOMPLETE_CHANGE_FIELD = STRUCT.field(REQ_HAS_INCOMPLETE_CHANGE.name(), BoolField.class);
  private static final BoolField MUST_BE_RESTARTED_FIELD = STRUCT.field(REQ_MUST_BE_RESTARTED.name(), BoolField.class);
  private static final StringField RUNTIME_CLUSTER_FIELD = STRUCT.field(REQ_RUNTIME_CLUSTER.name(), StringField.class);
  private static final StringField UPCOMING_CLUSTER_FIELD = STRUCT.field(REQ_UPCOMING_CLUSTER.name(), StringField.class);
  private static final StructField NODE_ADDITION_FIELD = STRUCT.field(EVENT_NODE_ADDITION.name(), StructField.class);
  private static final StructField NODE_REMOVAL_FIELD = STRUCT.field(EVENT_NODE_REMOVAL.name(), StructField.class);
  private static final StructField SETTING_CHANGED_FIELD = STRUCT.field(EVENT_SETTING_CHANGED.name(), StructField.class);
  private static final StringField STRIPE_ADDITION_V1_FIELD = STRUCT.field(EVENT_STRIPE_ADDITION.name(), StringField.class);
  private static final StringField STRIPE_REMOVAL_V1_FIELD = STRUCT.field(EVENT_STRIPE_REMOVAL.name(), StringField.class);
  private static final StructField STRIPE_ADDITION_V2_FIELD = STRUCT.field("EVENT_STRIPE_ADDITION_V2", StructField.class);
  private static final StructField STRIPE_REMOVAL_V2_FIELD = STRUCT.field("EVENT_STRIPE_REMOVAL_V2", StructField.class);
  private static final StringField LICENSE_DATE_FIELD = REQ_LICENSE_STRUCT.field("date", StringField.class);
  private static final ArrayField LICENSE_LIMITS_FIELD = REQ_LICENSE_STRUCT.field("limits", ArrayField.class);
  private static final ArrayField LICENSE_FLAGS_FIELD = REQ_LICENSE_STRUCT.field("flags", ArrayField.class);
  private static final StringField LIMITS_NAME_FIELD = LIMITS_STRUCT.field("name", StringField.class);
  private static final Int64Field LIMITS_VALUE_FIELD = LIMITS_STRUCT.field("value", Int64Field.class);
  private static final StringField FLAGS_NAME_FIELD = FLAGS_STRUCT.field("name", StringField.class);
  private static final BoolField FLAGS_VALUE_FIELD = FLAGS_STRUCT.field("value", BoolField.class);
  private static final StringField NODE_ADDITION_STRIPE_UID_FIELD = NODE_ADDITION_STRUCT.field("stripeUID", StringField.class);
  private static final StringField NODE_ADDITION_NODE_FIELD = NODE_ADDITION_STRUCT.field("node", StringField.class);
  private static final StringField NODE_ADDITION_NODE_UID_FIELD = NODE_ADDITION_STRUCT.field("nodeUID", StringField.class);
  private static final StringField NODE_ADDITION_CLUSTER_FIELD = NODE_ADDITION_STRUCT.field("cluster", StringField.class);
  private static final StringField NODE_REMOVAL_STRIPE_UID_FIELD = NODE_REMOVAL_STRUCT.field("stripeUID", StringField.class);
  private static final StringField NODE_REMOVAL_NODE_FIELD = NODE_REMOVAL_STRUCT.field("node", StringField.class);
  private static final StringField NODE_REMOVAL_CLUSTER_FIELD = NODE_REMOVAL_STRUCT.field("cluster", StringField.class);
  private static final StringField SETTING_CHANGED_CONFIGURATION_FIELD = SETTING_CHANGED_STRUCT.field("configuration", StringField.class);
  private static final StringField SETTING_CHANGED_CLUSTER_FIELD = SETTING_CHANGED_STRUCT.field("cluster", StringField.class);
  private static final StringField STRIPE_ADDITION_V2_STRIPE_UID_FIELD = STRIPE_ADDITION_V2_STRUCT.field("stripeUID", StringField.class);
  private static final StringField STRIPE_ADDITION_V2_CLUSTER_FIELD = STRIPE_ADDITION_V2_STRUCT.field("cluster", StringField.class);
  private static final StringField STRIPE_REMOVAL_V2_STRIPE_FIELD = STRIPE_REMOVAL_V2_STRUCT.field("stripe", StringField.class);
  private static final StringField STRIPE_REMOVAL_V2_CLUSTER_FIELD = STRIPE_REMOVAL_V2_STRUCT.field("cluster", StringField.class);

  @Override
  public byte[] encodeMessage(Message message) throws MessageCodecException {
    LOGGER.trace("encodeMessage({})", message);
    try {
      return STRUCT.encoder()
          .enm(TYPE_FIELD, message.getType())
          .encode()
          .array();
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public Message decodeMessage(byte[] bytes) throws MessageCodecException {
    try {
      // keep the Enm dispatch form: an unknown type from a future version must fail loudly
      final Message message = new Message(STRUCT.decoder(ByteBuffer.wrap(bytes)).enm(TYPE_FIELD).get());
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
      StructEncoder<Void> encoder = STRUCT.encoder();
      encoder.enm(TYPE_FIELD, type);
      switch (type) {
        case REQ_LICENSE: {
          License license = response.getPayload();
          if (license != null) {
            encoder.struct(REQ_LICENSE_FIELD)
                .string(LICENSE_DATE_FIELD, license.getExpiryDate().format(DT_FORMATTER))
                .structs("limits", license.getCapabilities().entrySet(), (entryEncoder, entry) -> entryEncoder
                    .string(LIMITS_NAME_FIELD, entry.getKey())
                    .int64(LIMITS_VALUE_FIELD, entry.getValue()))
                .structs("flags", license.getFlags().entrySet(), (entryEncoder, entry) -> entryEncoder
                    .string(FLAGS_NAME_FIELD, entry.getKey())
                    .bool(FLAGS_VALUE_FIELD, entry.getValue()));
          }
          break;
        }
        case REQ_HAS_INCOMPLETE_CHANGE: {
          encoder.bool(HAS_INCOMPLETE_CHANGE_FIELD, response.getPayload());
          break;
        }
        case REQ_MUST_BE_RESTARTED: {
          encoder.bool(MUST_BE_RESTARTED_FIELD, response.getPayload());
          break;
        }
        case REQ_RUNTIME_CLUSTER: {
          encoder.string(RUNTIME_CLUSTER_FIELD, encodeCluster(response.getPayload()));
          break;
        }
        case REQ_UPCOMING_CLUSTER: {
          encoder.string(UPCOMING_CLUSTER_FIELD, encodeCluster(response.getPayload()));
          break;
        }
        case EVENT_NODE_ADDITION: {
          List<Object> oo = response.getPayload();
          Cluster cluster = (Cluster) oo.get(0);
          UID addedNodeUID = (UID) oo.get(1);
          encoder.struct(NODE_ADDITION_FIELD)
              .string(NODE_ADDITION_STRIPE_UID_FIELD, cluster.getStripeByNode(addedNodeUID).get().getUID().toString()) // V1 (deprecated)
              .string(NODE_ADDITION_NODE_FIELD, encodeNode(cluster.getNode(addedNodeUID).get())) // V1 (deprecated)
              .string(NODE_ADDITION_NODE_UID_FIELD, addedNodeUID.toString()) // since V2
              .string(NODE_ADDITION_CLUSTER_FIELD, encodeCluster(cluster)); // since V2
          break;
        }
        case EVENT_NODE_REMOVAL: {
          List<Object> oo = response.getPayload();
          Cluster cluster = (Cluster) oo.get(0);
          UID stripeUID = (UID) oo.get(1);
          Node node = (Node) oo.get(2);
          encoder.struct(NODE_REMOVAL_FIELD)
              .string(NODE_REMOVAL_STRIPE_UID_FIELD, stripeUID.toString())
              .string(NODE_REMOVAL_NODE_FIELD, encodeNode(node))
              .string(NODE_REMOVAL_CLUSTER_FIELD, encodeCluster(cluster)); // since V2
          break;
        }
        case EVENT_SETTING_CHANGED: {
          List<Object> oo = response.getPayload();
          encoder.struct(SETTING_CHANGED_FIELD)
              .string(SETTING_CHANGED_CONFIGURATION_FIELD, encodeConfiguration((Configuration) oo.get(1)))
              .string(SETTING_CHANGED_CLUSTER_FIELD, encodeCluster((Cluster) oo.get(0)));
          break;
        }
        case EVENT_STRIPE_ADDITION: {
          List<Object> oo = response.getPayload();
          Cluster cluster = (Cluster) oo.get(0);
          UID stripeUID = (UID) oo.get(1);
          // V1 (deprecated)
          encoder.string(STRIPE_ADDITION_V1_FIELD, encodeStripe(cluster.getStripe(stripeUID).get()));
          // since V2
          encoder.struct(STRIPE_ADDITION_V2_FIELD)
              .string(STRIPE_ADDITION_V2_STRIPE_UID_FIELD, stripeUID.toString())
              .string(STRIPE_ADDITION_V2_CLUSTER_FIELD, encodeCluster(cluster));
          break;
        }
        case EVENT_STRIPE_REMOVAL: {
          List<Object> oo = response.getPayload();
          Cluster cluster = (Cluster) oo.get(0);
          Stripe stripe = (Stripe) oo.get(1);
          // V1 (deprecated)
          encoder.string(STRIPE_REMOVAL_V1_FIELD, encodeStripe(stripe));
          // since V2
          encoder.struct(STRIPE_REMOVAL_V2_FIELD)
              .string(STRIPE_REMOVAL_V2_STRIPE_FIELD, encodeStripe(stripe))
              .string(STRIPE_REMOVAL_V2_CLUSTER_FIELD, encodeCluster(cluster));
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
      StructDecoder<?> decoder = STRUCT.decoder(ByteBuffer.wrap(bytes));
      // keep the Enm dispatch form: an unknown type from a future version must fail loudly
      Type type = decoder.enm(TYPE_FIELD).get();
      LOGGER.trace("decodeResponse({})", type);
      switch (type) {
        case REQ_LICENSE: {
          StructDecoder<?> payload = decoder.struct(REQ_LICENSE_FIELD);
          if (payload == null) {
            return new Response(type, null);
          } else {
            LocalDate expiryDate = LocalDate.parse(payload.string(LICENSE_DATE_FIELD), DT_FORMATTER);
            Map<String, Long> limits = new HashMap<>();
            payload.structs(LICENSE_LIMITS_FIELD).forEachRemaining(entry -> limits.put(entry.string(LIMITS_NAME_FIELD), entry.int64(LIMITS_VALUE_FIELD, 0L)));
            Map<String, Boolean> flags = new HashMap<>();
            payload.structs(LICENSE_FLAGS_FIELD).forEachRemaining(entry -> flags.put(entry.string(FLAGS_NAME_FIELD), entry.bool(FLAGS_VALUE_FIELD, false)));
            return new Response(type, new License(limits, flags, expiryDate));
          }
        }
        case REQ_HAS_INCOMPLETE_CHANGE:
          return new Response(type, decoder.bool(HAS_INCOMPLETE_CHANGE_FIELD, false));
        case REQ_MUST_BE_RESTARTED:
          return new Response(type, decoder.bool(MUST_BE_RESTARTED_FIELD, false));
        case REQ_RUNTIME_CLUSTER:
          return new Response(type, decodeCluster(decoder.string(RUNTIME_CLUSTER_FIELD)));
        case REQ_UPCOMING_CLUSTER:
          return new Response(type, decodeCluster(decoder.string(UPCOMING_CLUSTER_FIELD)));
        case EVENT_NODE_ADDITION: {
          // since V2
          StructDecoder<?> event = decoder.struct(NODE_ADDITION_FIELD);
          UID nodeUID = UID.valueOf(event.string(NODE_ADDITION_NODE_UID_FIELD));
          Cluster cluster = decodeCluster(event.string(NODE_ADDITION_CLUSTER_FIELD));
          return new Response(type, asList(cluster, nodeUID));
        }
        case EVENT_NODE_REMOVAL: {
          // since V2
          StructDecoder<?> event = decoder.struct(NODE_REMOVAL_FIELD);
          UID stripeUID = UID.valueOf(event.string(NODE_REMOVAL_STRIPE_UID_FIELD));
          Node removedNode = decodeNode(event.string(NODE_REMOVAL_NODE_FIELD));
          Cluster cluster = decodeCluster(event.string(NODE_REMOVAL_CLUSTER_FIELD));
          return new Response(type, asList(cluster, stripeUID, removedNode));
        }
        case EVENT_SETTING_CHANGED: {
          StructDecoder<?> event = decoder.struct(SETTING_CHANGED_FIELD);
          final String configuration = event.string(SETTING_CHANGED_CONFIGURATION_FIELD);
          final String cluster = event.string(SETTING_CHANGED_CLUSTER_FIELD);
          return new Response(type, asList(decodeCluster(cluster), decodeConfiguration(configuration)));
        }
        case EVENT_STRIPE_ADDITION: {
          // since V2
          StructDecoder<?> event = decoder.struct(STRIPE_ADDITION_V2_FIELD);
          UID stripeUID = UID.valueOf(event.string(STRIPE_ADDITION_V2_STRIPE_UID_FIELD));
          Cluster cluster = decodeCluster(event.string(STRIPE_ADDITION_V2_CLUSTER_FIELD));
          return new Response(type, asList(cluster, stripeUID));
        }
        case EVENT_STRIPE_REMOVAL:
          // since V2
          StructDecoder<?> event = decoder.struct(STRIPE_REMOVAL_V2_FIELD);
          Stripe stripe = decodeStripe(event.string(STRIPE_REMOVAL_V2_STRIPE_FIELD));
          Cluster cluster = decodeCluster(event.string(STRIPE_REMOVAL_V2_CLUSTER_FIELD));
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
    return new ClusterFactory().create(Props.load(payload), configuration -> {
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
