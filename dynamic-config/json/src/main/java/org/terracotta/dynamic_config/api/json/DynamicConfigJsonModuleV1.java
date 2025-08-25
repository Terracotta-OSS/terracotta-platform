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
package org.terracotta.dynamic_config.api.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.service.FormatUpgrade;
import org.terracotta.json.Json;
import org.terracotta.json.gson.DelegateTypeAdapterFactory;
import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;

import java.io.IOException;
import java.util.Map;

/**
 * This module can be added to the existing ones and will override some definitions to make the object mapper compatible with V1
 *
 * @author Mathieu Carbou
 * @deprecated old V1 format. Do not use anymore. Here for reference and backward compatibility.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
@Json.Module.Overrides(DynamicConfigJsonModule.class)
public class DynamicConfigJsonModuleV1 implements GsonModule {
  @Override
  public void configure(GsonConfig config) {
    config.registerMixin(Node.class, NodeMixinV1.class);

    config.registerTypeAdapterFactory(new NodeNomadChangeAdapterFactory<NodeAdditionNomadChange>(NodeAdditionNomadChange.class) {
      @Override
      protected NodeAdditionNomadChange create(Cluster cluster, UID stripeUID, Node node) {
        return new NodeAdditionNomadChange(cluster, stripeUID, node);
      }
    });

    config.registerTypeAdapterFactory(new NodeNomadChangeAdapterFactory<NodeRemovalNomadChange>(NodeRemovalNomadChange.class) {
      @Override
      protected NodeRemovalNomadChange create(Cluster cluster, UID stripeUID, Node node) {
        return new NodeRemovalNomadChange(cluster, stripeUID, node);
      }
    });

    config.registerTypeAdapterFactory(new DelegateTypeAdapterFactory<NodeContext>(NodeContext.class) {
      @Override
      public TypeAdapter<NodeContext> create(Gson gson) {
        final TypeAdapter<Cluster> clusterAdapter = gson.getAdapter(Cluster.class);
        return new NodeContextTypeAdapter(clusterAdapter);
      }
    });
  }

  static class NodeMixinV1 {
    @SerializedName("nodeName")
    String name;
    @SerializedName("nodeHostname")
    String hostname;
    @SerializedName("nodePublicHostname")
    String publicHostname;
    @SerializedName("nodePort")
    Integer port;
    @SerializedName("nodePublicPort")
    Integer publicPort;
    @SerializedName("nodeGroupPort")
    Integer groupPort;
    @SerializedName("nodeBindAddress")
    String bindAddress;
    @SerializedName("nodeGroupBindAddress")
    String groupBindAddress;
    @SerializedName("nodeMetadataDir")
    RawPath metadataDir;
    @SerializedName("nodeLogDir")
    RawPath logDir;
    @SerializedName("nodeBackupDir")
    RawPath backupDir;
    @SerializedName("nodeLoggerOverrides")
    Map<String, String> loggerOverrides;
  }

  private static abstract class NodeNomadChangeAdapterFactory<T extends NodeNomadChange> extends DelegateTypeAdapterFactory<T> {
    public NodeNomadChangeAdapterFactory(Class<T> type) {
      super(type);
    }

    @Override
    public String toString() {
      return "Factory[type=" + type + ",adapter=" + NodeNomadChangeAdapter.class.getName() + "]";
    }

    @Override
    public TypeAdapter<T> create(Gson gson) {
      final TypeAdapter<Cluster> clusterAdapter = gson.getAdapter(Cluster.class);
      final TypeAdapter<Node> nodeAdapter = gson.getAdapter(Node.class);
      return new NodeNomadChangeAdapter(clusterAdapter, nodeAdapter);
    }

    protected abstract T create(Cluster cluster, UID stripeUID, Node node);

    private class NodeNomadChangeAdapter extends TypeAdapter<T> {
      private final TypeAdapter<Cluster> clusterAdapter;
      private final TypeAdapter<Node> nodeAdapter;

      public NodeNomadChangeAdapter(TypeAdapter<Cluster> clusterAdapter, TypeAdapter<Node> nodeAdapter) {
        this.clusterAdapter = clusterAdapter;
        this.nodeAdapter = nodeAdapter;
      }

      @Override
      public String toString() {
        return getClass().getName();
      }

      @Override
      public void write(JsonWriter out, T value) throws IOException {
        out.beginObject();
        out.name("cluster");
        clusterAdapter.write(out, value.getCluster());
        out.name("node");
        nodeAdapter.write(out, value.getNode());
        out.name("stripeId");
        out.value(value.getCluster().getStripeId(value.getStripeUID()).getAsInt());
        out.endObject();
      }

      @Override
      public T read(JsonReader in) throws IOException {
        Cluster cluster = null;
        Node node = null;
        Integer stripeId = null;
        in.beginObject();
        while (in.peek() != JsonToken.END_OBJECT) {
          switch (in.nextName()) {
            case "cluster":
              cluster = clusterAdapter.read(in);
              break;
            case "node":
              node = nodeAdapter.read(in);
              break;
            case "stripeId":
              stripeId = in.nextInt();
              break;
            default:
              in.skipValue();
          }
        }
        in.endObject();
        if (node == null || cluster == null || stripeId == null) {
          throw new IllegalArgumentException("Invalid " + type + ": node=" + node + ", cluster=" + cluster + ", stripeId=" + stripeId);
        }
        final Cluster upgraded = new FormatUpgrade().upgrade(cluster, Version.V1);
        return create(
            upgraded,
            upgraded.getStripe(stripeId).get().getUID(),
            upgraded.getNodeByName(node.getName()).get());
      }
    }
  }

  private static class NodeContextTypeAdapter extends TypeAdapter<NodeContext> {
    private final TypeAdapter<Cluster> clusterAdapter;

    public NodeContextTypeAdapter(TypeAdapter<Cluster> clusterAdapter) {
      this.clusterAdapter = clusterAdapter;
    }

    @Override
    public void write(JsonWriter out, NodeContext value) throws IOException {
      final Node node = value.getNode();
      out.beginObject();
      out.name("cluster");
      clusterAdapter.write(out, value.getCluster());
      out.name("nodeName");
      out.value(node.getName());
      out.name("stripeId");
      out.value(value.getCluster().getStripeIdByNode(node.getUID()).getAsInt());
      out.endObject();
    }

    @Override
    public NodeContext read(JsonReader in) throws IOException {
      String nodeName = null;
      Cluster cluster = null;
      in.beginObject();
      while (in.peek() != JsonToken.END_OBJECT) {
        switch (in.nextName()) {
          case "nodeName":
            nodeName = in.nextString();
            break;
          case "cluster":
            cluster = clusterAdapter.read(in);
            break;
          default:
            in.skipValue();
        }
      }
      in.endObject();
      if (nodeName == null || cluster == null) {
        throw new IllegalArgumentException("Invalid " + NodeContext.class.getName() + ": nodeName=" + nodeName + ", cluster=" + cluster);
      }
      return new NodeContext(cluster, cluster.getNodeByName(nodeName).get().getUID());
    }

    @Override
    public String toString() {
      return NodeContext.class.getName() + "V1";
    }
  }
}
