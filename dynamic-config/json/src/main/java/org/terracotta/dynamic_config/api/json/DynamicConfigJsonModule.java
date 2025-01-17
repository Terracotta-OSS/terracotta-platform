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

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.FormatUpgradeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockAwareDynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.UnlockConfigNomadChange;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.json.Json;
import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;
import org.terracotta.json.gson.UnsafeObjectTypeAdapterFactory;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.ChangeRequest;
import org.terracotta.nomad.server.ChangeState;
import org.terracotta.nomad.server.NomadServer;

import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
@Json.Module.Dependencies({StructJsonModule.class, InetJsonModule.class})
public class DynamicConfigJsonModule implements GsonModule {
  @Override
  public void configure(GsonConfig config) {
    config.objectToString(RawPath.class, RawPath::valueOf);
    config.objectToString(UID.class, UID::valueOf);
    config.objectToString(FailoverPriority.class, FailoverPriority::valueOf);
    config.objectToString(Version.class, Version::fromValue);

    config.registerSuperType(NomadChange.class);

    Stream.of(
        ClusterActivationNomadChange.class,
        MultiSettingNomadChange.class,
        NodeAdditionNomadChange.class,
        NodeRemovalNomadChange.class,
        SettingNomadChange.class,
        StripeAdditionNomadChange.class,
        StripeRemovalNomadChange.class,
        LockAwareDynamicConfigNomadChange.class,
        FormatUpgradeNomadChange.class,
        LockConfigNomadChange.class,
        UnlockConfigNomadChange.class
    ).forEach(type -> {
      config.postSerialization(type, (nomadChange, jsonElement) -> jsonElement.getAsJsonObject().addProperty("summary", nomadChange.getSummary()));
      config.registerSubtype(NomadChange.class, type);
    });

    config.registerSuperType(MutativeMessage.class, "messageType")
        .withSubtype(PrepareMessage.class, "PREPARE")
        .withSubtype(CommitMessage.class, "COMMIT")
        .withSubtype(RollbackMessage.class, "ROLLBACK")
        .withSubtype(TakeoverMessage.class, "TAKEOVER");

    config.registerMixin(ChangeDetails.class, ChangeDetailsMixin.class);
    config.registerMixin(ChangeState.class, ChangeStateMixin.class);
    config.registerMixin(ChangeRequest.class, ChangeRequestMixin.class);

    config.mapSuperType(Applicability.class, new Function<JsonObject, Class<? extends Applicability>>() {
      @Override
      public String toString() {
        return Applicability.V1.class.getName() + "|" + Applicability.V2.class.getName();
      }

      @Override
      public Class<? extends Applicability> apply(JsonObject jsonObject) {
        return jsonObject.has("scope") ? Applicability.V1.class : Applicability.V2.class;
      }
    });

    // These are the list of classes for which instances are passed inside a raw object or raw collection
    // or raw type. As such, the declared type does not allow enough information for Gson to be able to
    // rebuild the original object. So these classes are those authorized to be class-loaded in order
    // for Gson to rebuild the instance.
    // These are mostly exposed services, arguments and returned types through diagnostic port.
    config.allowClassLoading(
        AcceptRejectResponse.class,
        Boolean.class,
        ChangeState.class,
        Cluster.class,
        CommitMessage.class,
        DiscoverResponse.class,
        Duration.class,
        DynamicConfigService.class,
        License.class,
        NodeContext.class,
        NomadChangeInfo.class,
        NomadChangeInfo[].class,
        NomadServer.class,
        Object[].class,
        PrepareMessage.class,
        RollbackMessage.class,
        String.class,
        TakeoverMessage.class,
        TopologyService.class
    );
  }

  static class ChangeDetailsMixin<T> {
    @JsonAdapter(UnsafeObjectTypeAdapterFactory.class)
    private T result;
  }

  static class ChangeStateMixin<T> {
    @JsonAdapter(UnsafeObjectTypeAdapterFactory.class)
    private T changeResult;
  }

  static class ChangeRequestMixin<T> {
    @JsonAdapter(UnsafeObjectTypeAdapterFactory.class)
    private T changeResult;
  }
}
