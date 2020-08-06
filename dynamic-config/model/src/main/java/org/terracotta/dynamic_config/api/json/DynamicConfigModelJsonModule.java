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
package org.terracotta.dynamic_config.api.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.ReferenceTypeSerializer;
import com.fasterxml.jackson.databind.type.ReferenceType;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.type.TypeModifier;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.terracotta.common.struct.json.StructJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.OptionalConfig;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.inet.json.InetJsonModule;
import org.terracotta.json.TerracottaJsonModule;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigModelJsonModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public DynamicConfigModelJsonModule() {
    super(DynamicConfigModelJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    setMixInAnnotation(NodeContext.class, NodeContextMixin.class);
    setMixInAnnotation(Cluster.class, ClusterMixin.class);
    setMixInAnnotation(Stripe.class, StripeMixin.class);
    setMixInAnnotation(Node.class, NodeMixin.class);
    setMixInAnnotation(FailoverPriority.class, FailoverPriorityMixin.class);
    setMixInAnnotation(License.class, LicenseMixin.class);
    setMixInAnnotation(OptionalConfig.class, OptionalConfigMixin.class);
    setMixInAnnotation(LockContext.class, LockContextMixin.class);
  }

  @Override
  public Iterable<? extends Module> getDependencies() {
    return asList(
        new TerracottaJsonModule(),
        new StructJsonModule(),
        new InetJsonModule(),
        new Jdk8Module(),
        new JavaTimeModule());
  }

  @Override
  public void setupModule(SetupContext context) {
    super.setupModule(context);

    // OptionalConfig proper handling
    context.addTypeModifier(new TypeModifier() {
      @Override
      public JavaType modifyType(JavaType type, Type jdkType, TypeBindings context, TypeFactory typeFactory) {
        return type.isReferenceType() || type.isContainerType() || type.getRawClass() != OptionalConfig.class ? type : ReferenceType.upgradeFrom(type, type.containedTypeOrUnknown(0));
      }
    });
    context.addSerializers(new Serializers.Base() {
      @Override
      public JsonSerializer<?> findReferenceSerializer(SerializationConfig config, ReferenceType refType, BeanDescription beanDesc, TypeSerializer contentTypeSerializer, JsonSerializer<Object> contentValueSerializer) {
        if (OptionalConfig.class.isAssignableFrom(refType.getRawClass())) {
          boolean staticTyping = (contentTypeSerializer == null) && config.isEnabled(MapperFeature.USE_STATIC_TYPING);
          return new OptionalConfigSerializer(refType, staticTyping, contentTypeSerializer, contentValueSerializer);
        }
        return null;
      }
    });
  }

  public static class NodeContextMixin extends NodeContext {
    @JsonCreator
    public NodeContextMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                            @JsonProperty(value = "stripeId", required = true) int stripeId,
                            @JsonProperty(value = "nodeName", required = true) String nodeName) {
      super(cluster, stripeId, nodeName);
    }

    @JsonIgnore
    @Override
    public Node getNode() {
      return super.getNode();
    }

    @JsonIgnore
    @Override
    public Stripe getStripe() {
      return super.getStripe();
    }
  }

  public static class ClusterMixin extends Cluster {
    @JsonCreator
    protected ClusterMixin(@JsonProperty(value = "stripes", required = true) List<Stripe> stripes) {
      super(stripes);
    }

    @JsonIgnore
    @Override
    public Scope getScope() {
      return super.getScope();
    }

    @JsonIgnore
    @Override
    public Optional<Node> getSingleNode() throws IllegalStateException {
      return super.getSingleNode();
    }

    @JsonIgnore
    @Override
    public Optional<Stripe> getSingleStripe() {
      return super.getSingleStripe();
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
      return super.isEmpty();
    }

    @JsonIgnore
    @Override
    public Collection<InetSocketAddress> getNodeAddresses() {
      return super.getNodeAddresses();
    }

    @JsonIgnore
    @Override
    public int getNodeCount() {
      return super.getNodeCount();
    }

    @JsonIgnore
    @Override
    public int getStripeCount() {
      return super.getStripeCount();
    }

    @JsonIgnore
    @Override
    public Collection<Node> getNodes() {
      return super.getNodes();
    }

    @JsonIgnore
    @Override
    public Collection<String> getDataDirNames() {
      return super.getDataDirNames();
    }
  }

  public static class StripeMixin extends Stripe {
    @JsonIgnore
    @Override
    public Collection<InetSocketAddress> getNodeAddresses() {
      return super.getNodeAddresses();
    }

    @JsonIgnore
    @Override
    public Optional<Node> getSingleNode() throws IllegalStateException {
      return super.getSingleNode();
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
      return super.isEmpty();
    }

    @JsonIgnore
    @Override
    public Scope getScope() {
      return super.getScope();
    }

    @JsonIgnore
    @Override
    public int getNodeCount() {
      return super.getNodeCount();
    }
  }

  public static class NodeMixin extends Node {
    @JsonIgnore
    @Override
    public Scope getScope() {
      return super.getScope();
    }

    @JsonIgnore
    @Override
    public InetSocketAddress getAddress() {
      return super.getAddress();
    }

    @JsonIgnore
    @Override
    public InetSocketAddress getInternalAddress() {
      return super.getInternalAddress();
    }

    @JsonIgnore
    @Override
    public Optional<InetSocketAddress> getPublicAddress() {
      return super.getPublicAddress();
    }
  }

  public static class FailoverPriorityMixin extends FailoverPriority {
    public FailoverPriorityMixin(Type type, Integer voters) {
      super(type, voters);
    }

    @JsonCreator
    public static FailoverPriority valueOf(String str) {
      return FailoverPriority.valueOf(str);
    }

    @JsonValue
    @Override
    public String toString() {
      return super.toString();
    }
  }

  public static class OptionalConfigMixin<T> {
    @JsonValue
    private T value;
  }

  public static class LicenseMixin extends License {
    @JsonCreator
    public LicenseMixin(@JsonProperty(value = "capabilities", required = true) Map<String, Long> capabilityLimitMap,
                        @JsonProperty(value = "flags", required = true) Map<String, Boolean> flagsMap,
                        @JsonProperty(value = "expiryDate", required = true) LocalDate expiryDate) {
      super(capabilityLimitMap, flagsMap, expiryDate);
    }
  }

  private static class OptionalConfigSerializer extends ReferenceTypeSerializer<OptionalConfig<?>> {
    private static final long serialVersionUID = 1L;

    protected OptionalConfigSerializer(ReferenceType fullType, boolean staticTyping, TypeSerializer vts, JsonSerializer<Object> ser) {
      super(fullType, staticTyping, vts, ser);
    }

    protected OptionalConfigSerializer(OptionalConfigSerializer base, BeanProperty property, TypeSerializer vts, JsonSerializer<?> valueSer, NameTransformer unwrapper, Object suppressableValue, boolean suppressNulls) {
      super(base, property, vts, valueSer, unwrapper,
          suppressableValue, suppressNulls);
    }

    @Override
    protected ReferenceTypeSerializer<OptionalConfig<?>> withResolved(BeanProperty prop, TypeSerializer vts, JsonSerializer<?> valueSer, NameTransformer unwrapper) {
      return new OptionalConfigSerializer(this, prop, vts, valueSer, unwrapper, _suppressableValue, _suppressNulls);
    }

    @Override
    public ReferenceTypeSerializer<OptionalConfig<?>> withContentInclusion(Object suppressableValue, boolean suppressNulls) {
      return new OptionalConfigSerializer(this, _property, _valueTypeSerializer, _valueSerializer, _unwrapper, suppressableValue, suppressNulls);
    }

    @Override
    protected boolean _isValuePresent(OptionalConfig<?> value) {
      return value.isConfigured();
    }

    @Override
    protected Object _getReferenced(OptionalConfig<?> value) {
      return value.get();
    }

    @Override
    protected Object _getReferencedIfPresent(OptionalConfig<?> value) {
      return value.isConfigured() ? value.get() : null;
    }
  }

  public static abstract class LockContextMixin extends LockContext {
    @JsonCreator
    public LockContextMixin(@JsonProperty(value = "token", required = true) String token,
                            @JsonProperty(value = "ownerName", required = true) String ownerName,
                            @JsonProperty(value = "ownerTags", required = true) String ownerTags) {
      super(token, ownerName, ownerTags);
    }
  }
}
